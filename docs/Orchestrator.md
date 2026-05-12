# Hungry Walrus pipeline orchestrator

A single-file Python script that drives your existing `/pipeline:*` agent
commands in headless mode, watches the handoff files for convergence, and
posts progress to Discord. Replaces the manual prompt-and-wait loop in
`docs/notes.md`.

## What it does

Per layer (01 data, 02 domain, 03 ui):

1. Runs the developer agent (`/pipeline:develop`) in a fresh session.
2. Loops: code review (`/pipeline:codereview`) → fix (`/pipeline:fix`) → repeat.
3. Detects convergence by reading the review handoff and checking that all
   findings are in terminal states (Resolved / Deferred / Ignored).
4. Detects stalls: if the same finding ID transitions Fixed → Open twice,
   halts with a Discord notification.
5. On convergence, posts to Discord and blocks on stdin for human approval.
6. After all three layers, runs the QA agent (`/pipeline:test`) once and exits.

The reviewer agent's session is reused across rounds for the same layer
(continuity helps it remember deferred findings). The developer agent gets a
fresh session per layer; its session is reused for fix passes within that
layer.

## Setup

1. **Install Claude Code CLI** and authenticate it:
   ```
   npm install -g @anthropic-ai/claude-code
   claude        # interactive; complete the auth flow once, then quit
   claude -p "say hello"   # verify it works headless
   ```

2. **Create a Discord webhook** on the channel you want notifications in:
   Server Settings → Integrations → Webhooks → New Webhook → Copy URL.

3. -this is already done- **Drop the files into the repo:**
   ```
   .orchestrator/run.py
   ```
   Add the contents of `Makefile.orchestrate` to your existing `Makefile`,
   or `include` it.

4. **Set the webhook env var** (e.g. in your `.envrc` or shell profile):
   ```
   export HUNGRY_WALRUS_DISCORD_WEBHOOK='https://discord.com/api/webhooks/...'
   ```

   Do not commit this URL. Add `.orchestrator/` to `.gitignore`.

## Running

```
make orchestrate            # resume from state.json, or start at layer 1 if no state
make orchestrate LAYER=1    # explicit clean restart from layer 1
make orchestrate LAYER=2    # clean start at layer 2 (treats layer 1 as approved)
make orchestrate LAYER=3    # clean start at layer 3 (treats layers 1, 2 as approved)

make orchestrate-reset      # wipe state.json and log.txt
```

### Clean restart and ephemeral handoffs

When you pass `--layer N` (any value), the orchestrator treats existing review
and developer-notes handoffs for layers `N` and later as **stale and
disposable** and deletes them before starting:

- `./handoffs/code-review-{NN}-{name}.md`
- `./handoffs/developer-notes-{NN}-{name}.md`

for every targeted layer. Earlier layers' handoffs are preserved untouched
(they're still valuable as context for the layer being rebuilt).

Resume mode (no `--layer` argument) does **not** delete anything; it continues
from where state.json left off.

If the cleanup deletes files, you'll see a `🧹` notification in Discord
listing exactly what was deleted, and the same list goes to `.orchestrator/log.txt`.

## Behavior

- **Per-phase transitions** trigger a Discord message.
- **Heartbeat** every 15 minutes during a long agent run (each `claude -p`
  call may take many minutes; the heartbeat thread fires while you wait).
- **Human gate** after each layer converges. Script prints a prompt to your
  terminal and blocks on stdin. Edit handoff files manually before answering
  if you want to inject feedback for the next phase. Type `continue`, `halt`,
  or `skip`.
- **State persistence**: `.orchestrator/state.json` is updated after every
  transition. Ctrl-C and re-run is safe (resumes from where it left off
  unless you pass `--layer`).
- **Cost tracking**: per-layer and total cost from the JSON output's
  `total_cost_usd` field is accumulated into state and reported at the end.

## Failure handling

| Condition | Detection | Action |
|---|---|---|
| Rate limit (429) | exit code != 0, error text matches | Retry with backoff 30s/60s/120s; halt if all three fail |
| Quota / credit exhausted | error text matches | Halt with full error in Discord |
| Context window exceeded | error text matches | Halt with full error in Discord |
| Timeout (GNU `timeout` killed the process) | exit code 124 or 137 | Halt with full error in Discord |
| Unknown CLI error | exit code != 0, no pattern match | Halt with full error in Discord |
| Agent returned success but handoff file missing/empty | file check | Halt |
| Agent returned success but review handoff has no parseable findings | regex check | Halt |
| Convergence not reached in 8 rounds | round counter | Halt |
| Same finding ID regresses (Fixed→Open) twice | history tracking | Halt |

Every halt posts the exact failure kind, exit code, and stderr to Discord
(truncated at 1900 chars; full text in `.orchestrator/log.txt`).

## What it does NOT do

- Does **not** run `/pipeline:architect`, `/pipeline:design`, `/pipeline:scaffold`,
  or `/pipeline:cicd`. Per your notes, those are manual.
- Does **not** read your terminal for free-text feedback. To inject feedback,
  edit the relevant handoff file before answering `continue` at the human gate.
- Does **not** auto-resolve stalls. When two regressions on the same finding
  fire, you get a Discord ping and the pipeline halts. You triage manually.

## Tuning

Constants at the top of `run.py`:

- `MAX_REVIEW_ROUNDS = 8` — hard cap on dev/review iterations per layer.
- `REGRESSION_THRESHOLD = 2` — halt after N regressions on the same finding ID.
- `HEARTBEAT_INTERVAL = 15 * 60` — seconds between heartbeats.
- `DEVELOP_TIMEOUT_S`, `REVIEW_TIMEOUT_S`, `FIX_TIMEOUT_S`, `QA_TIMEOUT_S`
  — per-phase timeouts in seconds. The orchestrator wraps each `claude -p`
  call with GNU `timeout(1)`; if the process exceeds this limit, it is sent
  SIGTERM, then SIGKILL after a 30-second grace period, and treated as a
  halt-worthy `timeout` failure.

Failure text patterns in `classify_failure()` are best-effort. If you see a
real-world error misclassified, edit the function — the current matchers are
based on documented patterns and may not catch every CLI version's wording.

## Permissions

Each `claude -p` call is invoked with `--dangerously-skip-permissions` so the
agent doesn't block on confirmation prompts for file edits, bash commands, etc.
In a non-interactive orchestrated context with no human in the loop, blocking
on confirmation would simply hang. This is the documented headless-mode
pattern. The orchestrator is therefore only as safe as the agent definitions
and the prompts in `.claude/commands/pipeline/*.md`. Review those.