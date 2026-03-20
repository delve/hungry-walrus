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
- Throws `IllegalArgumentException` if `recipe.totalWeightG <= 0.0`. Guard against this in `CreateRecipeViewModel` and `AddEntryViewModel`.
- All use cases annotated `@Inject constructor()` — Hilt injects them into ViewModels with no extra DI module configuration required.

### `ValidateFoodDataUseCase`
- `AddEntryViewModel` should call `isComplete(result)` after a food search to decide whether to show the missing-values screen.
- On the missing-values screen, call `applyOverrides(result, kcal, protein, carbs, fat)` with the user-entered estimates. The returned result has `missingFields` re-derived — if it is now empty, the entry can be saved.
- `applyOverrides` never replaces an existing non-null value with a null override, so it is safe to call with partial user input (only pass non-null values for fields the user actually filled in).

### `ComputeRollingSummaryUseCase`
- Signature: `invoke(entries, dailyPlans, start, end): RollingSummary`
- The `SummariesViewModel` must:
  1. Call `LogEntryRepository.getEntriesForRange(start, end)` to get a `Flow<List<LogEntry>>`.
  2. For each date in the period, call `NutritionPlanRepository.getPlanForDate(date)` to build the `Map<LocalDate, NutritionPlan?>`.
  3. Pass both to this use case.
- `totalTarget` is null if every day in the period has no plan. The UI should show "Set up a nutrition plan to see targets" in this case (see design spec section 3.14).
- `dailyPlans` does not need an entry for every date — dates absent from the map are treated as null (no plan).
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
