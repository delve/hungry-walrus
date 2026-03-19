# Code Review 03 -- UI Layer Bug Fix: Daily Progress date filtering

Reviewer: Code Review Agent (pass 1, pass 2, pass 3, pass 4)
Date: 2026-03-31
Branch: firstpass
Files reviewed (pass 1):
- `app/src/main/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressViewModel.kt`
- `app/src/main/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressScreen.kt`
- `app/src/test/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressDateRefreshTest.kt`

Additional files reviewed (pass 2):
- `app/src/main/java/com/delve/hungrywalrus/util/Formatter.kt`
- `app/src/test/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressViewModelErrorTest.kt`
- `app/src/test/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressViewModelTest.kt`
- `app/src/test/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressViewModelEdgeCaseTest.kt`

Additional files reviewed (pass 3):
- `app/src/main/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressViewModel.kt` (re-read)
- `app/src/test/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressViewModelErrorTest.kt` (re-read)
- `app/src/test/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressDateRefreshTest.kt` (re-read)

Additional files reviewed (pass 4):
- `app/src/main/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressViewModel.kt` (re-read)
- `app/src/main/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressScreen.kt` (re-read)
- `app/src/main/java/com/delve/hungrywalrus/util/Formatter.kt` (re-read)
- `app/src/test/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressViewModelErrorTest.kt` (re-read)
- `app/src/test/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressDateRefreshTest.kt` (re-read)

---

## Summary

### Pass 1

This session fixed a date-staleness bug in `DailyProgressViewModel` where
`LocalDate.now()` was captured once at ViewModel construction time and never
re-evaluated. The fix replaces the one-shot call with a `MutableStateFlow<LocalDate>`
driven by a `todayProvider` lambda, and adds a `LifecycleResumeEffect` in the
screen composable to call `refreshDate()` on every resume. Four new unit tests
cover the refresh behaviour.

The fix is architecturally sound, minimal in scope, and does not break any
existing tests. The coroutine flow chain (`flatMapLatest` on `currentDate`) is
correct for the use case. One critical issue and several warnings existed,
primarily around the initialisation ordering of `todayProvider` relative to
`currentDate`, and secondary concerns about an unreachable `Error` state,
`internal` visibility of a test seam, and a minor labelling inconsistency in the UI.

### Pass 2

The remediation session (03a) addressed all six warnings and observations from
pass 1. The critical finding C01 was partially addressed with a documented
rationale. The code is now in a substantially improved state. The `.catch`
operator, `@VisibleForTesting` annotation, `displayDate` field in `Content`,
the `datedEntries` atomic pairing, the `kcal` label fix, and the `formatMacro`
dead-branch removal are all confirmed present in the source. Three new error
state tests are well-structured and meaningful. The `LifecycleResumeEffect`
comment is present and clear. No new critical issues were found in this pass.
One new warning and one new observation were identified.

### Pass 3

Session 03b addressed both remaining open findings: W05 and O05.

W05 is resolved: `.retry(RETRY_COUNT)` was added before `.catch` in the
`uiState` pipeline, and the terminal-after-retry behaviour is explicitly
documented and aligned with design spec section 3.1. The `RETRY_COUNT`
companion constant is correctly typed as `Long` to match the `retry` operator's
signature and is annotated `@VisibleForTesting`.

O05 is resolved: A new test `flow completes after error emission and no further
Content events arrive` directly verifies termination behaviour, and two
additional tests validate retry count and transient-error recovery.

No regressions were found in the previously resolved findings. One new warning
(W06) and one new observation (O06) were identified in this pass, both relating
to the newly introduced retry predicate.

### Pass 4

The fix session (2026-03-28) addressed W06. The retry predicate was narrowed
from `e is Exception` to `e is IOException`, with an explanatory comment added
in the ViewModel. A corresponding import (`java.io.IOException`) was added.
Three tests in `DailyProgressViewModelErrorTest.kt` were updated or added to
cover the narrowed predicate: `retries IO errors before emitting Error`,
`recovers on retry when transient IO error resolves`, and the new `does not
retry non-IO exceptions`.

W06 is now resolved. O06 remains open but the developer explicitly documented
that no remediation is required, and the rationale is sound: the primary intent
of the test (confirming recovery) is met, and data correctness is covered
elsewhere. All other previously resolved findings were verified against source
and show no regressions. No new findings were identified in this pass.

The ViewModel and test files are in a clean, well-structured state. The
`IOException`-scoped retry predicate is idiomatic and the test coverage for
error and retry behaviour is thorough.

---

## Critical Issues

### C01 -- `todayProvider` initialised after `currentDate`, so construction-time date always uses `LocalDate.now()`

**Status: Deferred -- rationale accepted with residual concern**

**File**: `DailyProgressViewModel.kt`, lines 53--56

The developer's rationale (03a notes) is that introducing `@AssistedInject`
solely for this test seam is disproportionate. The production behaviour is
unaffected. Tests stub `logRepo.getEntriesForDate(any())` before overriding
`todayProvider`, so the initial `LocalDate.now()` query at construction does
not cause a MockK failure. The deferred status is accepted.

The residual concern is that the test-seam pattern remains a mutable `internal
var` on the ViewModel (mitigated by `@VisibleForTesting` -- see W02 resolution
below). Any future developer adding a subscriber to `uiState` before calling
`refreshDate()` in a test will receive the wall-clock date rather than the
injected one, and the test comment does not warn of this. This is noted but not
re-raised as a new finding.

Pass 4 note: Verified in source. No change to this finding's status. The
`@VisibleForTesting` annotation and the explanatory KDoc comment remain present
at lines 48--54. The residual concern is unchanged.

---

## Warnings

### W01 -- `DailyProgressUiState.Error` is declared but never emitted

**Status: Resolved**

A `.catch` operator was added to the `uiState` flow pipeline in
`DailyProgressViewModel.kt` (line 88). The catch block emits
`DailyProgressUiState.Error` with the exception message or a fallback string.
Three new tests in `DailyProgressViewModelErrorTest.kt` verify the `planRepo`
throws, `logRepo` throws, and null-message cases respectively. The tests are
meaningful and correctly structured.

### W02 -- `todayProvider` is `internal var` -- mutable test seam exposed on the production type

**Status: Resolved**

`@VisibleForTesting` was added at line 53 of `DailyProgressViewModel.kt`, and
a KDoc comment explains the intent. The annotation makes the test-only purpose
explicit to any future caller in the module.

### W03 -- First test in `DailyProgressDateRefreshTest` does not test what its name claims

**Status: Resolved**

`every { logRepo.getEntriesForDate(any()) } returns flowOf(emptyList())` was
added at line 48 before the specific `fixedDate` stub at line 49. The test now
also asserts `content.displayDate == fixedDate` at line 59, confirming the
provider-supplied date reached the Content state. The test name now aligns with
what is actually verified.

### W04 -- Progress bar label uses "Kcal" instead of "kcal"

**Status: Resolved**

`DailyProgressScreen.kt` line 198 now reads `label = "kcal"`. Confirmed in
source.

### W05 -- `catch` operator placement means errors are terminal: no recovery path after an error

**Status: Resolved**

**Fix verified in pass 3.**

`.retry(RETRY_COUNT)` (where `RETRY_COUNT = 2L`) was inserted before `.catch`
in `DailyProgressViewModel.kt` at line 81. The retry operator re-subscribes to
the upstream `combine` flow (and transitively to `planRepo.getCurrentPlan()` and
`datedEntries`) up to twice before the exception propagates to `.catch`. After
all retries are exhausted, `.catch` emits `DailyProgressUiState.Error` and the
flow terminates as before.

The terminal behaviour after exhausting retries is explicitly documented with a
comment at lines 81--82 and is cited in the developer notes as intentionally
aligned with design spec section 3.1. The `RETRY_COUNT` constant is declared in
a `companion object` with `@VisibleForTesting` and `internal` visibility,
allowing tests to reference it without hardcoding the value.

Three tests in `DailyProgressViewModelErrorTest.kt` verify the retry behaviour:
`flow completes after error emission and no further Content events arrive`
(line 95), `retries transient errors before emitting Error` (line 119), and
`recovers on retry when transient error resolves` (line 146). All are
correctly structured and meaningful. The recovery test correctly verifies that
a transient failure on the first attempt does not prevent a `Content` emission
on a successful retry.

### W06 -- `retry` predicate `e is Exception` is semantically imprecise

**Status: Resolved**

**Fix verified in pass 4.**

The predicate at `DailyProgressViewModel.kt` line 87 was narrowed from
`e is Exception` to `e is IOException`. The `java.io.IOException` import was
added at line 22. A comment at lines 83--87 explains the rationale: only
transient Room I/O failures should trigger a retry; programming errors such as
`IllegalArgumentException` or `NullPointerException` should propagate directly
to `.catch` without retries.

Three tests in `DailyProgressViewModelErrorTest.kt` validate this behaviour
directly:
- `retries IO errors before emitting Error` (line 119): uses `IOException`,
  asserts collection count equals `1 + RETRY_COUNT = 3`.
- `recovers on retry when transient IO error resolves` (line 146): uses
  `IOException` on the first attempt, asserts `Content` is emitted on recovery.
- `does not retry non-IO exceptions` (line 173): uses `IllegalArgumentException`,
  asserts collection count is exactly 1 and the error message is propagated
  without retries.

All three tests are correctly structured. The existing error tests
(`emits Error state when planRepo throws`, `emits Error state when logRepo
throws`, `emits Error with fallback message when exception has no message`,
`flow completes after error emission`) continue to use `RuntimeException`, which
correctly exercises the non-retryable path under the new predicate.

---

## Observations

### O01 -- `formatMacro` in `Formatter.kt` has redundant branch in its `if` expression

**Status: Resolved**

The dead `if/else` was removed. `Formatter.formatMacro` now has a single return
expression at line 58. Confirmed in source.

### O02 -- `LifecycleResumeEffect` key is `Unit` -- acceptable but worth noting

**Status: Resolved**

A comment was added at lines 63--65 of `DailyProgressScreen.kt` explaining
that `Unit` is intentional because the effect should fire on every lifecycle
resume, not on parameter changes. Confirmed in source.

### O03 -- `refreshDate is no-op when date has not changed` test has a potential flakiness concern

**Status: Resolved**

`advanceUntilIdle()` was added at line 126 of `DailyProgressDateRefreshTest.kt`
before `expectNoEvents()`. The test is now deterministic. Confirmed in source.

### O04 -- `DailyProgressScreen.kt` imports `java.time.LocalDate` but only uses it for the top-bar date display

**Status: Resolved**

The screen no longer calls `LocalDate.now()` directly. The date is now read
from `state.displayDate` (line 99 of `DailyProgressScreen.kt`). The
`java.time.LocalDate` import was removed from the screen. The `displayDate`
field was added to `DailyProgressUiState.Content`, populated from the
`datedEntries` flow which pairs the date with its entries atomically. Confirmed
in source.

### O05 -- Error state test does not verify the flow terminates gracefully after error emission

**Status: Resolved**

**Fix verified in pass 3.**

The test `flow completes after error emission and no further Content events
arrive` was added to `DailyProgressViewModelErrorTest.kt` at line 95. It
collects the `Loading` and `Error` states, then calls `advanceUntilIdle()` and
asserts `expectNoEvents()`, confirming the upstream flow has terminated and no
further emissions occur. This directly addresses the coverage gap identified in
pass 2.

### O06 -- `recovers on retry` test does not assert data correctness on recovered Content

**Status: Open -- no remediation required, rationale accepted**

**File**: `DailyProgressViewModelErrorTest.kt`, lines 145--170

The `recovers on retry when transient IO error resolves` test verifies that a
`Content` state is emitted after a transient failure is resolved on retry. The
assertion at line 164 uses only a type check (`content is
DailyProgressUiState.Content`). The test does not assert the content of the
recovered state (e.g. `displayDate`, `plan`, `entries`, or totals).

The developer explicitly declined remediation in the fix session notes
(2026-03-28), stating that the primary intent of the test is confirming
recovery rather than data correctness, and that data correctness is covered
by other tests in `DailyProgressViewModelTest` and
`DailyProgressViewModelEdgeCaseTest`. This rationale is sound. The finding
is retained for transparency but does not require remediation. It will not
be re-raised in future passes.
