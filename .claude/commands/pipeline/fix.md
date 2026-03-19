---
description: Run a Developer session fixing review findings for a specific layer or component.
---

Use the developer agent to read review findings for layer $ARGUMENTS[1] from 
./handoffs/code-review-$ARGUMENTS[0]-$ARGUMENTS[1].md.

Review open issues in the handoff file and adjust code in the $ARGUMENTS[1] layer to correct them, or document your reasons for not correcting the finding in your handoff file. Add unit tests to cover any additional code created by the changes.

Use the handoff file and treat it as a running document for multiple passes, updating open issues as necessary.
./handoffs/code-review-$ARGUMENTS[0]-$ARGUMENTS[1].md.