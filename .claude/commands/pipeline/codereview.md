---
description: Run Code Review on the most recent Developer session output.
---

Use the codereviewer agent to review the most recent developer session.
Developer session notes are in 
./handoffs/developer-notes-$ARGUMENTS[0]-$ARGUMENTS[1].md

Report findings in the handoff file. When a finding is resolved update the issue state in the handoff. If there is a regression update the existing issue state to `Open` and add a regression note in the summary to reflect that rather than adding a new issue.

Use the handoff file and treat it as a running document for multiple passes, appending new findings and updating previous findings as necessary.
./handoffs/code-review-$ARGUMENTS[0]-$ARGUMENTS[1].md.