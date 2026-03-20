# Developer Notes: Session 02 — Domain Layer

## What was implemented

### New domain model (1 file)
- `domain/model/RollingSummary.kt` — computed result for 7-day or 28-day summaries: period intake totals, optional plan targets (null when no plan configured), and daily average

### Use cases (3 files)
- `domain/usecase/ScaleNutritionUseCase.kt` — scales per-100g nutrition values to a consumed weight; separate overload for recipe portions
- `domain/usecase/ValidateFoodDataUseCase.kt` — checks `FoodSearchResult` completeness and applies user-supplied overrides for missing fields; always re-derives `missingFields`
- `domain/usecase/ComputeRollingSummaryUseCase.kt` — pure function accepting pre-fetched entries and a per-day plan map; computes intake totals, plan targets (accounting for mid-period changes), and daily averages

### Deleted placeholder
- `domain/usecase/package-info.kt` — replaced by the three real use case files

## Deviations from architecture

None. All implementations exactly match the patterns described in architecture sections 3, 12, and 7.2.

## Integration notes for ViewModel layer

### `ScaleNutritionUseCase`
- Two call paths:
  - `invoke(kcalPer100g, proteinPer100g, carbsPer100g, fatPer100g, weightG): NutritionValues` — for API/manual food entries
  - `scaleRecipePortion(recipe, portionWeightG): NutritionValues` — for recipe portions
- Throws `IllegalArgumentException` if `weightG < 0.0`. Throws `IllegalArgumentException` if `recipe.totalWeightG <= 0.0`. Both guards live in the use case — callers do not need to duplicate them.
- All use cases annotated `@Inject constructor()` — Hilt injects them into ViewModels with no extra DI module configuration required.

### `ValidateFoodDataUseCase`
- `AddEntryViewModel` should call `isComplete(result)` after a food search to decide whether to show the missing-values screen.
- On the missing-values screen, call `applyOverrides(result, kcal, protein, carbs, fat)` with the user-entered estimates. The returned result has `missingFields` re-derived — if it is now empty, the entry can be saved.
- `applyOverrides` never replaces an existing non-null value with a null override, so it is safe to call with partial user input (only pass non-null values for fields the user actually filled in).

### `ComputeRollingSummaryUseCase`
- Signature: `invoke(entries, dailyPlans, start, end): RollingSummary`
- Throws `IllegalArgumentException` if `start` is after `end`.
- The `SummariesViewModel` must:
  1. Call `LogEntryRepository.getEntriesForRange(start, end)` to get a `Flow<List<LogEntry>>`.
  2. For each date in the period, call `NutritionPlanRepository.getPlanForDate(date)` to build the `Map<LocalDate, NutritionPlan?>`.
  3. Pass both to this use case.
- `totalTarget` is null if **any** day in the period has no plan entry (i.e. partial-plan coverage also yields null). The UI should show "Set up a nutrition plan to see targets" in this case (see design spec section 3.14).
- `dailyPlans` does not need an entry for every date — dates absent from the map are treated as null (no plan), which will cause `totalTarget` to be null for the period.
- `NutritionPlan.kcalTarget` is `Int`; it is widened to `Double` during accumulation. This is transparent to callers.

### No new repository methods required
The three use cases work with data already available from the existing repository interfaces. No repository changes are needed.

## Unit tests written

### `domain/usecase/ScaleNutritionUseCaseTest.kt` (5 tests)
- Scales per-100g values correctly for a given weight
- Returns zero values for zero weight
- Scales recipe to 50% portion correctly
- Full portion weight returns recipe totals
- Throws `IllegalArgumentException` for zero recipe weight

### `domain/usecase/ValidateFoodDataUseCaseTest.kt` (6 tests)
- `isComplete` returns true when `missingFields` is empty
- `isComplete` returns false when `missingFields` is non-empty
- `applyOverrides` fills a single missing field
- `applyOverrides` fills multiple missing fields
- `applyOverrides` preserves existing values when override is null
- `applyOverrides` re-derives `missingFields` after a partial override

### `domain/usecase/ComputeRollingSummaryUseCaseTest.kt` (8 tests)
- Correct total intake summed from entries
- Correct daily average (total ÷ periodDays)
- Null target when no plans configured
- Summed targets for a uniform plan across the period
- Handles mid-period plan changes (planA for first 3 days, planB for final 4 days)
- Zero intake for empty entries list
- Period days is inclusive of start and end (14–20 Mar = 7 days)
- `startDate` and `endDate` are preserved in the returned `RollingSummary`

**Total: 19 domain-layer unit tests, 0 failures, 0 errors.**

## Test run results

```
ScaleNutritionUseCaseTest:       5 passed
ValidateFoodDataUseCaseTest:     6 passed
ComputeRollingSummaryUseCaseTest: 8 passed

Total: 19 domain-layer tests, 0 failures, 0 errors.
```

Data-layer tests (49) were unaffected. Full test run: 68 tests, 0 failures.

## Notes for the next session

- **`SummariesViewModel`**: must call `getPlanForDate()` for each day in the selected period (7 or 28 calls). These are lightweight local DB reads. Consider whether to run them as a single coroutine block in `viewModelScope.launch`.
- **`AddEntryViewModel`**: the multi-step add-entry flow should hold the current `FoodSearchResult` as a state field and update it in-place via `ValidateFoodDataUseCase.applyOverrides()` as the user fills in missing values.
- **`ScaleNutritionUseCase` for manual entry**: when the user enters nutrition values directly (manual entry screen), the UI already has the final values — no scaling is needed. The use case is only relevant when the source is API-backed with per-100g reference values.
- **Recipe totals computation** (`CreateRecipeViewModel`): ingredient totals are computed at save time using the formula `(valuePer100g / 100.0) * weightG`. This is the same formula as `ScaleNutritionUseCase.invoke()`, so `CreateRecipeViewModel` should call the use case rather than duplicating the formula inline.

---

## Fix Session: Code Review 02 Findings

*Date: 2026-03-20*

### Changes made

#### m1 — Negative weight guard in `ScaleNutritionUseCase.invoke`

Added `require(weightG >= 0.0) { "weightG must not be negative" }` at the top of the `invoke` operator function, mirroring the existing guard in `scaleRecipePortion`. Zero weight remains valid (produces all-zero output, representing a "weigh nothing" scenario).

#### s1 — Test for negative weight in `ScaleNutritionUseCaseTest`

Added `invoke throws for negative weight` test using `@Test(expected = IllegalArgumentException::class)` with `weightG = -1.0`.

#### m2 — Partial-plan contract in `ComputeRollingSummaryUseCase`

Replaced the `hasAnyPlan` boolean flag with a `planCoveredDays` counter. `totalTarget` is now non-null only when `planCoveredDays == periodDays` — i.e. every day in the period has a plan entry. Partial coverage (some days have a plan, some do not) now correctly yields `null`, preventing the misleading partial-sum display identified in the review.

**Contract decision:** Return null unless all days have a plan. This is option 2 from the review recommendations. Rationale: a partial sum (e.g. one day's target displayed as a 7-day target) is worse than no target at all; the UI already has a clear "no target configured" state. The integration note for `SummariesViewModel` has been updated to reflect this contract.

#### s3 — Start-after-end guard in `ComputeRollingSummaryUseCase`

Added `require(!start.isAfter(end)) { "start must not be after end" }` at the top of `invoke` to produce a clear `IllegalArgumentException` rather than silent `NaN` propagation from a negative `periodDays` divisor.

#### New tests in `ComputeRollingSummaryUseCaseTest`

Two tests added:
- `returns null target when only some days have a plan` — 3 of 7 days covered, asserts `totalTarget` is null.
- `throws when start is after end` — `@Test(expected = IllegalArgumentException::class)` passing `end` as start and `start` as end.

#### s2 — `isComplete` thin alias in `ValidateFoodDataUseCase`

No action taken. The review correctly identifies this as a non-defect. `isComplete` is co-located with `applyOverrides` in the same use case; keeping both together is the right pattern for the ViewModel call flow.

### Test run results after fixes

```
ScaleNutritionUseCaseTest:        6 passed  (+1 negative weight test)
ValidateFoodDataUseCaseTest:      6 passed  (unchanged)
ComputeRollingSummaryUseCaseTest: 10 passed (+2 new tests)

Domain-layer total: 22 tests, 0 failures, 0 errors.
Full suite total:   88 tests, 0 failures, 0 errors.
```

---

## Fix Session: Code Review 02 Findings (second pass)

*Date: 2026-03-20*

### Changes made

#### m1 — Negative portionWeightG guard in `ScaleNutritionUseCase.scaleRecipePortion`

Added `require(portionWeightG >= 0.0) { "portionWeightG must not be negative" }` at the top of `scaleRecipePortion`, before the existing recipe weight guard. The order is: portionWeightG check first, then recipe.totalWeightG check — consistent with the parameter order in the function signature. Updated the KDoc `@throws` block to document the new guard.

#### s1 — Updated KDoc on `RollingSummary.totalTarget`

Replaced the stale description (_"null when no nutrition plan was configured for **any** day in the period"_) with the accurate contract: _"null when one or more days in the period have no active nutrition plan. It is non-null only when every day in the period has a plan entry."_

#### s2 — Single-day period test in `ComputeRollingSummaryUseCaseTest`

Added `single-day period has periodDays 1 and dailyAverage equals totalIntake` test using `start == end == 2026-03-20`. Asserts `periodDays == 1` and that each `dailyAverage` field equals the corresponding `totalIntake` field (confirming the `/ periodDays` formula is correct at its minimum boundary).

#### s3 — Negative portionWeightG test in `ScaleNutritionUseCaseTest`

Added `scaleRecipePortion throws for negative portionWeightG` annotated `@Test(expected = IllegalArgumentException::class)` passing `portionWeightG = -1.0`, parallel to the existing `scaleRecipePortion throws for zero recipe weight` test.

### Test run results after fixes

```
ScaleNutritionUseCaseTest:        7 passed  (+1 negative portionWeightG test)
ValidateFoodDataUseCaseTest:      6 passed  (unchanged)
ComputeRollingSummaryUseCaseTest: 11 passed (+1 single-day period test)

Domain-layer total: 24 tests, 0 failures, 0 errors.
Full suite total:   90 tests, 0 failures, 0 errors.
```
