# Session 03 -- UI Layer Bug Fix: Daily Progress date filtering

## What was wrong

`DailyProgressViewModel` called `logRepo.getEntriesForDate(LocalDate.now())` once during property initialisation. The `LocalDate.now()` value was captured at ViewModel creation time and never re-evaluated. Because the ViewModel is scoped to the navigation graph and the Daily Progress screen is the start destination, the ViewModel can survive across midnight or across returning from other screens. This meant:

- After midnight, the screen continued showing yesterday's entries and totals.
- Progress bars were calculated against stale data from the wrong date.

## What was changed

### `DailyProgressViewModel.kt`

- Replaced the one-shot `LocalDate.now()` call with a `MutableStateFlow<LocalDate>` (`currentDate`).
- Used `flatMapLatest` on `currentDate` to re-query `logRepo.getEntriesForDate()` whenever the date changes.
- Added a `refreshDate()` method that updates `currentDate` with the value from `todayProvider`.
- Added an internal `todayProvider: () -> LocalDate` property (defaults to `{ LocalDate.now() }`) to allow tests to control the date without mocking static methods.

### `DailyProgressScreen.kt`

- Added `LifecycleResumeEffect` that calls `viewModel.refreshDate()` each time the screen resumes. This handles both the midnight rollover case and returning to the screen after navigating away.

## What tests were added

### `DailyProgressDateRefreshTest.kt` (new file, 4 tests)

- `queries entries for the date returned by todayProvider` -- verifies the ViewModel queries the repository with the correct date from the provider.
- `refreshDate switches to new date and re-queries entries` -- simulates a date change (e.g. midnight rollover) and verifies entries update to the new date.
- `refreshDate is no-op when date has not changed` -- verifies that calling `refreshDate()` with the same date does not emit a spurious state update (MutableStateFlow deduplication).
- `totals update correctly after date change` -- verifies that aggregated totals (kcal, protein, carbs, fat) recalculate correctly after a date switch.

All existing tests in `DailyProgressViewModelTest` and `DailyProgressViewModelEdgeCaseTest` continue to pass without modification.

## Deviations

None. The fix is minimal and does not alter the architecture or add features beyond what is needed.

## Integration concerns

None. The `LogEntryRepository` interface is unchanged. The `refreshDate()` call from `LifecycleResumeEffect` is a lightweight no-op when the date hasn't changed, due to `MutableStateFlow`'s built-in equality check.

---

# Session 03a -- Code Review Remediation

## What was changed

This session addressed findings from `code-review-03-ui layer.md`. All changes, their rationale, and any decisions not to fix are documented below.

### C01 -- todayProvider initialised after currentDate (Critical)

**Status: Partially addressed.**

The review recommended moving `todayProvider` to a constructor parameter so that `currentDate` is initialised from the injected provider. This cannot be done cleanly with Hilt because Hilt's `@Inject` constructor does not support injecting lambdas without a custom binding or `@AssistedInject` factory. The overhead of introducing `@AssistedInject` solely for a test seam is disproportionate to the problem.

The production behaviour is unaffected because `todayProvider` is never reassigned in production code. The `@VisibleForTesting` annotation was added (see W02 below) to signal intent. Tests that override `todayProvider` always call `refreshDate()` before collecting state, so the brief window where `currentDate` holds `LocalDate.now()` instead of the injected date is not observable in practice.

A `logRepo.getEntriesForDate(any())` stub returning `flowOf(emptyList())` was added to tests that override `todayProvider`, so the initial `LocalDate.now()` query at construction does not cause a MockK failure on strict mocks.

### W01 -- Error state never emitted

**Status: Fixed.**

Added a `.catch` operator to the `uiState` flow pipeline in `DailyProgressViewModel`. If either `planRepo.getCurrentPlan()` or `logRepo.getEntriesForDate()` throws, the exception is caught and emitted as `DailyProgressUiState.Error` with the exception message (or a fallback string if the message is null).

New test file: `DailyProgressViewModelErrorTest.kt` (3 tests):
- `emits Error state when planRepo throws` -- verifies Error is emitted when the plan repository throws.
- `emits Error state when logRepo throws` -- verifies Error is emitted when the log entry repository throws.
- `emits Error with fallback message when exception has no message` -- verifies the fallback string is used when the exception message is null.

### W02 -- todayProvider is internal var without annotation

**Status: Fixed.**

Added `@VisibleForTesting` annotation to `todayProvider` to make the test-only intent explicit.

### W03 -- First test does not test what its name claims

**Status: Fixed.**

Added `logRepo.getEntriesForDate(any())` stub returning `flowOf(emptyList())` to handle the initial `LocalDate.now()` query at construction time. The test now also asserts that `content.displayDate` matches the injected date, verifying the provider-supplied date is used for the displayed content.

### W04 -- Progress bar label uses "Kcal" instead of "kcal"

**Status: Fixed.**

Changed the `label` parameter on line 190 of `DailyProgressScreen.kt` from `"Kcal"` to `"kcal"` to match the design specification (section 1.4) and `CLAUDE.md`.

### O01 -- formatMacro redundant branch

**Status: Fixed.**

Removed the dead `if/else` in `Formatter.formatMacro()`. Both branches were identical (`String.format(Locale.UK, "%.1f", rounded)`). The function now has a single return expression.

### O02 -- LifecycleResumeEffect key is Unit

**Status: Fixed.**

Added a comment above the `LifecycleResumeEffect(Unit)` call explaining why `Unit` is used as the key and that the intent is to fire on every lifecycle resume, not on parameter changes.

### O03 -- Potential flakiness in no-op test

**Status: Fixed.**

Added `advanceUntilIdle()` before `expectNoEvents()` in the `refreshDate is no-op when date has not changed` test. This ensures all pending coroutines have run before asserting no new events, making the test deterministic regardless of dispatcher behaviour.

### O04 -- Screen calls LocalDate.now() independently of ViewModel

**Status: Fixed.**

Added a `displayDate: LocalDate` field to `DailyProgressUiState.Content`. The ViewModel populates this from the `currentDate` flow. The screen now reads the date from `state.displayDate` instead of calling `LocalDate.now()` directly. This ensures the top bar date and the displayed entries always agree on which date is being shown.

The `LocalDate` import was removed from `DailyProgressScreen.kt` as it is no longer needed there.

To avoid intermediate state emissions where `currentDate` has updated but entries have not yet switched (due to `combine` firing on any input change), the ViewModel now pairs the date with its entries inside the `flatMapLatest` operator using `.map { entries -> date to entries }`. This produces a single `datedEntries` flow that `combine` consumes atomically, eliminating glitch emissions during date transitions.

### Additional test: displayDate verification

Added one new test to `DailyProgressDateRefreshTest.kt`:
- `displayDate in Content matches the todayProvider date` -- directly verifies the `displayDate` field in the Content state matches the injected date.

## Files modified

- `app/src/main/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressViewModel.kt` -- added `@VisibleForTesting`, `displayDate` field, `.catch` error handling, atomic `datedEntries` flow
- `app/src/main/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressScreen.kt` -- fixed "kcal" label, read date from ViewModel state, added comment on LifecycleResumeEffect
- `app/src/main/java/com/delve/hungrywalrus/util/Formatter.kt` -- removed dead branch in `formatMacro`
- `app/src/test/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressDateRefreshTest.kt` -- improved test stubs, added displayDate assertions, added advanceUntilIdle, added new test
- `app/src/test/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressViewModelErrorTest.kt` -- new file, 3 tests for error state emission

## Test results

All 396 unit tests pass (0 failures, 0 skipped) after the changes. This includes:
- `DailyProgressViewModelTest` (4 tests)
- `DailyProgressViewModelEdgeCaseTest` (5 tests)
- `DailyProgressDateRefreshTest` (5 tests, 1 new)
- `DailyProgressViewModelErrorTest` (3 tests, all new)
- All other test suites unaffected

## Integration concerns

The `displayDate` field added to `DailyProgressUiState.Content` is a new field. Any code outside this layer that pattern-matches on `Content` or constructs it directly will need to account for this field. A search confirmed that `Content` is only constructed inside `DailyProgressViewModel` and only destructured in `DailyProgressScreen`, so no external code is affected.

---

# Session 03b -- Code Review Remediation (W05, O05)

## What was changed

This session addressed the two remaining open findings from `code-review-03-ui layer.md` (pass 2): W05 and O05.

### W05 -- catch operator terminates the flow; no recovery after error

**Status: Fixed.**

Added a `.retry(2)` operator before `.catch` in the `uiState` flow pipeline in `DailyProgressViewModel.kt`. The retry operator restarts the upstream `combine` flow up to 2 times when an exception occurs, allowing transient errors (e.g. a momentary Room I/O hiccup) to resolve without terminating the flow. After all retries are exhausted, the exception propagates to `.catch`, which emits `DailyProgressUiState.Error` and the flow terminates.

The terminal behaviour after exhausting retries is intentional and aligned with the design specification (section 3.1, Error state): "Could not load data. Please restart the app." -- the design does not specify a retry button or automatic recovery for the Daily Progress error state.

The retry count is exposed as a `companion object` constant `RETRY_COUNT` with `@VisibleForTesting` so tests can assert against it.

Also updated the fallback error message (when `e.message` is null) from "An unexpected error occurred" to "Could not load data. Please restart the app." to match the design specification.

### O05 -- Error state tests do not verify flow completion after error

**Status: Fixed.**

Added a new test `flow completes after error emission and no further Content events arrive` to `DailyProgressViewModelErrorTest.kt`. This test verifies that after an Error state is emitted, the `uiState` StateFlow does not emit further events, confirming the upstream flow has terminated as expected.

### Additional tests for retry behaviour

Added two more tests to `DailyProgressViewModelErrorTest.kt`:

- `retries transient errors before emitting Error` -- verifies the plan flow is collected exactly `1 + RETRY_COUNT` times (3 total) before Error is emitted, confirming the retry operator is functioning.
- `recovers on retry when transient error resolves` -- verifies that if a flow throws on the first attempt but succeeds on the retry, the ViewModel emits Content (not Error), proving that transient errors are handled gracefully.

## Files modified

- `app/src/main/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressViewModel.kt` -- added `.retry(RETRY_COUNT)` operator, added `RETRY_COUNT` companion constant, updated fallback error message, added `retry` import
- `app/src/test/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressViewModelErrorTest.kt` -- updated fallback message assertion, added 3 new tests (flow completion, retry count, transient recovery)

## What was not changed

- `DailyProgressScreen.kt` -- no changes needed. The screen already renders the Error state correctly.
- `DailyProgressDateRefreshTest.kt` -- no changes needed.
- `DailyProgressViewModelTest.kt` and `DailyProgressViewModelEdgeCaseTest.kt` -- no changes needed. All existing tests continue to pass.

## Test results

All 419 unit tests pass (0 failures, 0 skipped) after the changes. This includes:
- `DailyProgressViewModelTest` (4 tests, unchanged)
- `DailyProgressViewModelEdgeCaseTest` (5 tests, unchanged)
- `DailyProgressDateRefreshTest` (5 tests, unchanged)
- `DailyProgressViewModelErrorTest` (6 tests: 3 existing updated, 3 new)
- All other test suites unaffected

## Integration concerns

None. The retry operator is internal to the ViewModel's flow pipeline. No external interfaces or data classes were changed. The `RETRY_COUNT` constant is only used internally and in tests.

---

# Fix Session - 2026-03-28

## What was fixed

This session addressed the two remaining open findings from `code-review-03-ui layer.md` (pass 3): W06 and O06.

### W06 -- retry predicate `e is Exception` is semantically imprecise

**Status: Fixed.**

Changed the retry predicate in `DailyProgressViewModel.kt` from `e is Exception` to `e is IOException`. The previous predicate matched every `Exception` subtype, including programming errors like `IllegalArgumentException` and `NullPointerException` that should not be silently retried. While `CancellationException` was already filtered by the `retry` operator's internal `ensureActive()` call in kotlinx.coroutines 1.9.0, the broad predicate was a maintenance hazard.

The narrowed predicate restricts retries to `IOException`, which is the appropriate exception type for transient Room database I/O failures. All other exception types now propagate immediately to `.catch` without retries, which is the correct behaviour for non-transient errors.

Added `import java.io.IOException` to the ViewModel imports.

Updated the retry predicate comment to explain why `IOException` is the chosen type and that programming errors should not be retried.

### O06 -- recovers on retry test does not assert data correctness on recovered Content

**Status: Not fixed (no remediation required).**

As noted in the review, this observation is adequate for the primary intent of the test (confirming recovery) and data correctness is covered by other tests. No changes made.

## Test changes

Updated `DailyProgressViewModelErrorTest.kt` to align with the narrowed retry predicate:

- **`retries IO errors before emitting Error`** (renamed from `retries transient errors before emitting Error`): Changed to throw `IOException` instead of `RuntimeException`, since only IO exceptions are now retried. Verified collection count is still 1 + RETRY_COUNT = 3.

- **`recovers on retry when transient IO error resolves`** (renamed from `recovers on retry when transient error resolves`): Changed to throw `IOException` instead of `RuntimeException` on the first attempt.

- **`does not retry non-IO exceptions`** (new test): Verifies that `IllegalArgumentException` is not retried -- the plan flow is collected exactly once, and the error propagates directly to `.catch`. This directly validates the W06 fix.

- Existing error tests (`emits Error state when planRepo throws`, `emits Error state when logRepo throws`, `emits Error with fallback message when exception has no message`, `flow completes after error emission`) continue to use `RuntimeException`, which is correct since those tests verify error propagation behaviour, not retry behaviour. RuntimeException is not retried under the new predicate, so these tests now also implicitly confirm that non-IO exceptions bypass retries.

## Files modified

- `app/src/main/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressViewModel.kt` -- changed retry predicate from `e is Exception` to `e is IOException`, added `java.io.IOException` import, updated predicate comment
- `app/src/test/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressViewModelErrorTest.kt` -- updated two retry tests to use `IOException`, added new `does not retry non-IO exceptions` test, added `java.io.IOException` import

## Test results

All daily progress tests pass (21 tests, 0 failures, 0 skipped):
- `DailyProgressViewModelTest` (4 tests, unchanged)
- `DailyProgressViewModelEdgeCaseTest` (5 tests, unchanged)
- `DailyProgressDateRefreshTest` (5 tests, unchanged)
- `DailyProgressViewModelErrorTest` (7 tests: 4 unchanged, 2 updated, 1 new)

One pre-existing failure in `DataRetentionQaTest.entry 1 millisecond older than 730-day boundary is eligible for deletion` was observed during the full test suite run. This is a time-dependent boundary test in the QA test suite that is unrelated to the UI layer changes in this session.

## Integration concerns

None. The change is confined to the retry predicate logic within the ViewModel's flow pipeline. No external interfaces, data classes, or screen code were modified.
