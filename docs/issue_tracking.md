# Issue tracking format
Code reviewers and developers will record and update findings
using the following rules and format.

# Rules
- Create an sequential ID for each finding using C## for critical, W## for 
  warnings, or O## for observations which may not need remediation. 
  DO NOT USE ANY OTHER SYSTEM FOR LABELING FINDINGS. Only C##, W##, O## are 
  valid IDs.
- Use consistently increasing numbers over multiple review passes such that 
  each finding from each review pass can be addressed uniquely. DO NOT REUSE
  IDs, use a unique ID for EVERY finding.
- The product owner will add their findings as P## in a similar fashion.
  Do not alter the product owner's input except to add resolution or similar
  notes.

## Developer Role
The developer only updates existing issues. The developer can only update 
the state and resolution fields. The developer can only use the states 
'Fixed', 'Deferred', or 'Ignored'.

## Code Reviewer Role
The code reviewer adds and updates issues. When adding issues the code
reviewer sets the ID, state, and summary fields. New issues have state 
'Open'. The code reviewer can only update the state field, and can only set
state to 'Open' or 'Resolved' based on its analysis.

## Severity Definitions
### Critical
Problems that must be resolved before proceeding. These include
architectural violations, broken interfaces between layers, incorrect
business logic, or missing error handling that would cause failures.

### Warning
Issues that should be addressed but do not block progress. These
include inconsistent patterns, suboptimal implementations, or minor
deviations from conventions.

### Observation
Suggestions for improvement that are not urgent. These include
readability improvements, potential future maintainability concerns,
or alternative approaches worth considering.

## State Definitions
`Open` - A new finding being added, or an old finding where the proposed
fix is deemed inadequate.

`Fixed` - The code has been changed to remediate or eliminate the finding
pending review.

`Resolved` - A proposed fix for the issue has been accepted as complete and
correct.

`Deferred` - The issue is deemed worthwhile but outside the current scope.

`Ignored` - The issue is deemed insignificant or erroneous.

## Internal state mappings to orchestration state

`Open` and `Fixed` are counted in the `open` state for the orchestrator.

`Resolved`, `Deferred`, and `Ignored` are counted in the `closed` state for the
orchestrator.


# Field definitions

## ID
A unique identifier for a finding. It begins with a letter based on its 
severity followed by a sequential two digit number that is unique in its 
severity category. EG C01, C02, W01, O01

## State
The state of the finding chosen from the state definitions described above.

## Summary
A brief overview of the finding including specific code lines and any 
negative consequences of ignoring this finding.


## Resolution
A summary of steps take to resolve the finding or brief justification 
for deferring or ignoring it.

# Recording Format
## Critical Issues

---

ID: (ID) State: (State)

Summary:

Text

Resolution:

Text

---

## Warnings

---

ID: (ID) State: (State)

Summary:

Text

Resolution:

Text

---

## Observations

---

ID: (ID) State: (State)

Summary:

Text

Resolution:

Text

---

