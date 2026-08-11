#!/usr/bin/env python3
"""
Hungry Walrus pipeline orchestrator.

Drives the develop -> review -> fix loop per layer, then runs QA.
Reads handoff files to detect convergence and regressions.
Posts notifications to a Discord webhook. Blocks on stdin for human gates.

Usage:
    python .orchestrator/run.py            # resume from state.json or start at layer 1
    python .orchestrator/run.py --layer 2  # clean start at layer 2 (treats earlier layers as approved)

Environment:
    HUNGRY_WALRUS_DISCORD_WEBHOOK    Required. Discord webhook URL.
    HUNGRY_WALRUS_REPO_DIR           Optional. Defaults to current working dir.
"""

import argparse
import json
import os
import re
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request
from dataclasses import dataclass, field, asdict
from datetime import datetime, timedelta, UTC
from pathlib import Path
from typing import Optional
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError


# ---------- Configuration ----------

LAYERS = [
    ("01", "data layer"),
    ("02", "domain layer"),
    ("03", "ui layer"),
]

MAX_REVIEW_ROUNDS = 8
REGRESSION_THRESHOLD = 2            # same ID Fixed->Open this many times -> halt
RETRY_BACKOFFS = [30, 60, 120]      # seconds, used per attempt on rate_limit
HEARTBEAT_INTERVAL = 15 * 60        # seconds

# Usage-limit (5-hour / weekly cap) handling
USAGE_LIMIT_WAIT_CAP_S = 5 * 3600           # halt if reset more than this far out
USAGE_LIMIT_POST_FAILURE_RETRY_S = 5 * 60   # if a retry after reset still fails, wait this long before one more try
USAGE_LIMIT_MAX_RETRIES = 2                 # consecutive retries before halting; counter resets on any success

# Mapping for common timezone abbreviations the CLI may emit instead of IANA names.
# zoneinfo can't resolve these directly; fall back to a sensible IANA equivalent.
TIMEZONE_ABBREVIATION_MAP = {
    "GMT": "Etc/GMT",
    "UTC": "UTC",
    "BST": "Europe/London",
    "CET": "Europe/Paris",
    "CEST": "Europe/Paris",
    "EST": "America/New_York",
    "EDT": "America/New_York",
    "CST": "America/Chicago",
    "CDT": "America/Chicago",
    "MST": "America/Denver",
    "MDT": "America/Denver",
    "PST": "America/Los_Angeles",
    "PDT": "America/Los_Angeles",
    "JST": "Asia/Tokyo",
    "AEST": "Australia/Sydney",
    "AEDT": "Australia/Sydney",
}

DEVELOP_TIMEOUT_S = 30 * 60
REVIEW_TIMEOUT_S = 15 * 60
FIX_TIMEOUT_S = 20 * 60
QA_TIMEOUT_S = 45 * 60

DISCORD_CONTENT_LIMIT = 1900        # 2000 with safety margin
STATE_DIR_NAME = ".orchestrator"
STATE_FILE_NAME = "state.json"
LOG_FILE_NAME = "log.txt"
TRANSCRIPTS_DIR_NAME = "transcripts"


# ---------- State model ----------

@dataclass
class LayerState:
    layer_id: str
    name: str
    status: str = "pending"           # pending, developing, reviewing, fixing, awaiting_human, approved, halted
    developer_session_id: Optional[str] = None
    reviewer_session_id: Optional[str] = None
    rounds: int = 0
    regression_counts: dict = field(default_factory=dict)
    finding_state_history: dict = field(default_factory=dict)  # id -> last seen state
    cost_usd: float = 0.0


@dataclass
class PipelineState:
    started_at: str
    current_layer: Optional[str] = None
    layers: dict = field(default_factory=dict)
    qa_status: str = "pending"         # pending, running, done, halted
    qa_cost_usd: float = 0.0
    total_cost_usd: float = 0.0


def state_path(repo_dir: Path) -> Path:
    return repo_dir / STATE_DIR_NAME / STATE_FILE_NAME


def log_path(repo_dir: Path) -> Path:
    return repo_dir / STATE_DIR_NAME / LOG_FILE_NAME


def load_state(repo_dir: Path) -> Optional[PipelineState]:
    p = state_path(repo_dir)
    if not p.exists():
        return None
    text = p.read_text().strip()
    if not text:
        append_log(repo_dir, f"State file {p} is empty; treating as no prior state")
        return None
    try:
        raw = json.loads(text)
    except json.JSONDecodeError as e:
        append_log(repo_dir, f"State file {p} is malformed ({e}); treating as no prior state")
        return None
    try:
        layers = {k: LayerState(**v) for k, v in raw.get("layers", {}).items()}
        raw["layers"] = layers
        return PipelineState(**raw)
    except TypeError as e:
        append_log(repo_dir, f"State file {p} has unexpected shape ({e}); treating as no prior state")
        return None


def save_state(repo_dir: Path, state: PipelineState) -> None:
    p = state_path(repo_dir)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(asdict(state), indent=2))


def append_log(repo_dir: Path, message: str) -> None:
    p = log_path(repo_dir)
    p.parent.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(UTC).isoformat(timespec="seconds").replace("+00:00", "Z")
    with p.open("a") as f:
        f.write(f"[{stamp}] {message}\n")


# ---------- Discord ----------

class Discord:
    def __init__(self, webhook_url: str, repo_dir: Path):
        self.webhook_url = webhook_url
        self.repo_dir = repo_dir

    def send(self, message: str) -> None:
        if len(message) > DISCORD_CONTENT_LIMIT:
            message = message[:DISCORD_CONTENT_LIMIT - 50] + "\n…[truncated; see log.txt]"
        body = json.dumps({"content": message}).encode("utf-8")
        req = urllib.request.Request(
            self.webhook_url,
            data=body,
            headers={
                "Content-Type": "application/json",
                "User-Agent": "hungry-walrus-orchestrator/1.0 (+https://github.com/delve/hungry-walrus)",
            },
            method="POST",
        )
        try:
            with urllib.request.urlopen(req, timeout=10) as resp:
                resp.read()
        except urllib.error.HTTPError as e:
            try:
                body = e.read().decode("utf-8", errors="replace")[:500]
            except Exception:
                body = "(could not read response body)"
            append_log(self.repo_dir, f"Discord notify HTTP {e.code}: {body}")
        except (urllib.error.URLError, TimeoutError) as e:
            append_log(self.repo_dir, f"Discord notify failed: {e}")


# ---------- Heartbeat thread ----------

class Heartbeat:
    def __init__(self, discord: Discord, label: str):
        self.discord = discord
        self.label = label
        self.start_time = time.monotonic()
        self._stop = threading.Event()
        self._thread = threading.Thread(target=self._run, daemon=True)

    def _run(self):
        while not self._stop.wait(HEARTBEAT_INTERVAL):
            elapsed_min = int((time.monotonic() - self.start_time) / 60)
            self.discord.send(f"💓 [hungry-walrus] Heartbeat: {self.label}, elapsed {elapsed_min}m")

    def __enter__(self):
        self._thread.start()
        return self

    def __exit__(self, *exc):
        self._stop.set()


# ---------- Failure classification ----------

def classify_failure(exit_code: int, stderr_text: str, json_output: dict) -> Optional[str]:
    """Returns None on success, otherwise: rate_limit, usage_limited,
    quota_exhausted, context_overflow, timeout, or unknown."""
    if exit_code == 0 and not (isinstance(json_output, dict) and json_output.get("is_error", False)):
        return None

    # GNU timeout(1) exit codes: 124 = killed by SIGTERM after timeout,
    # 137 = killed by SIGKILL after --kill-after grace period.
    if exit_code in (124, 137):
        return "timeout"

    text_parts = [stderr_text or ""]
    if isinstance(json_output, dict):
        text_parts.append(json_output.get("result") or "")
    text = " ".join(text_parts).lower()

    if "rate limit" in text or "429" in text or "too many requests" in text:
        return "rate_limit"
    # Usage limit: rolling 5-hour or weekly cap on Pro/Max plans. The reset
    # timestamp in the error text lets us decide whether to wait or halt.
    # Check this BEFORE quota_exhausted because the text contains "limit".
    if "hit your limit" in text and "reset" in text:
        return "usage_limited"
    if "credit balance" in text or "usage limit" in text or "quota" in text or "out of credits" in text:
        return "quota_exhausted"
    if "context" in text and ("exceed" in text or "too large" in text or "too long" in text):
        return "context_overflow"
    return "unknown"


def parse_usage_limit_reset(text: str) -> Optional[datetime]:
    """Extract a reset datetime from a CLI 'You've hit your limit · resets ...' message.

    Returns a timezone-aware datetime in UTC, or None if parsing fails (any
    failure case maps to the caller falling back to halt behavior).

    Format observed: 'resets 11:20pm (Europe/London)' — clock time plus a
    timezone in parens. The next occurrence of that clock time in that zone
    is the reset moment.
    """
    if not text:
        return None
    # Match: resets <time-of-day> (<timezone>)
    # Time accepts forms like 11:20pm, 11:20 PM, 11pm, 23:20.
    pattern = re.compile(
        r"resets?\s+"
        r"(?P<time>\d{1,2}(?::\d{2})?\s*(?:am|pm)?)"
        r"\s*\((?P<tz>[^)]+)\)",
        re.IGNORECASE,
    )
    match = pattern.search(text)
    if not match:
        return None

    time_str = match.group("time").strip().lower().replace(" ", "")
    tz_str = match.group("tz").strip()

    # Resolve timezone: IANA name first, then abbreviation map.
    tz = None
    try:
        tz = ZoneInfo(tz_str)
    except ZoneInfoNotFoundError:
        mapped = TIMEZONE_ABBREVIATION_MAP.get(tz_str.upper())
        if mapped:
            try:
                tz = ZoneInfo(mapped)
            except ZoneInfoNotFoundError:
                return None
    if tz is None:
        return None

    # Parse the time of day. Accept "11:20pm", "11pm", "23:20".
    am_pm = None
    bare = time_str
    if bare.endswith("am") or bare.endswith("pm"):
        am_pm = bare[-2:]
        bare = bare[:-2]
    if ":" in bare:
        try:
            hour_str, minute_str = bare.split(":", 1)
            hour = int(hour_str)
            minute = int(minute_str)
        except ValueError:
            return None
    else:
        try:
            hour = int(bare)
            minute = 0
        except ValueError:
            return None

    if am_pm == "pm" and hour < 12:
        hour += 12
    elif am_pm == "am" and hour == 12:
        hour = 0

    if not (0 <= hour <= 23 and 0 <= minute <= 59):
        return None

    # Find the next occurrence of this clock time in the target timezone.
    now_local = datetime.now(tz)
    candidate = now_local.replace(hour=hour, minute=minute, second=0, microsecond=0)
    if candidate <= now_local:
        candidate = candidate + timedelta(days=1)

    return candidate.astimezone(UTC)


@dataclass
class ClaudeResult:
    success: bool
    failure_kind: Optional[str]
    session_id: Optional[str]
    cost_usd: float
    raw_output: str
    raw_stderr: str
    exit_code: int
    transcript_path: Optional[str] = None


def _slugify_label(label: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", label.lower()).strip("-")


def write_transcript(
    repo_dir: Path,
    label: str,
    attempt: int,
    prompt: str,
    cmd: list[str],
    exit_code: int,
    stdout: str,
    stderr: str,
    parsed: dict,
    failure_kind: Optional[str],
) -> str:
    """Write a per-invocation transcript to .orchestrator/transcripts/.
    Returns the relative path for reference in notifications."""
    transcripts_dir = repo_dir / STATE_DIR_NAME / TRANSCRIPTS_DIR_NAME
    transcripts_dir.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(UTC).strftime("%Y%m%dT%H%M%SZ")
    slug = _slugify_label(label)
    filename = f"{stamp}-{slug}-attempt{attempt}.json"
    path = transcripts_dir / filename
    payload = {
        "label": label,
        "attempt": attempt,
        "timestamp_utc": datetime.now(UTC).isoformat(timespec="seconds").replace("+00:00", "Z"),
        "command": cmd,
        "prompt": prompt,
        "exit_code": exit_code,
        "failure_kind": failure_kind,
        "stdout_parsed_json": parsed if isinstance(parsed, dict) else None,
        "stdout_raw": stdout,
        "stderr_raw": stderr,
    }
    path.write_text(json.dumps(payload, indent=2))
    try:
        return str(path.relative_to(repo_dir))
    except ValueError:
        return str(path)


def invoke_claude(
    repo_dir: Path,
    prompt: str,
    timeout_s: int,
    resume_session_id: Optional[str],
    discord: Discord,
    label: str,
    agent: Optional[str] = None,
) -> ClaudeResult:
    """Run `claude -p` wrapped in GNU timeout, with retries for rate-limit failures.
    If `agent` is provided, the session runs as that sub-agent (loads the
    definition from .claude/agents/<agent>.md as its system prompt)."""
    cmd = [
        "timeout", "--signal=TERM", "--kill-after=30s", str(timeout_s),
        "claude",
        "-p", prompt,
        "--output-format", "json",
        "--dangerously-skip-permissions",
    ]
    if agent:
        cmd += ["--agent", agent]
    if resume_session_id:
        cmd += ["--resume", resume_session_id]

    attempt = 0
    while True:
        attempt += 1
        prompt_preview = prompt if len(prompt) <= 500 else prompt[:500] + f"… [+{len(prompt) - 500} chars]"
        append_log(repo_dir, f"--- Invoke claude BEGIN ({label}) attempt {attempt} ---")
        append_log(repo_dir, f"cwd: {repo_dir}")
        append_log(repo_dir, f"agent: {agent or '(default Claude, no sub-agent)'}")
        append_log(repo_dir, f"resume_session_id: {resume_session_id}")
        append_log(repo_dir, f"command: {' '.join(cmd[:3])} ... [prompt+flags omitted]")
        append_log(repo_dir, f"prompt:\n{prompt_preview}")
        invoke_start = time.monotonic()
        # Defensive: never let the child inherit the parent's stdin (it'll
        # block forever if claude ever prompts interactively). Also set a
        # Python-level timeout as a backstop in case the GNU timeout wrapper
        # fails to kill its child (e.g., signal delivery issues, process
        # group weirdness). The Python timeout is set generously past the
        # GNU timeout so the inner wrapper has the first chance to clean up.
        py_timeout = timeout_s + 120
        try:
            with Heartbeat(discord, label):
                proc = subprocess.run(
                    cmd,
                    cwd=str(repo_dir),
                    capture_output=True,
                    text=True,
                    check=False,
                    stdin=subprocess.DEVNULL,
                    timeout=py_timeout,
                )
        except subprocess.TimeoutExpired as e:
            invoke_elapsed = time.monotonic() - invoke_start
            append_log(
                repo_dir,
                f"--- Invoke claude END ({label}) attempt {attempt}: "
                f"PYTHON-LEVEL TIMEOUT after {invoke_elapsed:.1f}s (limit {py_timeout}s) ---",
            )
            stdout = (e.stdout or b"").decode("utf-8", errors="replace") if isinstance(e.stdout, bytes) else (e.stdout or "")
            stderr = (e.stderr or b"").decode("utf-8", errors="replace") if isinstance(e.stderr, bytes) else (e.stderr or "")
            transcript = write_transcript(
                repo_dir, label, attempt, prompt, cmd,
                -1, stdout, stderr, {}, "timeout",
            )
            discord.send(
                f"🛑 [hungry-walrus] HALT on {label}: child process exceeded "
                f"Python-level timeout ({py_timeout}s). The GNU timeout wrapper "
                f"failed to kill it. Transcript: `{transcript}`"
            )
            append_log(repo_dir, "Python-level timeout fired; GNU timeout did not kill child")
            return ClaudeResult(False, "timeout", None, 0.0, stdout, stderr, -1, transcript)
        invoke_elapsed = time.monotonic() - invoke_start
        append_log(repo_dir, f"--- Invoke claude END ({label}) attempt {attempt}: exit={proc.returncode} elapsed={invoke_elapsed:.1f}s ---")
        stdout = proc.stdout or ""
        stderr = proc.stderr or ""
        parsed = {}
        try:
            parsed = json.loads(stdout)
        except json.JSONDecodeError:
            pass

        failure = classify_failure(proc.returncode, stderr, parsed)
        cost = float(parsed.get("total_cost_usd") or 0.0) if isinstance(parsed, dict) else 0.0
        session_id = parsed.get("session_id") if isinstance(parsed, dict) else None

        transcript = write_transcript(
            repo_dir, label, attempt, prompt, cmd,
            proc.returncode, stdout, stderr, parsed, failure,
        )
        append_log(repo_dir, f"Transcript written: {transcript}")

        if failure is None:
            return ClaudeResult(True, None, session_id, cost, stdout, stderr, proc.returncode, transcript)

        append_log(repo_dir, f"Failure kind={failure} exit={proc.returncode} stderr[:500]={stderr[:500]}")

        if failure == "rate_limit" and attempt <= len(RETRY_BACKOFFS):
            wait = RETRY_BACKOFFS[attempt - 1]
            if attempt >= 2:
                discord.send(
                    f"⚠️ [hungry-walrus] Rate-limited on {label}. "
                    f"Retry {attempt}/{len(RETRY_BACKOFFS)} in {wait}s."
                )
            time.sleep(wait)
            continue

        return ClaudeResult(False, failure, session_id, cost, stdout, stderr, proc.returncode, transcript)


# Counter for consecutive usage_limited retries. Resets to 0 on any successful
# invoke_claude call. Module-level state so it persists across phase calls.
_usage_limit_retry_count = 0


def invoke_claude_with_usage_retry(
    repo_dir: Path,
    prompt: str,
    timeout_s: int,
    resume_session_id: Optional[str],
    discord: Discord,
    label: str,
    agent: Optional[str] = None,
) -> ClaudeResult:
    """Wraps invoke_claude. On usage_limited failure, parses the reset time
    from the error text; if the reset is within USAGE_LIMIT_WAIT_CAP_S, sleeps
    until then and retries. After a successful retry, the counter resets. If
    the retry itself fails, waits USAGE_LIMIT_POST_FAILURE_RETRY_S and tries
    once more. After USAGE_LIMIT_MAX_RETRIES consecutive failures, halts."""
    global _usage_limit_retry_count

    while True:
        result = invoke_claude(
            repo_dir, prompt, timeout_s, resume_session_id, discord, label, agent,
        )

        if result.failure_kind != "usage_limited":
            if result.success:
                if _usage_limit_retry_count > 0:
                    append_log(
                        repo_dir,
                        f"Resetting usage_limit retry counter (was {_usage_limit_retry_count})",
                    )
                _usage_limit_retry_count = 0
            return result

        # usage_limited: try to parse the reset time from the result text
        result_text = ""
        try:
            parsed = json.loads(result.raw_output)
            if isinstance(parsed, dict):
                result_text = parsed.get("result") or ""
        except json.JSONDecodeError:
            pass
        # Also check stderr for the message in case it migrates there
        combined = " ".join([result_text, result.raw_stderr or ""])

        reset_utc = parse_usage_limit_reset(combined)
        if reset_utc is None:
            append_log(
                repo_dir,
                f"usage_limited but reset time could not be parsed from: {combined[:300]}",
            )
            discord.send(
                f"🛑 [hungry-walrus] Usage limit detected on {label} but reset time "
                f"could not be parsed. Halting.\nRaw text: ```{combined[:500]}```"
            )
            # Fall back: treat as a halt-worthy failure with original classification text.
            return result

        now_utc = datetime.now(UTC)
        wait_s = (reset_utc - now_utc).total_seconds()
        # Add a small buffer to avoid retrying exactly at the reset instant.
        wait_s = max(0, wait_s) + 30

        if wait_s > USAGE_LIMIT_WAIT_CAP_S:
            discord.send(
                f"🛑 [hungry-walrus] Usage limit hit on {label}. Reset is "
                f"{reset_utc.isoformat()} which is more than "
                f"{USAGE_LIMIT_WAIT_CAP_S // 3600}h away. Halting for investigation."
            )
            append_log(
                repo_dir,
                f"usage_limited: reset {reset_utc.isoformat()} exceeds cap ({wait_s:.0f}s). Halting.",
            )
            return result

        _usage_limit_retry_count += 1
        if _usage_limit_retry_count > USAGE_LIMIT_MAX_RETRIES:
            discord.send(
                f"🛑 [hungry-walrus] Usage limit retry budget exhausted "
                f"({USAGE_LIMIT_MAX_RETRIES} consecutive failures) on {label}. Halting."
            )
            append_log(repo_dir, f"usage_limited: retry budget exhausted on {label}")
            return result

        if _usage_limit_retry_count == 1:
            discord.send(
                f"⏸️ [hungry-walrus] Usage limit hit on {label}. "
                f"Waiting until {reset_utc.isoformat()} "
                f"(approximately {int(wait_s / 60)}m from now), then retrying."
            )
            append_log(
                repo_dir,
                f"usage_limited on {label}: sleeping {wait_s:.0f}s until {reset_utc.isoformat()}",
            )
            time.sleep(wait_s)
        else:
            # Second attempt: post-failure short wait
            wait_short = USAGE_LIMIT_POST_FAILURE_RETRY_S
            discord.send(
                f"⏸️ [hungry-walrus] First retry on {label} still usage-limited. "
                f"Waiting {wait_short // 60}m, then trying once more."
            )
            append_log(
                repo_dir,
                f"usage_limited on {label} after first retry: sleeping {wait_short}s",
            )
            time.sleep(wait_short)

        discord.send(f"▶️ [hungry-walrus] Resuming {label} after usage-limit wait.")
        append_log(repo_dir, f"Resuming {label} after usage-limit wait (retry {_usage_limit_retry_count})")


# ---------- Handoff parsing ----------

FINDING_RE = re.compile(r"ID:\s*([CWO]\d+)\s*State:\s*(\w+)", re.IGNORECASE)


@dataclass
class Finding:
    id: str
    state: str


def parse_findings(handoff_path: Path) -> list[Finding]:
    if not handoff_path.exists():
        return []
    text = handoff_path.read_text()
    findings = []
    for match in FINDING_RE.finditer(text):
        findings.append(Finding(id=match.group(1).upper(), state=match.group(2).capitalize()))
    return findings


def is_converged(findings: list[Finding]) -> bool:
    if not findings:
        return False
    terminal = {"Resolved", "Deferred", "Ignored"}
    return all(f.state in terminal for f in findings)


def detect_regressions(
    findings: list[Finding],
    prior_history: dict,
    regression_counts: dict,
) -> Optional[str]:
    """Detect Fixed -> Open transitions. Increment counts. Return an ID if
    threshold reached, else None. Updates prior_history in place at the end."""
    tripped = None
    for f in findings:
        prior_state = prior_history.get(f.id)
        if prior_state == "Fixed" and f.state == "Open":
            regression_counts[f.id] = regression_counts.get(f.id, 0) + 1
            if regression_counts[f.id] >= REGRESSION_THRESHOLD and tripped is None:
                tripped = f.id
    for f in findings:
        prior_history[f.id] = f.state
    return tripped


# ---------- Handoff paths ----------

def handoff_developer(repo_dir: Path, layer_id: str, layer_name: str) -> Path:
    return repo_dir / "handoffs" / f"developer-notes-{layer_id}-{layer_name}.md"


def handoff_review(repo_dir: Path, layer_id: str, layer_name: str) -> Path:
    return repo_dir / "handoffs" / f"code-review-{layer_id}-{layer_name}.md"


# ---------- Halt / error reporting ----------

def emit_halt(discord: Discord, repo_dir: Path, phase: str, result: ClaudeResult) -> None:
    error_text = (result.raw_stderr or "").strip() or "(no stderr)"
    json_error = ""
    try:
        parsed = json.loads(result.raw_output)
        if isinstance(parsed, dict) and parsed.get("is_error"):
            json_error = f"\nJSON result field:\n{parsed.get('result', '')}"
    except json.JSONDecodeError:
        pass

    message = (
        f"🛑 [hungry-walrus] HALT during {phase}\n"
        f"Condition: {result.failure_kind}\n"
        f"Exit code: {result.exit_code}\n"
        f"Transcript: `{result.transcript_path or '(none)'}`\n"
        f"Stderr:\n```\n{error_text}\n```"
        f"{json_error}"
    )
    discord.send(message)
    append_log(repo_dir, f"HALT phase={phase} kind={result.failure_kind} exit={result.exit_code}")
    append_log(repo_dir, f"Transcript: {result.transcript_path}")
    append_log(repo_dir, f"Full stderr:\n{result.raw_stderr}")
    append_log(repo_dir, f"Full stdout:\n{result.raw_output}")


# ---------- Pipeline phases ----------

def run_develop(repo_dir: Path, layer: LayerState, discord: Discord, state: PipelineState) -> bool:
    layer.status = "developing"
    save_state(repo_dir, state)
    discord.send(f"🟢 [hungry-walrus] Layer {layer.layer_id} ({layer.name}) — develop pass starting")

    prompt = (
        f"Use the developer agent to implement layer {layer.name} from "
        f"./handoffs/architecture.md\n\n"
        f"Write your session notes to the handoff file "
        f"./handoffs/developer-notes-{layer.layer_id}-{layer.name}.md"
    )
    result = invoke_claude_with_usage_retry(
        repo_dir, prompt, DEVELOP_TIMEOUT_S,
        resume_session_id=None,
        discord=discord, label=f"develop layer {layer.layer_id}",
        agent="developer",
    )
    state.total_cost_usd += result.cost_usd
    layer.cost_usd += result.cost_usd

    if not result.success:
        emit_halt(discord, repo_dir, f"develop layer {layer.layer_id}", result)
        layer.status = "halted"
        save_state(repo_dir, state)
        return False

    if result.session_id:
        layer.developer_session_id = result.session_id

    handoff = handoff_developer(repo_dir, layer.layer_id, layer.name)
    if not handoff.exists() or handoff.stat().st_size == 0:
        discord.send(
            f"🛑 [hungry-walrus] Layer {layer.layer_id} — develop pass returned success "
            f"but handoff `{handoff.name}` is missing or empty. Halting.\n"
            f"Transcript: `{result.transcript_path or '(none)'}`"
        )
        append_log(repo_dir, f"Silent failure: handoff {handoff} missing/empty after develop")
        append_log(repo_dir, f"Transcript: {result.transcript_path}")
        layer.status = "halted"
        save_state(repo_dir, state)
        return False

    discord.send(f"🟢 [hungry-walrus] Layer {layer.layer_id} ({layer.name}) — develop pass complete")
    save_state(repo_dir, state)
    return True


def run_review(repo_dir: Path, layer: LayerState, discord: Discord, state: PipelineState) -> Optional[list[Finding]]:
    layer.status = "reviewing"
    save_state(repo_dir, state)

    prompt = (
        f"Use the codereviewer agent to review the most recent developer session.\n"
        f"Developer session notes are in "
        f"./handoffs/developer-notes-{layer.layer_id}-{layer.name}.md\n\n"
        f"Report findings in the handoff file. When a finding is resolved update the "
        f"issue state in the handoff. If there is a regression update the existing "
        f"issue state to `Open` and add a regression note in the summary to reflect "
        f"that rather than adding a new issue.\n\n"
        f"Use the handoff file and treat it as a running document for multiple passes, "
        f"appending new findings and updating previous findings as necessary.\n"
        f"./handoffs/code-review-{layer.layer_id}-{layer.name}.md"
    )
    result = invoke_claude_with_usage_retry(
        repo_dir, prompt, REVIEW_TIMEOUT_S,
        resume_session_id=None,
        discord=discord, label=f"review layer {layer.layer_id} round {layer.rounds + 1}",
        agent="code-reviewer",
    )
    state.total_cost_usd += result.cost_usd
    layer.cost_usd += result.cost_usd

    if not result.success:
        emit_halt(discord, repo_dir, f"review layer {layer.layer_id}", result)
        layer.status = "halted"
        save_state(repo_dir, state)
        return None

    if result.session_id:
        layer.reviewer_session_id = result.session_id

    handoff = handoff_review(repo_dir, layer.layer_id, layer.name)
    if not handoff.exists():
        discord.send(
            f"🛑 [hungry-walrus] Layer {layer.layer_id} — review returned success but "
            f"`{handoff.name}` does not exist. Halting.\n"
            f"Transcript: `{result.transcript_path or '(none)'}`"
        )
        append_log(repo_dir, f"Silent failure: review handoff {handoff} missing")
        append_log(repo_dir, f"Transcript: {result.transcript_path}")
        layer.status = "halted"
        save_state(repo_dir, state)
        return None

    findings = parse_findings(handoff)
    if not findings:
        discord.send(
            f"🛑 [hungry-walrus] Layer {layer.layer_id} — review handoff contains no "
            f"parseable findings. Treating as silent failure. Halting.\n"
            f"Transcript: `{result.transcript_path or '(none)'}`"
        )
        append_log(repo_dir, f"Silent failure: zero findings parsed from {handoff}")
        append_log(repo_dir, f"Transcript: {result.transcript_path}")
        layer.status = "halted"
        save_state(repo_dir, state)
        return None

    save_state(repo_dir, state)
    return findings


def run_fix(repo_dir: Path, layer: LayerState, discord: Discord, state: PipelineState) -> bool:
    layer.status = "fixing"
    save_state(repo_dir, state)

    prompt = (
        f"Use the developer agent to read review findings for layer {layer.name} from "
        f"./handoffs/code-review-{layer.layer_id}-{layer.name}.md.\n\n"
        f"Review open issues in the handoff file and adjust code in the {layer.name} "
        f"layer to correct them, or document your reasons for not correcting the "
        f"finding in your handoff file. Add unit tests to cover any additional code "
        f"created by the changes.\n\n"
        f"Use the handoff file and treat it as a running document for multiple passes, "
        f"updating open issues as necessary.\n"
        f"./handoffs/code-review-{layer.layer_id}-{layer.name}.md"
    )
    result = invoke_claude_with_usage_retry(
        repo_dir, prompt, FIX_TIMEOUT_S,
        resume_session_id=None,
        discord=discord, label=f"fix layer {layer.layer_id} round {layer.rounds}",
        agent="developer",
    )
    state.total_cost_usd += result.cost_usd
    layer.cost_usd += result.cost_usd

    if not result.success:
        emit_halt(discord, repo_dir, f"fix layer {layer.layer_id}", result)
        layer.status = "halted"
        save_state(repo_dir, state)
        return False

    # Don't overwrite developer_session_id here. It marks "develop has been
    # done" and is used by process_layer to skip redoing develop on resume.
    # The fix pass session ID, if needed for debugging, is in the transcript.

    save_state(repo_dir, state)
    return True


def run_qa(repo_dir: Path, discord: Discord, state: PipelineState) -> bool:
    state.qa_status = "running"
    save_state(repo_dir, state)
    discord.send("🟢 [hungry-walrus] QA pass starting")

    prompt = (
        "Use the qa agent to verify the Hungry Walrus application. Build the project, "
        "run all existing tests, write additional unit tests to fill coverage gaps, "
        "write integration tests for layer interactions, and produce a full QA report "
        "at ./handoffs/qa-report.md."
    )
    result = invoke_claude_with_usage_retry(
        repo_dir, prompt, QA_TIMEOUT_S,
        resume_session_id=None,
        discord=discord, label="QA",
        agent="qa",
    )
    state.qa_cost_usd = result.cost_usd
    state.total_cost_usd += result.cost_usd

    if not result.success:
        emit_halt(discord, repo_dir, "QA", result)
        state.qa_status = "halted"
        save_state(repo_dir, state)
        return False

    qa_report = repo_dir / "handoffs" / "qa-report.md"
    if not qa_report.exists() or qa_report.stat().st_size == 0:
        discord.send(
            "🛑 [hungry-walrus] QA returned success but `qa-report.md` is missing or empty. Halting.\n"
            f"Transcript: `{result.transcript_path or '(none)'}`"
        )
        append_log(repo_dir, "Silent failure: qa-report.md missing/empty after QA")
        append_log(repo_dir, f"Transcript: {result.transcript_path}")
        state.qa_status = "halted"
        save_state(repo_dir, state)
        return False

    state.qa_status = "done"
    save_state(repo_dir, state)
    discord.send(
        f"✅ [hungry-walrus] QA complete. Report at handoffs/qa-report.md. "
        f"Total run cost: ${state.total_cost_usd:.2f}."
    )
    return True


# ---------- Human gate ----------

def human_gate(discord: Discord, layer: LayerState) -> str:
    discord.send(
        f"✅ [hungry-walrus] Layer {layer.layer_id} ({layer.name}) — converged after "
        f"{layer.rounds} round(s). Human review needed. Reply at terminal: "
        f"`continue`, `halt`, or `skip`."
    )
    print()
    print(f"=== Layer {layer.layer_id} ({layer.name}) ready for your review ===")
    print(f"Rounds: {layer.rounds}, cost: ${layer.cost_usd:.2f}")
    print("Edit handoff files now if you want to inject feedback for the next layer.")
    print("Type one of: continue, halt, skip")
    while True:
        try:
            response = input("> ").strip().lower()
        except EOFError:
            return "halt"
        if response in ("continue", "halt", "skip"):
            return response
        print("Please type exactly: continue, halt, or skip")


# ---------- Inner loop per layer ----------

def process_layer(repo_dir: Path, layer: LayerState, discord: Discord, state: PipelineState) -> str:
    # Skip develop if it has already succeeded once (developer_session_id is
    # only set after a successful develop pass). Resume goes straight to the
    # review/fix loop, with layer.rounds preserving how far the prior run got.
    if not layer.developer_session_id:
        if not run_develop(repo_dir, layer, discord, state):
            return "halt"
    else:
        append_log(
            repo_dir,
            f"Layer {layer.layer_id}: resuming with existing developer_session_id "
            f"(rounds so far: {layer.rounds}, reviewer_session_id: {layer.reviewer_session_id})",
        )
        discord.send(
            f"🔁 [hungry-walrus] Layer {layer.layer_id} ({layer.name}) — resuming "
            f"review/fix loop at round {layer.rounds + 1}"
        )

    while layer.rounds < MAX_REVIEW_ROUNDS:
        layer.rounds += 1
        save_state(repo_dir, state)

        findings = run_review(repo_dir, layer, discord, state)
        if findings is None:
            return "halt"

        open_count = sum(1 for f in findings if f.state == "Open")
        fixed_count = sum(1 for f in findings if f.state == "Fixed")
        discord.send(
            f"🔵 [hungry-walrus] Layer {layer.layer_id} — review round {layer.rounds}: "
            f"{open_count} Open, {fixed_count} Fixed, {len(findings)} total"
        )

        if is_converged(findings):
            layer.status = "awaiting_human"
            save_state(repo_dir, state)
            return human_gate(discord, layer)

        regressed = detect_regressions(findings, layer.finding_state_history, layer.regression_counts)
        if regressed:
            discord.send(
                f"🛑 [hungry-walrus] Layer {layer.layer_id} — STALLED. "
                f"Finding {regressed} regressed {REGRESSION_THRESHOLD} times. Halting."
            )
            append_log(repo_dir, f"Stall on layer {layer.layer_id}: {regressed} regressed")
            layer.status = "halted"
            save_state(repo_dir, state)
            return "halt"

        if not run_fix(repo_dir, layer, discord, state):
            return "halt"

    discord.send(
        f"🛑 [hungry-walrus] Layer {layer.layer_id} hit max rounds "
        f"({MAX_REVIEW_ROUNDS}) without converging. Halting."
    )
    append_log(repo_dir, f"Max rounds reached on layer {layer.layer_id}")
    layer.status = "halted"
    save_state(repo_dir, state)
    return "halt"


# ---------- Top level ----------

def purge_ephemeral_handoffs(repo_dir: Path, start_layer: int) -> list[str]:
    """Delete code-review and developer-notes handoffs for layers >= start_layer.
    Returns a list of file paths that were deleted (for logging)."""
    deleted = []
    for layer_id, layer_name in LAYERS:
        if int(layer_id) < start_layer:
            continue
        for path in (
            handoff_review(repo_dir, layer_id, layer_name),
            handoff_developer(repo_dir, layer_id, layer_name),
        ):
            if path.exists():
                try:
                    path.unlink()
                    deleted.append(str(path.relative_to(repo_dir)))
                except OSError as e:
                    append_log(repo_dir, f"Failed to delete {path}: {e}")
    return deleted


def init_state(repo_dir: Path, start_layer: Optional[int], discord: Optional["Discord"] = None) -> PipelineState:
    existing = load_state(repo_dir)
    if start_layer is None:
        if existing:
            append_log(repo_dir, "Resuming from existing state.json")
            return existing
        append_log(repo_dir, "No prior state; starting fresh at layer 1")
        start_layer = 1

    # Clean restart: delete stale review and developer-notes handoffs for
    # the targeted layers. Earlier-layer handoffs are preserved as context.
    purged = purge_ephemeral_handoffs(repo_dir, start_layer)
    if purged:
        append_log(repo_dir, f"Clean restart at layer {start_layer}: deleted {purged}")
        if discord:
            files_list = "\n".join(f"- `{p}`" for p in purged)
            discord.send(
                f"🧹 [hungry-walrus] Clean restart at layer {start_layer}. "
                f"Deleted stale handoff files:\n{files_list}"
            )

    state = PipelineState(started_at=datetime.now(UTC).isoformat(timespec="seconds").replace("+00:00", "Z"))
    if existing:
        for layer_id, layer_name in LAYERS:
            idx = int(layer_id)
            if idx < start_layer:
                if layer_id in existing.layers:
                    prior = existing.layers[layer_id]
                    prior.status = "approved"
                    state.layers[layer_id] = prior
                else:
                    state.layers[layer_id] = LayerState(layer_id=layer_id, name=layer_name, status="approved")
            else:
                state.layers[layer_id] = LayerState(layer_id=layer_id, name=layer_name)
        state.total_cost_usd = existing.total_cost_usd
    else:
        for layer_id, layer_name in LAYERS:
            idx = int(layer_id)
            status = "approved" if idx < start_layer else "pending"
            state.layers[layer_id] = LayerState(layer_id=layer_id, name=layer_name, status=status)

    save_state(repo_dir, state)
    return state


def main():
    parser = argparse.ArgumentParser(description="Hungry Walrus pipeline orchestrator")
    parser.add_argument(
        "--layer",
        type=int,
        choices=[1, 2, 3],
        default=None,
        help="Optional starting layer. LAYER=1 means clean restart from the beginning.",
    )
    args = parser.parse_args()

    repo_dir = Path(os.environ.get("HUNGRY_WALRUS_REPO_DIR", os.getcwd())).resolve()
    webhook = os.environ.get("HUNGRY_WALRUS_DISCORD_WEBHOOK")
    if not webhook:
        print("ERROR: HUNGRY_WALRUS_DISCORD_WEBHOOK not set", file=sys.stderr)
        sys.exit(2)

    discord = Discord(webhook, repo_dir)

    state = init_state(repo_dir, args.layer, discord)
    discord.send(f"🚀 [hungry-walrus] Orchestrator starting (layer arg: {args.layer or 'resume'})")

    try:
        for layer_id, layer_name in LAYERS:
            layer = state.layers[layer_id]
            if layer.status == "approved":
                append_log(repo_dir, f"Skipping already-approved layer {layer_id}")
                continue
            state.current_layer = layer_id
            save_state(repo_dir, state)

            decision = process_layer(repo_dir, layer, discord, state)
            if decision == "halt":
                discord.send("🛑 [hungry-walrus] Pipeline halted. See terminal/log for details.")
                sys.exit(1)
            if decision == "skip":
                layer.status = "approved"
                save_state(repo_dir, state)
                continue
            layer.status = "approved"
            save_state(repo_dir, state)

        if not run_qa(repo_dir, discord, state):
            sys.exit(1)
        sys.exit(0)
    except KeyboardInterrupt:
        discord.send("🛑 [hungry-walrus] Orchestrator interrupted (Ctrl-C). State saved.")
        append_log(repo_dir, "Interrupted by user")
        sys.exit(130)


if __name__ == "__main__":
    main()