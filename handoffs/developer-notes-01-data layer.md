# Developer Notes: Session 01 -- Data Layer Review

## What this session did

Reviewed the codebase against Architecture Revision 1 (2026-03-22) and QA Report
(2026-03-21). This session consisted of three passes:

**Pass 1**: The data layer was already substantially complete from a prior commit
(`c68b2fd data layer update`). This pass identified and closed one remaining gap
from the architecture revision (SummariesViewModel reload on visit).

**Pass 2 (re-verification)**: Re-ran the full review against the current codebase
to confirm all architecture changes are reflected. No additional code changes were
needed. All 277 tests pass.

**Pass 3**: Fixed the deferred `compileSdk = 35` deviation now that Android SDK
platform 35 has been installed in the build environment.

---

## Architecture Revision 1 -- Status of each change

### Requirements changes

| Item | Status | Notes |
|---|---|---|
| P04 Manual entry no longer requires weight | Done | `ManualEntryScreen` captures final values (kcal, protein, carbs, fat) directly. No weight field. `setDirectEntry()` in `AddEntryViewModel` stores them without scaling. |
| P03 Plan managed from Settings, not dedicated screen | Done | No `PlanViewModel` or `plan` route exists. Plan view/edit is in `SettingsViewModel` / `SettingsScreen`. |
| P05 Daily progress shows running totals | Done | `DailyProgressViewModel` uses `combine()` over two reactive Flows and derives `totalKcal`, `totalProteinG`, `totalCarbsG`, `totalFatG` in the emitted state. |
| P06 Summaries show plan targets alongside intake | Done | `SummariesUiState.Content` carries `summary.totalTarget`; `SummariesScreen` renders both `totalIntake` and `totalTarget` via `NutritionProgressBar`. |
| P07 / Bug 5 Summaries refresh on each visit | Fixed Pass 1 | See change detail below. |
| P01 No-plan notice updates reactively | Done | `DailyProgressViewModel` uses `combine(planRepo.getCurrentPlan(), logRepo.getEntriesForDate(...))` -- the plan Flow is continuously observed, so any plan change is reflected immediately. |
| Bug 3 Plan validation rules clarified | Done | `SettingsViewModel.savePlan` rejects `kcal <= 0` (must be positive) and rejects macros `< 0` (zero accepted). Matches the clarified spec. |

### Architecture changes

| Item | Status | Notes |
|---|---|---|
| Bug 2 compileSdk = 35 | Fixed Pass 3 | SDK platform 35 is now installed. `app/build.gradle.kts` updated from `compileSdk = 36` to `compileSdk = 35`. Build is clean with no AGP warnings. |
| m1 IOException vs HttpException cache fallback | Done | `FoodLookupRepositoryImpl.lookupBarcode` falls back to stale cache only on `IOException`; `HttpException` paths never serve stale data. |
| m2 DAO providers @Singleton | Done | All five DAO provider methods in `DatabaseModule` carry `@Singleton`. |
| P02 Recipe ingredient flow supports OFF sources | Done | `AddEntryViewModel.selectFood(FoodSearchResult)` accepts any `FoodSearchResult` regardless of `FoodSource`. The `FoodSearchScreen` is reached from `CreateRecipeScreen` via `onNavigateToIngredientSearch(source)` for both "usda" and "off" sources. |
| Bug 4 Worker recipe non-deletion documented | Correct in code; documented in architecture | `DataRetentionWorker` has no `RecipeDao` injection. |
| In-memory Room database testing | Deferred | QA recommendation. Room's `inMemoryDatabaseBuilder` requires an Android `Context`, unavailable in JVM unit tests without Robolectric. Prerequisite: add Robolectric dependency or move to `src/androidTest/`. |

---

## Changes made

### Pass 1 -- `SummariesViewModel`: expose `reloadSummary()` (Architecture Section 7.5)

**Problem**: `loadSummary()` was private and only called from `init` and `selectTab`.
No mechanism for the screen to trigger a reload on re-entry. The per-day plan data
in `buildDailyPlans` is fetched once per `loadSummary` call (not a reactive Flow),
so returning from Settings after a plan change would not update the summary targets.

**Fix**:
- Added `private var currentTab = SummaryTab.Day7` to track the active tab.
- `selectTab(tab)` sets `currentTab = tab` before calling `loadSummary(tab)`.
- Added public `fun reloadSummary()` that calls `loadSummary(currentTab)`.
- `SummariesScreen` now has `LaunchedEffect(Unit) { viewModel.reloadSummary() }` just
  before state collection, so each re-entry to the Summaries destination triggers a
  fresh plan data fetch.

**Files changed**:
- `app/src/main/java/com/delve/hungrywalrus/ui/screen/summaries/SummariesViewModel.kt`
- `app/src/main/java/com/delve/hungrywalrus/ui/screen/summaries/SummariesScreen.kt`

### Pass 3 -- `compileSdk = 35` (Architecture Section 2 / Bug 2)

**Problem**: `app/build.gradle.kts` had `compileSdk = 36`, diverging from the
architecture's required `compileSdk = 35` and producing an AGP 8.7.3 compatibility
warning on every build.

**Fix**: Changed `compileSdk = 36` to `compileSdk = 35` in `app/build.gradle.kts`.
SDK platform 35 (`~/Android/Sdk/platforms/android-35`) is now installed alongside
36 and 36.1. Build is clean with no warnings.

**File changed**:
- `app/build.gradle.kts`

---

## Tests

### Tests written in Pass 1

Added 2 tests to `SummariesViewModelTest`:

| Test | What it verifies |
|---|---|
| `reloadSummary re-fetches plan data and emits fresh state` | Calling `reloadSummary()` invokes `planRepo.getPlanForDate()` again and emits `Content` state when a plan exists. |
| `reloadSummary after selectTab reloads the 28-day tab` | After `selectTab(Day28)`, `reloadSummary()` produces state with `selectedTab = Day28` and `periodDays = 28`. |

### Full test suite (Pass 3 verification)

```
./gradlew assembleDebug testDebugUnitTest -- BUILD SUCCESSFUL
```

No AGP warnings. All 277 tests pass.

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

## Known gaps / what next sessions should tackle

1. **In-memory Room database tests**: The `room-testing` dependency is present but
   Room's `inMemoryDatabaseBuilder` requires an Android `Context`. This cannot be
   exercised in the JVM unit test suite (`src/test/`) without Robolectric. To add
   these tests, either add the Robolectric dependency or move them to instrumented
   tests (`src/androidTest/`). Priority queries to cover:
   - `NutritionPlanDao.getCurrentPlan` -- `ORDER BY effectiveFrom DESC LIMIT 1` correctness.
   - `LogEntryDao.getEntriesForDate` -- day-boundary epoch arithmetic.
2. **`ApiKeyStore` error-recovery unit tests**: The try/catch path that clears corrupted
   EncryptedSharedPreferences and returns null is not directly tested.
3. **UI screens**: Most screens are implemented. UI layer session should follow to verify
   screen implementations against the design specification.

---

## Session 02 -- Code Review Fix Pass (2026-03-22)

### Review findings addressed

All four findings from `handoffs/code-review-01-data layer.md` were actioned.

#### W01 — UTC timezone hardcoded for day-boundary arithmetic (Fixed)

**Change**: Replaced `ZoneOffset.UTC` with `ZoneId.systemDefault()` in both repositories.

- `LogEntryRepositoryImpl.getEntriesForDate` and `getEntriesForRange` — four call sites updated.
- `NutritionPlanRepositoryImpl.getPlanForDate` — one call site updated.
- Import changed from `java.time.ZoneOffset` to `java.time.ZoneId` in both files.

**Tests updated**: `LogEntryRepositoryTest` and `NutritionPlanRepositoryTest` previously computed
expected epoch-milli values using `ZoneOffset.UTC`. Updated to `ZoneId.systemDefault()` so the
mock stubs match the repository's new computation in all timezone environments.

**Files changed**:
- `app/src/main/java/com/delve/hungrywalrus/data/repository/LogEntryRepositoryImpl.kt`
- `app/src/main/java/com/delve/hungrywalrus/data/repository/NutritionPlanRepositoryImpl.kt`
- `app/src/test/java/com/delve/hungrywalrus/data/repository/LogEntryRepositoryTest.kt`
- `app/src/test/java/com/delve/hungrywalrus/data/repository/NutritionPlanRepositoryTest.kt`

#### W02 — `EncryptedSharedPreferences` recovery path can crash (Fixed)

**Change**: Wrapped the second `createEncryptedPrefs(context)` call in its own `try/catch`.
On a second failure the provider falls back to a plain (unencrypted) `SharedPreferences`
instance under a separate file name (`encrypted_prefs_plain`), logs a warning via
`android.util.Log.w`, and returns normally. The app remains usable; the API key will not
survive process restarts in this degraded state, but there is no startup crash.

No unit test added — the recovery path requires an Android `Context` and a controllably
broken Keystore, which is not exercisable in the JVM test suite without Robolectric or
instrumented tests. This remains a known gap (see item 2 above).

**Files changed**:
- `app/src/main/java/com/delve/hungrywalrus/di/NetworkModule.kt`

#### O01 — `FoodCacheDao.get(cacheKey)` is dead code (Fixed)

**Change**: Removed the `get(cacheKey: String): FoodCacheEntity?` method from `FoodCacheDao`.
The per-selection food caching path is not yet wired up; when it is implemented end-to-end
the method can be re-added alongside the `FoodLookupRepository.cacheFood()` counterpart.

**Files changed**:
- `app/src/main/java/com/delve/hungrywalrus/data/local/dao/FoodCacheDao.kt`

#### O02 — `RecipeIngredientDao.getByRecipeId` is dead code (Fixed)

**Change**: Removed `getByRecipeId(recipeId: Long): Flow<List<RecipeIngredientEntity>>` from
`RecipeIngredientDao`. Ingredients are already loaded atomically via the `@Relation` on
`RecipeWithIngredients`; the standalone Flow method was redundant. The unused
`kotlinx.coroutines.flow.Flow` import was removed with it.

**Files changed**:
- `app/src/main/java/com/delve/hungrywalrus/data/local/dao/RecipeIngredientDao.kt`

### Tests

No new tests were written. Two existing test classes were updated to keep expected values
consistent with the W01 timezone fix. The full suite was re-run after all changes.

```
./gradlew testDebugUnitTest -- BUILD SUCCESSFUL
```

All 277 tests pass, 0 failures.

---

## Session 04 -- Pass 2 findings fix (2026-03-22)

Addressed the two remaining open findings from Pass 2 of the code review
(`handoffs/code-review-01-data layer.md`).

### O03 — `NutritionPlanRepository.getCurrentPlan()` KDoc (Fixed)

**Change**: Replaced the incorrect "Snapshot limitation" KDoc on `getCurrentPlan()`.
The original comment described a midnight-staleness problem that does not exist: the
implementation passes `Long.MAX_VALUE` as the timestamp predicate, so there is no
snapshot of "current time" and no possibility of a plan being missed at midnight.

The new comment accurately describes the `Long.MAX_VALUE` strategy and clarifies that:
- plans are always saved with `effectiveFrom = System.currentTimeMillis()`, so no
  future-dated plan can slip through the predicate
- Room re-runs the query on every table change, so the Flow always reflects the latest
  active plan without re-collection

No tests were needed for a documentation-only change.

**Files changed**:
- `app/src/main/java/com/delve/hungrywalrus/data/repository/NutritionPlanRepository.kt`

### O04 — `OffApiService.getProductByBarcode` fetches full product payload (Fixed)

**Change**: Added `@Query("fields") fields: String = "code,product_name,nutriments"` to
`getProductByBarcode`. This matches the projection already applied to `searchProducts` and
limits the OFF API response to only the three fields the app needs, reducing response size.

The default value means no call site changes were required: `FoodLookupRepositoryImpl`
continues to call `getProductByBarcode(barcode)` without specifying `fields`, and the
Retrofit interface sends `?fields=code,product_name,nutriments` automatically. All existing
MockK-based tests set up expectations on `getProductByBarcode(barcode)` — the Kotlin
default parameter value is applied at the call site in both the production code and the
mock setup, so all expectations remain correct without modification.

No new tests were needed. The behaviour change is a network-level optimisation (smaller
response body); the application logic is unchanged.

**Files changed**:
- `app/src/main/java/com/delve/hungrywalrus/data/remote/openfoodfacts/OffApiService.kt`

### Tests

```
./gradlew testDebugUnitTest -- BUILD SUCCESSFUL
```

All 277 tests pass, 0 failures.

### Review status after this session

All six findings across all three passes of the code review are now resolved:

| Finding | Description | Status |
|---|---|---|
| W01 | UTC timezone in day-boundary arithmetic | Resolved (Session 02) |
| W02 | EncryptedSharedPreferences recovery crash | Resolved (Session 02) |
| O01 | `FoodCacheDao.get(cacheKey)` dead code | Resolved (Session 02) |
| O02 | `RecipeIngredientDao.getByRecipeId` dead code | Resolved (Session 02) |
| O03 | `NutritionPlanRepository` KDoc contradicts implementation | Resolved (Session 04) |
| O04 | `getProductByBarcode` fetches full payload | Resolved (Session 04) |

---

## Session 03 -- Re-verification Pass (2026-03-22)

All four findings in `handoffs/code-review-01-data layer.md` were already resolved by Session 02.
Code confirmed in the correct state; no further changes needed.

```
./gradlew testDebugUnitTest -- BUILD SUCCESSFUL (all tasks UP-TO-DATE)
```

All 277 tests pass, 0 failures.
