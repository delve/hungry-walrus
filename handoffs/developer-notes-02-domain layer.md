# Developer Notes: Session 02 -- Domain Layer Review

## What this session did

Reviewed the domain layer against the current Architecture (Revision 1, 2026-03-22),
Requirements, and Design specifications. The domain layer was already fully implemented
in a prior commit. This session performed a complete verification pass: every model and
use case was checked against the spec, tests were run to confirm all pass, and no code
changes were required.

---

## Domain Layer Inventory

### Domain Models (`domain/model/`)

| File | Contents | Status |
|---|---|---|
| `NutritionValues.kt` | `data class NutritionValues(kcal, proteinG, carbsG, fatG)` | ✅ Matches Section 6.3 |
| `FoodSearchResult.kt` | `data class FoodSearchResult(id, name, source, kcalPer100g?, proteinPer100g?, carbsPer100g?, fatPer100g?, missingFields)` | ✅ Matches Section 6.3 |
| `FoodSource.kt` | `enum class FoodSource { USDA, OPEN_FOOD_FACTS, MANUAL }` | ✅ Matches Section 6.3 |
| `NutritionField.kt` | `enum class NutritionField { KCAL, PROTEIN, CARBS, FAT }` | ✅ Matches Section 6.3 |
| `NutritionPlan.kt` | `data class NutritionPlan(id, kcalTarget, proteinTargetG, carbsTargetG, fatTargetG, effectiveFrom)` | ✅ Matches Section 5.2 |
| `LogEntry.kt` | `data class LogEntry(id, foodName, kcal, proteinG, carbsG, fatG, timestamp)` | ✅ Matches Section 5.2 |
| `Recipe.kt` | `data class Recipe(id, name, totalWeightG, totalKcal, totalProteinG, totalCarbsG, totalFatG, createdAt, updatedAt)` | ✅ Matches Section 5.2 |
| `RecipeIngredient.kt` | `data class RecipeIngredient(id, recipeId, foodName, weightG, kcalPer100g, proteinPer100g, carbsPer100g, fatPer100g)` | ✅ Matches Section 5.2 |
| `RecipeWithIngredients.kt` | `data class RecipeWithIngredients(recipe, ingredients)` | ✅ Matches Section 6.1 |
| `RollingSummary.kt` | `data class RollingSummary(periodDays, startDate, endDate, totalIntake, totalTarget?, dailyAverage)` | ✅ Matches Section 7.5 |
| `OfflineException.kt` | Custom exception for offline/no-network errors | ✅ Matches Section 8.4 |

### Use Cases (`domain/usecase/`)

Architecture Section 3 defines three non-trivial use cases for the domain layer:

| Use Case | Purpose | Status |
|---|---|---|
| `ScaleNutritionUseCase` | Scales per-100g nutrition to consumed weight (Sections 12.1, 12.2); also scales recipe portions. Two overloads: `invoke()` for per-100g → weight, `scaleRecipePortion()` for recipe → portion. | ✅ Implemented |
| `ValidateFoodDataUseCase` | Validates completeness of `FoodSearchResult`; applies user-supplied overrides for missing fields; re-derives `missingFields` after each override (Section 6.3). | ✅ Implemented |
| `ComputeRollingSummaryUseCase` | Computes a `RollingSummary` for a date range given log entries and per-day plan history. Handles full and partial plan coverage, mid-period plan changes. Pure function with no I/O. (Sections 7.5, 17.7). | ✅ Implemented |

All three use cases carry `@Inject constructor()` for Hilt injection.

---

## Architecture Spec Conformance

### Section 3 (Architecture Pattern)

> Domain Layer: Contains use cases only where business logic is non-trivial.
> Simple CRUD flows pass through directly from ViewModel to repository.

Confirmed: the domain layer contains exactly the three non-trivial use cases called out
in the spec. There are no spurious use cases and no missing ones.

### Section 5.2 (Entity Definitions)

All domain models carry the correct field names and types as specified. Key points verified:

- `NutritionPlan.effectiveFrom` is `Long` (epoch millis). ✅
- `LogEntry` stores only final calculated values; no weight field. ✅
  (Architecture Section 5.2: "The consumed weight is not stored in the log entry.")
- `RecipeIngredient` stores per-100g reference values (non-nullable), not final values.
  A `FoodSearchResult` with missing fields must have all values resolved before constructing
  a `RecipeIngredient`. This is documented in the class KDoc. ✅
- `FoodSearchResult.missingFields` is always consistent with nullable per-100g fields —
  `ValidateFoodDataUseCase.applyOverrides` re-derives it after every override. ✅

### Section 6.3 (Domain Models)

`FoodSource` enum values confirmed: `USDA`, `OPEN_FOOD_FACTS`, `MANUAL`. The architecture
spec lists `FoodSource { USDA, OPEN_FOOD_FACTS, MANUAL }` — all three values are present. ✅

### Section 8.4 (Error Handling)

`OfflineException` is the error type for `IOException`/offline scenarios. It extends
`Exception` (not `RuntimeException`) and is caught in the repository layer to produce
`Result.failure(OfflineException(...))`. ✅

### Section 12.1–12.3 (Nutrition Value Scaling)

`ScaleNutritionUseCase` implements:
- `invoke()`: `scaledValue = (valuePer100g / 100.0) * weightG` ✅
- `scaleRecipePortion()`: `scaledValue = (recipeTotalValue / recipeTotalWeightG) * portionWeightG` ✅
- Manual entry: no use case involvement; values are passed directly in `AddEntryViewModel.setDirectEntry()`. ✅

---

## Tests

No new tests were written in this session. All existing tests were verified to pass.

### Domain use case tests (53 total)

| Test class | Tests | Passed | Failed |
|---|---|---|---|
| `ComputeRollingSummaryUseCaseEdgeCaseTest` | 11 | 11 | 0 |
| `ComputeRollingSummaryUseCaseTest` | 11 | 11 | 0 |
| `ScaleNutritionUseCaseEdgeCaseTest` | 9 | 9 | 0 |
| `ScaleNutritionUseCaseTest` | 7 | 7 | 0 |
| `ValidateFoodDataUseCaseEdgeCaseTest` | 9 | 9 | 0 |
| `ValidateFoodDataUseCaseTest` | 6 | 6 | 0 |

### Full test suite

```
./gradlew testDebugUnitTest -- BUILD SUCCESSFUL
```

All 277 tests pass.

| Test class | Tests | Passed | Failed |
|---|---|---|---|
| `OffResponseMapperTest` | 6 | 6 | 0 |
| `UsdaResponseMapperTest` | 5 | 5 | 0 |
| `FoodLookupRepositoryImplTest` | 20 | 20 | 0 |
| `LogEntryRepositoryTest` | 5 | 5 | 0 |
| `NutritionPlanRepositoryTest` | 5 | 5 | 0 |
| `RecipeRepositoryImplTest` | 8 | 8 | 0 |
| `ComputeRollingSummaryUseCaseEdgeCaseTest` | 11 | 11 | 0 |
| `ComputeRollingSummaryUseCaseTest` | 11 | 11 | 0 |
| `ScaleNutritionUseCaseEdgeCaseTest` | 9 | 9 | 0 |
| `ScaleNutritionUseCaseTest` | 7 | 7 | 0 |
| `ValidateFoodDataUseCaseEdgeCaseTest` | 9 | 9 | 0 |
| `ValidateFoodDataUseCaseTest` | 6 | 6 | 0 |
| `AddEntryViewModelIntegrationTest` | 12 | 12 | 0 |
| `DataRetentionIntegrationTest` | 8 | 8 | 0 |
| `FoodLookupIntegrationTest` | 16 | 16 | 0 |
| `NutritionCalculationIntegrationTest` | 12 | 12 | 0 |
| `RepositoryToViewModelIntegrationTest` | 7 | 7 | 0 |
| `BottomNavItemTest` | 3 | 3 | 0 |
| `RoutesTest` | 5 | 5 | 0 |
| `AddEntryViewModelTest` | 27 | 27 | 0 |
| `CreateRecipeViewModelTest` | 9 | 9 | 0 |
| `DailyProgressViewModelEdgeCaseTest` | 5 | 5 | 0 |
| `DailyProgressViewModelTest` | 4 | 4 | 0 |
| `RecipeDetailViewModelTest` | 4 | 4 | 0 |
| `RecipeListViewModelTest` | 3 | 3 | 0 |
| `SettingsViewModelTest` | 16 | 16 | 0 |
| `SummariesViewModelTest` | 9 | 9 | 0 |
| `FormatterEdgeCaseTest` | 18 | 18 | 0 |
| `FormatterTest` | 9 | 9 | 0 |
| `DataRetentionWorkerTest` | 8 | 8 | 0 |
| **Total** | **277** | **277** | **0** |

---

## Changes made

None. The domain layer was complete and correct as implemented. No code changes were
required in this session.

---

---

## Session 03 — Code Review Fix Pass (Domain Layer)

**Date**: 2026-03-22
**Review source**: `handoffs/code-review-02-domain layer.md`

### Changes made

#### C01 — KDoc clarification for `dailyPlans` parameter
**File**: `app/src/main/java/com/delve/hungrywalrus/domain/usecase/ComputeRollingSummaryUseCase.kt`

Added a sentence to the `dailyPlans` KDoc: "Keys need not cover every date in the period; an absent key is treated identically to a null value — that day has no plan." This removes the implicit contract ambiguity without changing behaviour.

#### W01 — Int-to-Double widening comment
**File**: `app/src/main/java/com/delve/hungrywalrus/domain/usecase/ComputeRollingSummaryUseCase.kt:56`

Added an inline comment at the `targetKcal += plan.kcalTarget` accumulation line noting the Int-to-Double widening is intentional and referencing the spec (section 5.2). No code change.

#### W02 — Negative override guards in `ValidateFoodDataUseCase.applyOverrides`
**File**: `app/src/main/java/com/delve/hungrywalrus/domain/usecase/ValidateFoodDataUseCase.kt`

Added `require(value >= 0.0)` guards for each of the four non-null override parameters (`kcalPer100g`, `proteinPer100g`, `carbsPer100g`, `fatPer100g`). A null override still passes through (leaving the field unchanged); only non-null negative values throw. Updated KDoc to document the `@throws` contract.

Added 4 new tests to `ValidateFoodDataUseCaseEdgeCaseTest` (one per field) verifying `IllegalArgumentException` is thrown on negative inputs.

#### W03 — Negative per-100g guards in `ScaleNutritionUseCase.invoke`
**File**: `app/src/main/java/com/delve/hungrywalrus/domain/usecase/ScaleNutritionUseCase.kt`

Added `require(value >= 0.0)` guards for all four per-100g parameters (`kcalPer100g`, `proteinPer100g`, `carbsPer100g`, `fatPer100g`) in `invoke`. Updated KDoc with the `@throws` contract. The existing `weightG` guard is unchanged.

Added 4 new tests to `ScaleNutritionUseCaseEdgeCaseTest` (one per field) verifying `IllegalArgumentException` is thrown on negative per-100g inputs.

#### O01 — Remove duplicate tests from `ScaleNutritionUseCaseEdgeCaseTest`
**File**: `app/src/test/java/com/delve/hungrywalrus/domain/usecase/ScaleNutritionUseCaseEdgeCaseTest.kt`

Removed 2 tests already covered by `ScaleNutritionUseCaseTest`:
- `negative weightG throws IllegalArgumentException` (duplicate of `invoke throws for negative weight`)
- `zero recipe totalWeightG throws IllegalArgumentException` (duplicate of `scaleRecipePortion throws for zero recipe weight`)

Net test count for the edge-case class: 9 → (9 - 2 + 4) = 11 (4 new per-100g guard tests added for W03).

#### O02 — Update test comment in `ComputeRollingSummaryUseCaseEdgeCaseTest`
**File**: `app/src/test/java/com/delve/hungrywalrus/domain/usecase/ComputeRollingSummaryUseCaseEdgeCaseTest.kt`

Rewrote the comment in `total intake sums all entries regardless of date field on entry` to make the caller contract explicit: "The use case does not filter entries by date — the ViewModel is responsible for passing only entries within the [start, end] window."

---

### Test results after changes

```
./gradlew testDebugUnitTest -- BUILD SUCCESSFUL
```

All 283 tests pass (up from 277; +6 net from new guard tests).

| Test class | Tests | Passed | Failed |
|---|---|---|---|
| `ComputeRollingSummaryUseCaseEdgeCaseTest` | 11 | 11 | 0 |
| `ComputeRollingSummaryUseCaseTest` | 11 | 11 | 0 |
| `ScaleNutritionUseCaseEdgeCaseTest` | 11 | 11 | 0 |
| `ScaleNutritionUseCaseTest` | 7 | 7 | 0 |
| `ValidateFoodDataUseCaseEdgeCaseTest` | 13 | 13 | 0 |
| `ValidateFoodDataUseCaseTest` | 6 | 6 | 0 |
| **Domain total** | **59** | **59** | **0** |
| **Full suite total** | **283** | **283** | **0** |

---

---

## Session 04 — Code Review Fix Pass 2 (Domain Layer)

**Date**: 2026-03-22
**Review source**: `handoffs/code-review-02-domain layer.md` (Review Pass 2)

### Changes made

#### O03 — Remove duplicate `negative portionWeightG` test from `ScaleNutritionUseCaseEdgeCaseTest`
**File**: `app/src/test/java/com/delve/hungrywalrus/domain/usecase/ScaleNutritionUseCaseEdgeCaseTest.kt`

Removed `negative portionWeightG throws IllegalArgumentException` (the test was a pre-existing edge-case test that duplicated `scaleRecipePortion throws for negative portionWeightG` in `ScaleNutritionUseCaseTest`). This duplicate was noted by the reviewer as introduced during the Session 03 fix pass.

`ScaleNutritionUseCaseEdgeCaseTest`: 11 → 10 tests.

#### O04 — Rename misleading test in `ValidateFoodDataUseCaseEdgeCaseTest`
**File**: `app/src/test/java/com/delve/hungrywalrus/domain/usecase/ValidateFoodDataUseCaseEdgeCaseTest.kt`

Renamed test from `applyOverrides does not overwrite an existing non-null kcal with a non-null override` to `applyOverrides replaces existing non-null kcal when a non-null override is provided`. The old name implied the original value was preserved; the test body actually asserts the override value replaces it. No code change; test count unchanged.

---

### Test results after changes

```
./gradlew testDebugUnitTest -- BUILD SUCCESSFUL
```

All 282 tests pass (down from 283 by the one removed duplicate; no new tests added).

| Test class | Tests | Passed | Failed |
|---|---|---|---|
| `ComputeRollingSummaryUseCaseEdgeCaseTest` | 11 | 11 | 0 |
| `ComputeRollingSummaryUseCaseTest` | 11 | 11 | 0 |
| `ScaleNutritionUseCaseEdgeCaseTest` | 10 | 10 | 0 |
| `ScaleNutritionUseCaseTest` | 7 | 7 | 0 |
| `ValidateFoodDataUseCaseEdgeCaseTest` | 13 | 13 | 0 |
| `ValidateFoodDataUseCaseTest` | 6 | 6 | 0 |
| **Domain total** | **58** | **58** | **0** |
| **Full suite total** | **282** | **282** | **0** |

---

## Known gaps / what next sessions should tackle

These items carry over from the Session 01 notes and the QA report. None are domain
layer issues.

1. **In-memory Room database tests** for DAO SQL correctness (`NutritionPlanDao.getCurrentPlan`
   ordering, `LogEntryDao.getEntriesForDate` day-boundary arithmetic). The `room-testing`
   dependency is present; requires Robolectric or instrumented tests to run.

2. **`ApiKeyStore` error-recovery unit tests**: the try/catch path that clears corrupted
   `EncryptedSharedPreferences` and returns `null` is not directly tested.

3. **UI layer session**: verify screen implementations against the Design Specification.
   All screens are implemented; this session should confirm UI behaviour and layout
   match the spec.

---

## Session 05 — Code Review Fix Pass 3 (Domain Layer)

**Date**: 2026-03-24
**Review source**: `handoffs/code-review-02-domain layer.md` (Review Pass 3)

### Changes made

#### O05 — Remove 4 duplicate tests from `ComputeRollingSummaryUseCaseEdgeCaseTest`
**File**: `app/src/test/java/com/delve/hungrywalrus/domain/usecase/ComputeRollingSummaryUseCaseEdgeCaseTest.kt`

Removed the following four tests already covered by `ComputeRollingSummaryUseCaseTest`:

1. `result startDate and endDate match inputs` — duplicate of `startDate and endDate are preserved in result` (base, line 127)
2. `periodDays for 7-day window is exactly 7` — duplicate of `periodDays is inclusive of start and end` (base, line 122)
3. `totalTarget is null when dailyPlans is empty map` — duplicate of `returns null target when dailyPlans has no non-null values` (base, line 76)
4. `throws when start is after end` — identical name and assertion in both files (base, line 144)

Also removed the now-empty section comments `// --- Data retention: entries must be present in the window ---`,
`// --- Boundary: totalTarget null when dailyPlans is empty map ---`, and `// --- start after end guard ---`.
Replaced with a single `// --- Period length ---` comment above the retained 28-day `periodDays` test.

`ComputeRollingSummaryUseCaseEdgeCaseTest`: 11 → 7 tests.

The edge-case class retains its distinct coverage: daily aggregation comment contract, empty-entries
daily average, 28-day window target accumulation, 28-day partial-plan null target, mid-period plan
change, 28-day period length, and very-large-value overflow test.

---

### Test results after changes

```
./gradlew testDebugUnitTest -- BUILD SUCCESSFUL
```

All 278 tests pass (down from 282 by the four removed duplicates; no new tests added).

| Test class | Tests | Passed | Failed |
|---|---|---|---|
| `ComputeRollingSummaryUseCaseEdgeCaseTest` | 7 | 7 | 0 |
| `ComputeRollingSummaryUseCaseTest` | 11 | 11 | 0 |
| `ScaleNutritionUseCaseEdgeCaseTest` | 10 | 10 | 0 |
| `ScaleNutritionUseCaseTest` | 7 | 7 | 0 |
| `ValidateFoodDataUseCaseEdgeCaseTest` | 13 | 13 | 0 |
| `ValidateFoodDataUseCaseTest` | 6 | 6 | 0 |
| **Domain total** | **54** | **54** | **0** |
| **Full suite total** | **278** | **278** | **0** |

---
