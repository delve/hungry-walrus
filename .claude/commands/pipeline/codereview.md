---
description: Run Code Review on the most recent Developer session output.
---

Use the codereviewer agent to review the most recent developer session.
Developer session notes are in 
./handoffs/developer-notes-$ARGUMENTS[0]-$ARGUMENTS[1].md

Report findings in the handoff file. When a finding is resolved update the finding in the handoff file with a summary of the fix. If a finding is deferred or found to be invalid update the finding in the handoff file with a rationale. If there is a regression update the existing finding to reflect that rather than adding a new one.

Use the handoff file and treat it as a running document for multiple passes, appending new findings and updating previous findings as necessary.
./handoffs/code-review-$ARGUMENTS[0]-$ARGUMENTS[1].md.