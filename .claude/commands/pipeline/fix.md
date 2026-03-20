---
description: Run a Developer session fixing review findings for a specific layer or component.
---

Use the developer agent to read review findings for layer $ARGUMENTS[1] from 
./handoffs/code-review-$ARGUMENTS[0]-$ARGUMENTS[1].md.

Adjust code in the $ARGUMENTS[1] layer to correct the review findings, or 
document your reasons for not correcting the finding in your handoff file.
Add unit tests to cover any additional code created by the changes.
Run all unit tests for the layer after making changes to verify nothing is
broken.

Append your session notes to the handoff file 
./handoffs/developer-notes-$ARGUMENTS[0]-$ARGUMENTS[1].md
