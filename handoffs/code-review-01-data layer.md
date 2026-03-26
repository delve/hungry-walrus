# Code Review: Session 01 -- Data Layer

**Reviewer**: Code Review Agent
**Date**: 2026-03-22
**Developer notes reviewed**: `handoffs/developer-notes-01-data layer.md`
**Architecture reference**: `handoffs/architecture.md` (Revision 1, 2026-03-22)

---

## Scope

This review covers the data layer as implemented after session 01:

- `data/local/` — Room entities, DAOs, database class
- `data/remote/` — Retrofit service interfaces, DTOs, response mappers
- `data/repository/` — Repository implementations and interfaces
- `di/` — Hilt DI modules (DatabaseModule, NetworkModule, RepositoryModule)
- `domain/model/` — Domain model classes
- `domain/usecase/` — Use case classes
- `worker/DataRetentionWorker.kt`
- Associated unit and integration tests

---

## Pass 1 — 2026-03-22

### Summary

The data layer is well-structured and faithfully implements the architecture spec for most concerns. The Room schema, repository implementations, caching strategy, error-handling taxonomy (IOException vs HttpException), DI modules, and use cases are all correct. All 277 tests pass. No critical issues found. Two warnings and two observations are noted.

---

### Critical Issues

None.

---

### Warnings

---

#### W01 — UTC timezone hardcoded for day-boundary arithmetic

**Files**: [LogEntryRepositoryImpl.kt](app/src/main/java/com/delve/hungrywalrus/data/repository/LogEntryRepositoryImpl.kt#L17-L18), [NutritionPlanRepositoryImpl.kt](app/src/main/java/com/delve/hungrywalrus/data/repository/NutritionPlanRepositoryImpl.kt#L26)

Both repositories convert `LocalDate` to epoch millis using `ZoneOffset.UTC`:

```kotlin
// LogEntryRepositoryImpl.kt
val startOfDay = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
val endOfDay = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

// NutritionPlanRepositoryImpl.kt
val millis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
```

`System.currentTimeMillis()` (used when saving entries and plans) always produces UTC epoch millis. This means a log entry saved at 12:30 AM BST (UTC+1, i.e. 11:30 PM UTC the previous day) would be assigned to the wrong calendar day when queried with UTC day boundaries. The same applies to plan `effectiveFrom` timestamps read back via `getPlanForDate`.

The architecture document does not specify which timezone to use for day-boundary arithmetic. The `CLAUDE.md` specifies UK formatting conventions. For UK users:

- **GMT (Oct–Mar)**: UTC == local time. No discrepancy.
- **BST (Mar–Oct, UTC+1)**: Entries logged between midnight and 1 AM local time land on the previous UTC day. This is a real behavioural discrepancy for roughly six months of the year.

The correct fix is to use `ZoneId.systemDefault()` (device timezone) or a fixed `ZoneId.of("Europe/London")` depending on whether the app is intended for UK users exclusively.

**Recommendation**: Resolve the timezone strategy in the architecture document, then update both repositories consistently. Using `ZoneId.systemDefault()` is the most general fix; `ZoneId.of("Europe/London")` is appropriate if the app is UK-only. Using `ZoneOffset.UTC` is only correct if the app is explicitly defined as UTC-only.

**Status**: Resolved (Pass 3). Both `LogEntryRepositoryImpl` and `NutritionPlanRepositoryImpl` now use `ZoneId.systemDefault()` for all `LocalDate`-to-epoch conversions. Day boundaries now follow the device timezone.

---

#### W02 — `EncryptedSharedPreferences` recovery path can crash if second creation also fails

**File**: [NetworkModule.kt](app/src/main/java/com/delve/hungrywalrus/di/NetworkModule.kt#L51-L63)

The recovery block clears the corrupted preferences file and calls `createEncryptedPrefs(context)` a second time without a try/catch. If the Android Keystore is permanently unavailable (e.g. device hardware failure, factory reset in progress), the second call throws and the app crashes at Hilt graph construction time, producing a fatal startup crash with no user-visible recovery path.

This was documented as a known gap in the developer notes.

**Recommendation**: Wrap the second `createEncryptedPrefs(context)` call in a try/catch and fall back to an unencrypted `SharedPreferences` instance if it also fails. Log a warning so the issue is visible in crash reporting.

**Status**: Resolved (Pass 3). The second `createEncryptedPrefs(context)` call is now wrapped in a nested try/catch. On failure it falls back to a plain `SharedPreferences` instance (`encrypted_prefs_plain`) and logs a warning via `android.util.Log.w`. The app remains usable; the API key will not persist across restarts in this degraded state.

---

### Observations

---

#### O01 — `FoodCacheDao.get(cacheKey)` is dead code; per-selection food caching is unimplemented

**File**: [FoodCacheDao.kt](app/src/main/java/com/delve/hungrywalrus/data/local/dao/FoodCacheDao.kt#L13)

The DAO defines `get(cacheKey: String): FoodCacheEntity?` (line 13) for composite-key cache lookup. The method is never called anywhere in the codebase. `FoodLookupRepositoryImpl` only calls `getByBarcode()`.

Architecture section 6.2 states: *"Individual food items from search results are not pre-cached; they are cached when the user selects a specific item and its per-100g data is resolved."* The `get(cacheKey)` method is the natural lookup counterpart to this caching path, but neither the cache-write side (no `cacheResult()` call for search selections) nor the read side (`get(cacheKey)`) is wired up in the repository. The DAO method is dead code and the per-selection caching path is unimplemented.

This may be intentional for session 01 scope (the caching trigger likely belongs in `AddEntryViewModel`). However, without a corresponding repository method such as `cacheFood(result: FoodSearchResult)`, the ViewModel cannot trigger this caching without bypassing the repository layer.

**Recommendation**: If the per-selection caching is out of scope for the data session, remove `FoodCacheDao.get(cacheKey)` and add it back when the path is implemented end-to-end. Otherwise, add a `cacheFood(result: FoodSearchResult)` method to `FoodLookupRepository` and wire it from `AddEntryViewModel.selectFood()`.

**Status**: Resolved (Pass 3). `FoodCacheDao.get(cacheKey)` has been removed from the DAO. The per-selection caching path remains unimplemented; deferred to a future session.

---

#### O02 — `RecipeIngredientDao.getByRecipeId` is dead code

**File**: [RecipeIngredientDao.kt](app/src/main/java/com/delve/hungrywalrus/data/local/dao/RecipeIngredientDao.kt#L12)

`getByRecipeId(recipeId: Long): Flow<List<RecipeIngredientEntity>>` is defined on the DAO but never called in `RecipeRepositoryImpl` or anywhere else. The repository loads ingredients via `recipeDao.getById(id)` which uses a Room `@Relation` (defined in `RecipeWithIngredients`) to load ingredients atomically as part of the `@Transaction` query. The explicit per-recipe ingredient Flow method is redundant.

**Recommendation**: Remove `getByRecipeId` from `RecipeIngredientDao`. If a future use case requires streaming ingredients independently, re-add it at that point.

**Status**: Resolved (Pass 3). `RecipeIngredientDao.getByRecipeId` has been removed.

---

### Items confirmed correct

The following areas were reviewed and found to match the architecture specification:

| Area | Verdict |
|---|---|
| Room schema — all five entities match architecture section 5.2 exactly | Correct |
| `RecipeIngredientEntity` — `ForeignKey(CASCADE)` + index on `recipeId` | Correct |
| `HungryWalrusDatabase` — version 1, `exportSchema = true`, no destructive migration | Correct |
| `DatabaseModule` — all five DAO providers annotated `@Singleton` (architecture item m2) | Correct |
| `NutritionPlanRepositoryImpl.getCurrentPlan()` — uses `Long.MAX_VALUE` to emit all plans; comment explains rationale | Correct |
| `FoodLookupRepositoryImpl.lookupBarcode` — IOException falls back to stale cache; HttpException does not (architecture item m1) | Correct |
| HTTP 404 on barcode lookup returns `Result.success(null)` | Correct |
| `mapHttpError` covers 400, 403, 429, 500–599 consistently for both API clients | Correct |
| `RepositoryModule` — all four repositories bound `@Singleton` | Correct |
| `NetworkModule` — separate `@Named("usda")` / `@Named("off")` OkHttp clients and Retrofit instances | Correct |
| USDA API key appended via interceptor; logging interceptor only active in `BuildConfig.DEBUG` | Correct |
| `DataRetentionWorker` — 730-day log retention, 30-day cache retention; both thresholds match repository constants | Correct |
| `ComputeRollingSummaryUseCase` — `totalTarget` is `null` unless every day in the period has a plan | Correct |
| `ScaleNutritionUseCase` — `require(weightG >= 0.0)` and `require(recipe.totalWeightG > 0.0)` guards | Correct |
| `ValidateFoodDataUseCase.applyOverrides` — `missingFields` re-derived after overrides applied | Correct |
| `RecipeRepositoryImpl.saveRecipe` and `updateRecipe` wrapped in `database.withTransaction` | Correct |
| `RecipeRepositoryImpl.toEntity(overrideRecipeId)` — always sets `id = 0` for new ingredient inserts | Correct |
| `OffResponseMapper` — falls back to `product.code` as name when `productName` is null/blank | Correct |
| `OffApiService` fields parameter limits response to `code,product_name,nutriments` | Correct |
| No Room type converters registered — all fields are primitives, Strings, or nullable Doubles | Correct |
| Test coverage — all 30 test classes pass; repository, mapper, use case, and integration tests present | Correct |

---

## Pass 2 — 2026-03-22

### Summary

No code changes were observed that address any Pass 1 findings; W01, W02, O01, and O02 remain open. Two additional observations are raised from closer inspection of the repository interfaces.

---

### Critical Issues

None.

---

### Warnings

No new warnings.

---

### Observations

---

#### O03 — `NutritionPlanRepository.getCurrentPlan()` doc comment describes a limitation the implementation does not have

**File**: [NutritionPlanRepository.kt](app/src/main/java/com/delve/hungrywalrus/data/repository/NutritionPlanRepository.kt#L8-L16)

The interface KDoc states:

> **Snapshot limitation**: the "current time" used to evaluate which plan is active is captured once when the Flow is first collected. Room re-executes the query on database changes, but the timestamp predicate does not update. If the app session spans midnight, a plan whose `effectiveFrom` falls after the collection instant will not appear until the Flow is re-collected.

The implementation passes `Long.MAX_VALUE` as `now`, so the SQL predicate is effectively `effectiveFrom <= 9223372036854775807` — always true for any stored timestamp. There is no snapshot of "current time" and no midnight staleness problem. The impl comment correctly explains this:

> *"Pass Long.MAX_VALUE so every plan ever inserted is eligible … since we never insert plans with a future effectiveFrom, this reliably returns the latest active plan."*

The interface comment contradicts the implementation and could mislead a reader into thinking the midnight-boundary issue is present when it is not.

**Recommendation**: Replace the interface KDoc with a description that matches the actual `Long.MAX_VALUE` strategy and notes that plans are always inserted with `effectiveFrom = System.currentTimeMillis()`, so no future-dated plan can slip through.

**Status**: Resolved (Pass 4). The KDoc now correctly describes the `Long.MAX_VALUE` strategy: every inserted plan is eligible, plans are always saved with `effectiveFrom = System.currentTimeMillis()` so no future-dated plan exists, and Room re-executes the query on every table change so updates are reflected immediately without re-collection.

---

#### O04 — `OffApiService.getProductByBarcode` fetches the full product payload

**File**: [OffApiService.kt](app/src/main/java/com/delve/hungrywalrus/data/remote/openfoodfacts/OffApiService.kt#L19-L22)

The search endpoint correctly limits the response to three fields:

```kotlin
@Query("fields") fields: String = "code,product_name,nutriments",
```

The barcode endpoint has no equivalent `fields` parameter, so it receives the full OFF product object — potentially hundreds of fields including ingredient lists, packaging details, image URLs, and category taxonomy. All extra fields are silently discarded by `ignoreUnknownKeys = true`, so behaviour is correct, but the response payload is much larger than necessary.

**Recommendation**: Add `@Query("fields") fields: String = "code,product_name,nutriments"` to `getProductByBarcode` to match the search endpoint's field projection and reduce response size.

**Status**: Resolved (Pass 4). `@Query("fields") fields: String = "code,product_name,nutriments"` has been added to `getProductByBarcode`, matching the search endpoint's field projection.

---

## Pass 3 — 2026-03-22

### Summary

Four findings from earlier passes were resolved. O03 and O04 remain open. No new findings.

| Finding | Resolution |
|---|---|
| W01 | Resolved — both repositories updated to `ZoneId.systemDefault()` |
| W02 | Resolved — second `createEncryptedPrefs` call wrapped in nested try/catch with plain-prefs fallback |
| O01 | Resolved — `FoodCacheDao.get(cacheKey)` removed |
| O02 | Resolved — `RecipeIngredientDao.getByRecipeId` removed |
| O03 | Open — `NutritionPlanRepository` interface KDoc unchanged |
| O04 | Open — `OffApiService.getProductByBarcode` still has no `fields` parameter |

### Critical Issues

None.

### Warnings

None.

### Observations

None new.

---

## Pass 4 — 2026-03-22

### Summary

Both remaining open findings resolved. All six findings from Passes 1–2 are now closed. No new findings.

| Finding | Resolution |
|---|---|
| O03 | Resolved — `NutritionPlanRepository.getCurrentPlan()` KDoc rewritten to accurately describe the `Long.MAX_VALUE` strategy |
| O04 | Resolved — `@Query("fields")` parameter added to `getProductByBarcode`, matching the search endpoint's field projection |

### Critical Issues

None.

### Warnings

None.

### Observations

None.
