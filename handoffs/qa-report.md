# QA Report: Hungry Walrus

*Date: 2026-03-21*
*QA Agent: Claude Sonnet 4.6*

---

## Build Status

The project compiles successfully.

```
./gradlew assembleDebug
BUILD SUCCESSFUL in 13s
```

One non-fatal warning is emitted:

> WARNING: We recommend using a newer Android Gradle plugin to use compileSdk = 36. This Android Gradle plugin (8.7.3) was tested up to compileSdk = 35.

This is a configuration mismatch (`compileSdk = 36` set in `app/build.gradle.kts` while the AGP version has only been tested to 35) and does not prevent the build from succeeding. It is not a code defect.

---

## Existing Test Results

Running `./gradlew testDebugUnitTest` against the developer-authored tests produced:

| Test Class | Tests | Passed | Failed |
|---|---|---|---|
| `OffResponseMapperTest` | 6 | 6 | 0 |
| `UsdaResponseMapperTest` | 5 | 5 | 0 |
| `FoodLookupRepositoryImplTest` | 20 | 20 | 0 |
| `LogEntryRepositoryTest` | 5 | 5 | 0 |
| `NutritionPlanRepositoryTest` | 5 | 5 | 0 |
| `RecipeRepositoryImplTest` | 8 | 8 | 0 |
| `ComputeRollingSummaryUseCaseTest` | 11 | 11 | 0 |
| `ScaleNutritionUseCaseTest` | 7 | 7 | 0 |
| `ValidateFoodDataUseCaseTest` | 6 | 6 | 0 |
| `BottomNavItemTest` | 3 | 3 | 0 |
| `RoutesTest` | 5 | 5 | 0 |
| `AddEntryViewModelTest` | 24 | 24 | 0 |
| `CreateRecipeViewModelTest` | 9 | 9 | 0 |
| `DailyProgressViewModelTest` | 4 | 4 | 0 |
| `PlanViewModelTest` | 5 | 5 | 0 |
| `RecipeDetailViewModelTest` | 4 | 4 | 0 |
| `RecipeListViewModelTest` | 3 | 3 | 0 |
| `SettingsViewModelTest` | 5 | 5 | 0 |
| `SummariesViewModelTest` | 5 | 5 | 0 |
| `FormatterTest` | 9 | 9 | 0 |
| **Total** | **149** | **149** | **0** |

All 149 developer-authored tests pass with zero failures and zero errors.

---

## New Unit Tests

### `FormatterEdgeCaseTest`
**File:** `app/src/test/java/com/delve/hungrywalrus/util/FormatterEdgeCaseTest.kt`
**Tests added:** 18

| Test | What it tests |
|---|---|
| `formatKcal rounds 0_5 up (half-up rounding)` | Verifies half-up rounding at the kcal midpoint (0.5 → 1) |
| `formatKcal handles large value with comma separator` | 10,000 and 100,000 correctly formatted with commas |
| `formatKcal handles negative value without throwing` | Formatter does not crash on a negative kcal |
| `formatMacro rounds 0_25 up to 0_5 with half-up rounding` | Midpoint (0.25 * 2 = 0.5) rounds up with Math.round |
| `formatMacro rounds 0_24 down to 0_0` | Value below midpoint rounds down |
| `formatMacro rounds 0_26 up to 0_5` | Value above midpoint rounds up |
| `formatMacro handles large macro value` | 500.0 formatted correctly |
| `formatMacro preserves one decimal place for whole numbers` | "30.0" not "30" |
| `roundMacro rounds 0_25 up to 0_5 with half-up rounding` | Numeric rounding at midpoint |
| `roundMacro rounds 0_24 down to 0_0` | Below midpoint rounds down |
| `roundMacro rounds 0_75 up to 1_0` | Above midpoint rounds up |
| `roundMacro is idempotent on already rounded values` | Already-rounded values unchanged |
| `roundKcal rounds 0_5 up` | kcal midpoint rounds up |
| `roundKcal is unchanged for whole numbers` | 500 stays 500 |
| `formatDate epoch millis returns parseable date string` | Epoch overload matches dd/MM/yyyy regex |
| `formatDate LocalDate and epoch millis overloads produce same result` | Both overloads are consistent for same date |
| `formatTime returns HH_mm formatted string` | HH:mm regex check |
| `formatTime pads single digit hour and minute with leading zero` | "01:05" not "1:5" |

---

### `ScaleNutritionUseCaseEdgeCaseTest`
**File:** `app/src/test/java/com/delve/hungrywalrus/domain/usecase/ScaleNutritionUseCaseEdgeCaseTest.kt`
**Tests added:** 9

| Test | What it tests |
|---|---|
| `very large weight produces proportionally large values` | 10,000g input scales correctly without overflow |
| `zero per-100g reference values produce zero scaled values for any weight` | Zero references give zero output for any weight |
| `exactly 100g weight returns per-100g reference values unchanged` | Identity case: 100g scale factor |
| `negative weightG throws IllegalArgumentException` | Guard condition for invoke() |
| `portion weight equal to total recipe weight returns full recipe totals` | 100% portion returns full recipe values |
| `zero portion weight returns all zero nutrition` | Zero portion gives zero output |
| `portion larger than total recipe weight scales above 100 percent` | Over-full portion is permitted and scales correctly |
| `zero recipe totalWeightG throws IllegalArgumentException` | Guard condition for scaleRecipePortion() |
| `negative portionWeightG throws IllegalArgumentException` | Guard condition for scaleRecipePortion() |

---

### `ValidateFoodDataUseCaseEdgeCaseTest`
**File:** `app/src/test/java/com/delve/hungrywalrus/domain/usecase/ValidateFoodDataUseCaseEdgeCaseTest.kt`
**Tests added:** 9

| Test | What it tests |
|---|---|
| `isComplete returns false when all four fields are missing` | All-missing case |
| `isComplete returns true when all four fields are present` | All-present case |
| `applyOverrides accepts zero as a valid override for kcal` | Zero is a valid (not missing) value |
| `applyOverrides accepts zero for all macros simultaneously` | Zero accepted for all fields at once |
| `applyOverrides does not overwrite an existing non-null kcal with a non-null override` | Override replaces existing value when supplied |
| `applyOverrides preserves existing non-null kcal when override is null` | Null override leaves existing value intact |
| `missingFields remains non-empty when not all missing fields are filled` | Partial override leaves remaining fields missing |
| `applyOverrides called multiple times is idempotent when values unchanged` | Repeated calls with null overrides are stable |
| `entry cannot be considered complete until all missing fields are overridden` | Full lifecycle: step-by-step override until complete |

---

### `ComputeRollingSummaryUseCaseEdgeCaseTest`
**File:** `app/src/test/java/com/delve/hungrywalrus/domain/usecase/ComputeRollingSummaryUseCaseEdgeCaseTest.kt`
**Tests added:** 11

| Test | What it tests |
|---|---|
| `total intake sums all entries regardless of date field on entry` | All entries in the list are summed regardless of timestamp |
| `dailyAverage is zero for all nutrients when entries list is empty` | Zero-intake average |
| `28-day window accumulates 28 days of plan targets` | Full 28-day target accumulation |
| `totalTarget is null for 28-day window when any day is missing a plan` | Partial plan coverage for 28-day period |
| `mid-period plan change accumulates different targets for each day` | 4 days at 1500 kcal + 3 days at 2500 kcal = 13,500 |
| `result startDate and endDate match inputs` | Output fields match the provided dates |
| `periodDays for 7-day window is exactly 7` | Inclusive day count for 7-day window |
| `periodDays for 28-day window is exactly 28` | Inclusive day count for 28-day window |
| `totalTarget is null when dailyPlans is empty map` | Empty map yields null target |
| `throws when start is after end` | Guard condition |
| `very large values are accumulated without overflow` | 100,000 kcal per entry accumulates correctly |

---

### `DataRetentionWorkerTest`
**File:** `app/src/test/java/com/delve/hungrywalrus/worker/DataRetentionWorkerTest.kt`
**Tests added:** 8

| Test | What it tests |
|---|---|
| `doWork returns success when both DAOs succeed` | Happy path returns Result.success() |
| `doWork passes log retention threshold of approximately 730 days ago` | Threshold is bracketed by before/after call times |
| `doWork passes cache retention threshold of approximately 30 days ago` | Cache threshold correctly set |
| `doWork calls logEntryDao deleteOlderThan exactly once` | No double-delete |
| `doWork calls foodCacheDao deleteOlderThan exactly once` | No double-delete |
| `doWork returns retry when logEntryDao throws` | Error handling returns Result.retry() |
| `doWork returns retry when foodCacheDao throws` | Error handling returns Result.retry() |
| `log retention threshold is greater than cache retention threshold` | 730-day threshold is earlier (smaller millis) than 30-day threshold |

---

### `PlanViewModelEdgeCaseTest`
**File:** `app/src/test/java/com/delve/hungrywalrus/ui/screen/plan/PlanViewModelEdgeCaseTest.kt`
**Tests added:** 10

| Test | What it tests |
|---|---|
| `savePlan rejects zero kcal` | kcal must be positive per validation |
| `savePlan rejects negative kcal` | Negative kcal rejected |
| `savePlan rejects empty string for all fields` | All four fields produce errors |
| `savePlan rejects non-numeric input for macros` | Non-parseable strings produce errors |
| `savePlan accepts zero for protein, carbs, and fat` | Zero is valid for macros |
| `savePlan calls repository with correct parsed values` | Repository receives correctly parsed doubles |
| `savePlan emits PlanSaved event after successful save` | Event channel delivers event |
| `initial state is Loading before getCurrentPlan emits` | Initial state correct |
| `Content state reflects current plan from repository` | Repository plan threaded through |
| `Content state plan is null when no plan configured` | Null plan handled |

---

### `DailyProgressViewModelEdgeCaseTest`
**File:** `app/src/test/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressViewModelEdgeCaseTest.kt`
**Tests added:** 5

| Test | What it tests |
|---|---|
| `entries are sorted descending by timestamp` | Sort order is newest-first per spec |
| `totals are zero when no entries logged` | Zero-entry day shows zero totals |
| `totals aggregate all four macronutrients correctly across multiple entries` | Multi-entry aggregation |
| `single entry totals match that entry exactly` | Single-entry aggregation |
| `state is Content not Error when plan is null` | No-plan state is Content, not Error |

---

## Integration Tests

### `NutritionCalculationIntegrationTest`
**File:** `app/src/test/java/com/delve/hungrywalrus/integration/NutritionCalculationIntegrationTest.kt`
**Tests added:** 12
**Layers verified:** Domain use cases working together; nutritional calculation correctness end-to-end.

| Test | Interaction verified |
|---|---|
| `scale 200g chicken breast from 100g reference gives correct values` | ScaleNutritionUseCase: per-100g → weight |
| `scale 50g oats from 100g reference gives half the reference values` | ScaleNutritionUseCase: fractional weight |
| `scale 0g of any food returns all zeros` | ScaleNutritionUseCase: zero weight edge case |
| `50 percent of recipe gives half of recipe totals` | ScaleNutritionUseCase: recipe portion |
| `25 percent of recipe gives quarter of recipe totals` | ScaleNutritionUseCase: recipe quarter portion |
| `food with missing kcal is not complete until override supplied` | ValidateFoodDataUseCase → isComplete → applyOverrides |
| `food with all four missing fields requires all four overrides` | Full missing-field override lifecycle |
| `daily totals computed by summing individual scaled entries` | Aggregation arithmetic |
| `7-day summary correctly accumulates intake over 7 days` | ScaleNutritionUseCase + ComputeRollingSummaryUseCase |
| `28-day summary correctly accumulates intake and targets` | 28-day window with full plan coverage |
| `totalTarget is null in 7-day summary when plan missing for one day` | Partial plan coverage nullifies target |
| `scaling two ingredients and summing matches manually computed totals` | Multi-ingredient recipe total verification |

---

### `RepositoryToViewModelIntegrationTest`
**File:** `app/src/test/java/com/delve/hungrywalrus/integration/RepositoryToViewModelIntegrationTest.kt`
**Tests added:** 7
**Layers verified:** Repository layer → ViewModel layer data flow, using mocked repositories and real use case implementations.

| Test | Interaction verified |
|---|---|
| `DailyProgressViewModel combines plan and entries from repository into Content state` | NutritionPlanRepository + LogEntryRepository → DailyProgressViewModel |
| `DailyProgressViewModel shows null plan and zero totals when no plan configured` | No-plan path through repository → ViewModel |
| `DailyProgressViewModel entries are sorted descending by timestamp` | Repository list → ViewModel sort order |
| `SummariesViewModel produces NoPlan state when no plan configured` | NutritionPlanRepository (null) + ComputeRollingSummaryUseCase → SummariesViewModel |
| `SummariesViewModel produces Content state with correct totalTarget when plan is fully configured` | Full-plan path through use case to ViewModel |
| `SummariesViewModel calculates correct daily average for 7-day period` | ComputeRollingSummaryUseCase dailyAverage via ViewModel |
| `SummariesViewModel switches to 28-day period when tab changed` | Tab selection → re-load with correct period |

---

### `FoodLookupIntegrationTest`
**File:** `app/src/test/java/com/delve/hungrywalrus/integration/FoodLookupIntegrationTest.kt`
**Tests added:** 16
**Layers verified:** API client (mocked Retrofit services) → FoodLookupRepositoryImpl → domain models, plus ValidateFoodDataUseCase.

| Test | Interaction verified |
|---|---|
| `searchUsda returns mapped FoodSearchResult with correct nutrient values` | UsdaApiService → UsdaResponseMapper → FoodSearchResult |
| `searchUsda result with missing kcal has KCAL in missingFields and is not complete` | Missing nutrient → missingFields → ValidateFoodDataUseCase.isComplete = false |
| `searchUsda returns OfflineException on IOException` | IOException → OfflineException wrapping |
| `searchUsda returns failure with Invalid API key message on HTTP 403` | HTTP 403 → error message mapping |
| `searchUsda returns failure with Too many requests message on HTTP 429` | HTTP 429 → error message mapping |
| `searchUsda returns failure on unexpected exception` | Catch-all exception path |
| `searchOpenFoodFacts returns mapped results` | OffApiService → OffResponseMapper → FoodSearchResult |
| `searchOpenFoodFacts result with all null nutriments has all four missing fields` | Null nutriments → all 4 fields missing |
| `searchOpenFoodFacts returns OfflineException on IOException` | IOException → OfflineException |
| `lookupBarcode uses cached result when cache is fresh` | FoodCacheDao fresh hit → no network call |
| `lookupBarcode returns null when product not found (status 0)` | status=0 → Result.success(null) |
| `lookupBarcode returns stale cache on IOException (offline)` | Expired cache + IOException → stale cache returned |
| `lookupBarcode returns failure on HttpException 500 even when stale cache exists` | HTTP 5xx does NOT fall back to stale cache |
| `lookupBarcode returns success null on HTTP 404` | HTTP 404 → Result.success(null) |
| `lookupBarcode result with missing nutriments has all four fields in missingFields` | Null nutriments in barcode response → 4 missing fields |
| `complete barcode lookup result satisfies validateUseCase isComplete` | Complete API response → validateUseCase.isComplete = true |

---

### `DataRetentionIntegrationTest`
**File:** `app/src/test/java/com/delve/hungrywalrus/integration/DataRetentionIntegrationTest.kt`
**Tests added:** 8
**Layers verified:** DataRetentionWorker → LogEntryDao and FoodCacheDao; retention thresholds per spec.

| Test | Interaction verified |
|---|---|
| `log retention threshold is exactly 730 days in the past` | Worker passes (now - 730 days) to logEntryDao |
| `worker does not call any recipe deletion method` | No recipe DAO is invoked (recipes retained indefinitely) |
| `a log entry 731 days old would have timestamp older than deletion threshold` | 731-day-old entry would be deleted |
| `a log entry 729 days old would not be deleted` | 729-day-old entry is within retention window |
| `food cache retention threshold is exactly 30 days in the past` | Worker passes (now - 30 days) to foodCacheDao |
| `doWork returns success when both DAOs succeed` | Happy path |
| `doWork returns retry when logEntryDao throws` | Error recovery |
| `doWork returns retry when foodCacheDao throws` | Error recovery |

---

### `AddEntryViewModelIntegrationTest`
**File:** `app/src/test/java/com/delve/hungrywalrus/integration/AddEntryViewModelIntegrationTest.kt`
**Tests added:** 12
**Layers verified:** AddEntryViewModel using real ScaleNutritionUseCase and ValidateFoodDataUseCase; LogEntryRepository (mocked) receiving correctly scaled values.

| Test | Interaction verified |
|---|---|
| `setWeight with 200g produces correctly scaled nutrition for API food` | ScaleNutritionUseCase wired through ViewModel for food |
| `setWeight with 100g returns values equal to per-100g reference` | Identity case through ViewModel |
| `setWeight with zero does not compute scaled nutrition` | Zero weight rejected at ViewModel level |
| `setWeight with negative value does not produce scaled nutrition` | Negative weight rejected (weight <= 0 guard) |
| `setWeight with non-numeric string clears scaled nutrition` | Non-parseable input clears scaled nutrition |
| `setWeight for recipe computes proportional nutrition based on recipe totals` | scaleRecipePortion wired through ViewModel |
| `food with missing kcal cannot produce scaled nutrition until override applied` | Missing field lifecycle: selectFood → applyMissingValues → setWeight |
| `saveEntry creates LogEntry with correctly scaled values from food` | Scaled nutrition correctly persisted in LogEntry |
| `saveEntry creates LogEntry with recipe-scaled values` | Recipe portion scaling correctly persisted |
| `searchOff with results updates state to Results` | FoodLookupRepository → AddEntryViewModel search state |
| `searchOff with no results sets NoResults state` | Empty search results path |
| `setWeight with very large value does not crash and produces large scaled result` | 100,000g input handled without exception |

---

## Bugs Found

### Bug 1 — `Formatter.formatMacro` and `roundMacro` use half-up rounding for the midpoint (0.25 rounds to 0.5)

**Description:** The `Formatter.formatMacro` and `Formatter.roundMacro` methods use `Math.round(value * 2.0) / 2.0`. `Math.round` applies "round half up" semantics: 0.25 * 2 = 0.5, `Math.round(0.5) = 1`, result = 0.5. This means the value 0.25g rounds UP to 0.5g. This is technically correct "nearest 0.5" with half-up tie-breaking, and is consistent with the behaviour verified in the developer's existing `FormatterTest` (e.g. `12.75 → 13.0`). The implementation is internally consistent.

**Severity:** Not a bug — the rounding behaviour is correct and consistent with the spec ("rounded to the nearest 0.5g"). The QA test file was corrected to accurately assert the half-up rounding.

---

### Bug 2 — `compileSdk = 36` with AGP 8.7.3 (tested only to compileSdk 35)

**Description:** `app/build.gradle.kts` sets `compileSdk = 36` but the Android Gradle Plugin 8.7.3 was only tested to `compileSdk = 35`. This produces a warning at build time.

**Steps to reproduce:** Run `./gradlew assembleDebug` and observe the warning.

**Expected behaviour per spec:** Architecture document specifies `compileSdk = 35`. The `app/build.gradle.kts` has `compileSdk = 36` which diverges from the architecture document.

**Actual behaviour:** Build succeeds with warning; `compileSdk = 36` is used.

**Severity:** Minor — the build succeeds, but this is a deviation from the architecture document's specified `compileSdk = 35` and may expose untested build-tool interactions.

---

### Bug 3 — `PlanViewModel.savePlan` accepts zero for macronutrient targets but rejects zero for kcal

**Description:** `PlanViewModel.savePlan` validates kcal with `kcal <= 0` (rejecting zero), but validates protein/carbs/fat with `< 0` (accepting zero). This asymmetry means a user can save a plan with 0g protein, 0g carbs, and 0g fat but cannot save with 0 kcal. The requirements do not explicitly state macronutrient targets must be non-zero, so accepting zero for macros may be intentional. However, this is not documented and the asymmetry could be confusing.

**Steps to reproduce through test:** `PlanViewModelEdgeCaseTest.savePlan accepts zero for protein, carbs, and fat` passes, confirming the behaviour.

**Expected behaviour per spec:** Requirements state the user enters "daily targets: total kilocalories, protein (g), carbohydrates (g), and fat (g)". No explicit floor is stated for macros. A plan with 0g fat is arguably valid. However, 0 kcal is arguably valid too (e.g. a fasting day target), yet the code rejects it. The inconsistency is the concern.

**Actual behaviour:** `savePlan("0", "150", "250", "65")` produces a validation error for kcal but `savePlan("2000", "0", "0", "0")` succeeds.

**Severity:** Minor — the app will not crash; users simply cannot set a 0-kcal plan.

---

### Bug 4 — `DataRetentionWorker` is not directly tested for recipe non-deletion (no `RecipeDao` in worker)

**Description:** The requirements state "Recipes are retained indefinitely." The `DataRetentionWorker` correctly has no `RecipeDao` injection and makes no calls to delete recipes. This is the correct implementation. However, there was no pre-existing test verifying this property, and a future developer could accidentally inject a `RecipeDao` and add recipe deletion. The new `DataRetentionIntegrationTest.worker does not call any recipe deletion method` test now documents and verifies this contract.

**Severity:** Not a current bug — just a previously untested contract. Now covered.

---

### Bug 5 — `SummariesViewModel` summary is not reactive to new log entries while the screen is open

**Description:** `SummariesViewModel.loadSummary` uses `logRepo.getEntriesForRange(start, end).first()` to collect a single snapshot of log entries. If the user adds a log entry while the summaries screen is visible, the displayed totals will not update until the user navigates away and back.

**Steps to reproduce through test:** This is noted in the developer session notes ("Integration notes: SummariesViewModel uses `.first()` to collect entries — not reactive to changes while the screen is open") and is confirmed as a known design limitation.

**Expected behaviour per spec:** The spec does not explicitly require reactive updates on the summary screen. The developer notes describe this as "acceptable for v1".

**Actual behaviour:** Summary data is a snapshot taken at load time. New entries logged while the screen is open do not appear until the screen is reloaded.

**Severity:** Minor — documented design limitation. Not a regression risk.

---

### Bug 6 — `setWeight` in `AddEntryViewModel` does not call `ScaleNutritionUseCase` when food has null nutrition values; this is the correct behaviour but it is silently unrecoverable without applying overrides first

**Description:** When a user selects a food with missing nutrition values and enters a weight, `setWeight` returns `null` for `scaledNutrition` because the null-check on `food.kcalPer100g` (and other fields) is performed inline before calling the use case. The user must apply overrides via `applyMissingValues` before a weight can produce scaled output.

**Steps to reproduce through test:** `AddEntryViewModelIntegrationTest.food with missing kcal cannot produce scaled nutrition until override applied` demonstrates this exactly.

**Expected behaviour per spec:** Requirements state "If any of the four core nutritional values are missing from an API response, prompt the user to provide an estimate for the missing values before the entry can be saved." The flow — select food → detect missing fields → prompt for overrides → apply → then enter weight — is the correct intended UX.

**Actual behaviour:** Consistent with the requirement. The `selectFood` method returns `true` when missing values are present, which signals the caller to navigate to the missing-values screen. This is working correctly and is not a bug.

**Severity:** Not a bug — verified correct behaviour.

---

## Coverage Assessment

### Well-Tested Areas

**Data layer — remote mappers:**
`UsdaResponseMapper` and `OffResponseMapper` have complete coverage of the mapping logic including all combinations of missing and present nutrient fields.

**Data layer — repositories:**
`FoodLookupRepositoryImpl` has extensive coverage (20 tests) covering all code paths: fresh cache, expired cache, IOException, HttpException (404, 5xx, 403, 429), unexpected exception, search results, cache population. `RecipeRepositoryImpl` covers save, update (with transaction ordering), delete, and list retrieval. `NutritionPlanRepository` and `LogEntryRepository` cover the key day-boundary epoch conversion logic.

**Domain layer — use cases:**
`ScaleNutritionUseCase` and `ComputeRollingSummaryUseCase` have thorough boundary-condition coverage including negative inputs, zero inputs, single-day periods, mid-period plan changes, and partial plan coverage. `ValidateFoodDataUseCase` covers the complete override lifecycle.

**ViewModel layer:**
All 8 ViewModels have unit tests. `AddEntryViewModel` has the most comprehensive test coverage (24 tests), including barcode lookup, recipe selection, missing value overrides, and the ingredient mode path.

**Data retention:**
`DataRetentionWorker` now has unit and integration tests verifying the 730-day log retention rule, 30-day cache eviction, and the requirement that recipes are never deleted by the worker.

**Nutritional calculations:**
Integration tests verify per-100g scaling, recipe portion scaling, daily progress aggregation, and rolling 7-day / 28-day summaries against manually computed expected values.

---

### Areas Remaining Undertested

**`ApiKeyStore`:**
No dedicated unit tests exist for `ApiKeyStore`. It is implicitly covered through `SettingsViewModel` and `AddEntryViewModel` tests that mock it. The error-handling path (EncryptedSharedPreferences decryption failure triggering a clear-and-return-null) is not tested. Adding direct unit tests with a mock `SharedPreferences` that throws on `getString` would improve confidence in the error recovery path.

**`RecipeDetailViewModel` and `RecipeListViewModel`:**
Only 4 tests each. `RecipeDetailViewModel` tests cover loading, not-found, delete, and ingredient loading, but do not verify all mapped fields from the repository. `RecipeListViewModel` has no test for the `showDeleteConfirmFor` state management.

**`CreateRecipeViewModel` — live totals recomputation:**
While ingredient add/remove and totals are tested, the formula used in `recomputeTotals()` (which calls `ScaleNutritionUseCase`) is not verified against the spec formula `(valuePer100g / 100.0) * weightG` with specific numerical assertions across multiple ingredients with different weights.

**`PlanViewModel` — reactive update after external plan change:**
The behaviour when `getCurrentPlan` emits a new plan while the ViewModel is in `Saved` state (it should not overwrite Saved with Content) is tested only by the developer's test, not by a new test. The comment-documented behaviour (`Only update to Content if we haven't already saved`) could use an explicit regression test.

**DAO layer (Room queries):**
All DAO tests use mocked DAOs. The SQL query logic (e.g. the `effectiveFrom <= :now ORDER BY effectiveFrom DESC LIMIT 1` query in `NutritionPlanDao`) is not tested with an in-memory Room database. The `room-testing` dependency is present in `build.gradle.kts` but no in-memory database integration tests exist. Testing the actual SQL — particularly the day-boundary queries in `LogEntryDao` and the plan-for-date query in `NutritionPlanDao` — with an in-memory database would catch SQL correctness issues that mocked DAOs cannot.

**Worker scheduling:**
`DataRetentionWorker` is tested for its `doWork()` logic, but the `HungryWalrusApp.onCreate` scheduling (24-hour period, 1-hour initial delay) is not tested.

**Navigation:**
`RoutesTest` verifies route string construction for three helper functions. The complete navigation graph (all routes, back-stack behaviour, shared ViewModel scoping) is not covered by any automated test. This is expected given the Android-specific nature of navigation testing, but it represents a gap.

**`BarcodeScanScreen`:**
As noted by the developer, the barcode scanning screen requires a real device for meaningful testing. No automated tests cover the CameraX, ML Kit, or permission flow paths.

---

### Recommendations

1. **Add in-memory Room tests for DAO SQL correctness.** Priority: day-boundary queries in `LogEntryDao.getEntriesForDate` and the `ORDER BY effectiveFrom DESC LIMIT 1` logic in `NutritionPlanDao.getCurrentPlan`. The `room-testing` dependency is already present; only the test infrastructure is missing.

2. **Add unit tests for `ApiKeyStore` error-recovery path.** A mock `SharedPreferences` that throws on `getString` would verify the try/catch block that clears corrupted preferences and returns `null`.

3. **Add regression test for `PlanViewModel` not reverting from `Saved` to `Content` on plan change.** This is a documented but untested behaviour.

4. **Add `RecipeListViewModel` delete confirmation state tests.** The `showDeleteConfirmFor` state field (used to show the confirmation dialog) has no test coverage.

5. **Verify `CreateRecipeViewModel` live totals formula with multi-ingredient numerical assertions.** Currently only single-ingredient totals are verified with specific numbers; multi-ingredient cases use only structural assertions (ingredient count, weight sum).
