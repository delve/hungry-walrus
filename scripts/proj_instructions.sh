#!/usr/bin/env bash
#

set -euo pipefail

# --- Configuration ----------------------------------------------------------
# ANTHROPIC_API_KEY="${ANTHROPIC_API_KEY:-STUB_API_KEY}"
REPO_OWNER="delve"
REPO_NAME="hungry-walrus"
REPO_BRANCH="main"
# CLAUDE_MODEL="claude-opus-4-5"
# CLAUDE_API_URL="https://api.anthropic.com/v1/messages"
# ANTHROPIC_API_VERSION="2023-06-01"
# MAX_TOKENS=8192

# Fixed instructions. File URLs are appended
PROMPT_TEXT=$(cat <<'EOF'
You are a coach assisting me in building AI agents. These agents will build software applications according to my specifications; each agent performing a specific role in the development process. I am a site reliability engineer and am technically skilled in many adjacent practices but have minimal experience in software design. I can write code in several languages, but cannot architect an application from scratch. I can manipulate CI/CD pipelines but do not know all the steps that are required.
Do not preface every answer with a compliment. Push back on my assumptions or decisions where appropriate. Seek clarification when I am unclear, and more details if you need them.

The relevant files for the project are available on Github using the following URLs:
EOF
)

# Directories to walk, relative to the current working directory.
SEARCH_DIRS=("./.claude" "./docs")

# --- Helpers ----------------------------------------------------------------
require() {
  command -v "$1" >/dev/null 2>&1 || { echo "Missing dependency: $1" >&2; exit 1; }
}
require find

raw_url_for() {
  # Strip leading ./ from the local path and build the raw.githubusercontent URL.
  local path="${1#./}"
  printf 'https://raw.githubusercontent.com/%s/%s/refs/heads/%s/%s\n' \
    "$REPO_OWNER" "$REPO_NAME" "$REPO_BRANCH" "$path"
}

# --- Collect files ----------------------------------------------------------
mapfile -t FILES < <(find "${SEARCH_DIRS[@]}" -type f | sort)

if [[ ${#FILES[@]} -eq 0 ]]; then
  echo "No files found under: ${SEARCH_DIRS[*]}" >&2
  exit 1
fi

# --- Fetch each file and assemble the message body -------------------------
# The user message is built as a single string: the prompt, followed by one
# block per file containing the URL and its contents.
MESSAGE_BODY="$PROMPT_TEXT"$'\n\n'

for path in "${FILES[@]}"; do
  url="$(raw_url_for "$path")"
  MESSAGE_BODY+="--- FILE: $path"$'\n'
  MESSAGE_BODY+="--- URL:  $url"$'\n'
done

echo "$MESSAGE_BODY" > ./docs/project_instructions.txt