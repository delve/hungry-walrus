# Developer Notes: Session 01 - Data Layer

## What was implemented

### Domain models (5 files)
- `domain/model/NutritionPlan.kt`
- `domain/model/LogEntry.kt`
- `domain/model/Recipe.kt`
- `domain/model/RecipeIngredient.kt`
- `domain/model/RecipeWithIngredients.kt` (domain version, separate from Room relation class)

### USDA FoodData Central remote (3 files)
- `data/remote/usda/UsdaApiService.kt` - Retrofit interface for `foods/search`
- `data/remote/usda/UsdaDto.kt` - `@Serializable` DTOs: `UsdaSearchResponse`, `UsdaFood`, `UsdaNutrient`
- `data/remote/usda/UsdaResponseMapper.kt` - Maps USDA foods to `FoodSearchResult` using nutrient IDs 1008/1003/1005/1004

### Open Food Facts remote (3 files)
- `data/remote/openfoodfacts/OffApiService.kt` - Retrofit interface for search and barcode lookup
- `data/remote/openfoodfacts/OffDto.kt` - `@Serializable` DTOs with `@SerialName` for hyphenated JSON keys
- `data/remote/openfoodfacts/OffResponseMapper.kt` - Maps OFF products to `FoodSearchResult`, falls back to product code when name is null/blank

### Repositories (8 files)
- `data/repository/NutritionPlanRepository.kt` (interface)
- `data/repository/NutritionPlanRepositoryImpl.kt` - Maps entities to domain, converts LocalDate to epoch millis via UTC
- `data/repository/LogEntryRepository.kt` (interface)
- `data/repository/LogEntryRepositoryImpl.kt` - Handles date-to-epoch conversion (start of day = midnight, end of day = 23:59:59.999)
- `data/repository/RecipeRepository.kt` (interface)
- `data/repository/RecipeRepositoryImpl.kt` - Delete-and-reinsert strategy for ingredient updates; sets id=0 on ingredient entities when saving to force auto-generation
- `data/repository/FoodLookupRepository.kt` (interface)
- `data/repository/FoodLookupRepositoryImpl.kt` - Cache-then-network for barcode, network-first for text search, ConnectivityManager check, HTTP error mapping
- `data/repository/OfflineException.kt` - Custom exception for offline state

### DI modules (2 files updated)
- `di/NetworkModule.kt` - Full implementation with two Retrofit instances (`@Named("usda")` and `@Named("off")`), EncryptedSharedPreferences with resilient try-catch-recreate, OkHttp interceptors, kotlinx-serialization JSON config
- `di/RepositoryModule.kt` - `@Binds` for all four repository interfaces, `@Singleton` scoped

### Removed placeholder files
- `data/remote/usda/package-info.kt`
- `data/remote/openfoodfacts/package-info.kt`
- `data/repository/package-info.kt`

## Deviations from architecture

1. **RecipeRepository.getRecipeWithIngredients return type**: The task spec says `Flow<RecipeWithIngredients>` but the DAO's `getById()` returns `Flow<RecipeWithIngredients?>` (nullable). The repository interface returns `Flow<RecipeWithIngredients?>` to avoid throwing when the recipe does not exist, which is important for the detail screen to handle gracefully.

2. **USDA API key in interceptor vs. service parameter**: The architecture doc says the OkHttp interceptor appends the API key. The `UsdaApiService` also accepts an `apiKey` parameter on the method. The `FoodLookupRepositoryImpl` passes the key explicitly via the service method parameter rather than relying solely on the interceptor. Both paths add the key, but the explicit parameter gives the repository direct control over which key is used and makes testing simpler. The interceptor still exists as a fallback per the architecture spec.

3. **NetworkModule constants**: `ENCRYPTED_PREFS_FILE` and `USDA_API_KEY_PREF` are exposed as public constants on `NetworkModule`'s companion-level scope (top-level `const` in the `object`). This allows the SettingsViewModel to reference the same keys without hardcoding strings.

## Issues encountered

1. **Date epoch calculation in test**: The initial test hardcoded an incorrect epoch millis value for 2026-03-20. Fixed by asserting the relationship between start and end millis (exactly 86,399,999ms apart) and verifying start is on a day boundary, rather than hardcoding a specific epoch value.

## Notes for the next session

- **SettingsViewModel** will need to use `NetworkModule.USDA_API_KEY_PREF` and `NetworkModule.ENCRYPTED_PREFS_FILE` constants when reading/writing the USDA API key. The `SharedPreferences` instance is already provided by Hilt as a singleton from `NetworkModule.provideEncryptedSharedPreferences()`.

- **USDA API key duplication**: Both the OkHttp interceptor and `FoodLookupRepositoryImpl.searchUsda()` read the API key from `EncryptedSharedPreferences`. The interceptor adds it as a query param unconditionally. The service method also accepts it as a parameter. This means the key may appear twice in the URL. The USDA API tolerates this (last value wins), but if this is a concern, either remove the `api_key` parameter from `UsdaApiService.searchFoods()` or remove the interceptor. The interceptor approach is architecturally cleaner; the explicit parameter exists because the task specification requested it on the Retrofit interface.

- **FoodLookupRepositoryImpl** injects `SharedPreferences` (the encrypted instance) and `@ApplicationContext Context`. Both are provided by `NetworkModule` and Hilt respectively.

- **RecipeRepository.saveRecipe** returns `Unit`, not the generated recipe ID. If the create-recipe flow needs the ID after saving, the interface may need adjustment.

- **LogEntryRepositoryImpl** uses `< endOfDay` (exclusive) in the DAO query. The DAO's `getEntriesForDate` query uses `timestamp < :endOfDay`, so setting endOfDay to 23:59:59.999 ensures all entries within the day are captured, including entries at exactly midnight of the next day minus 1ms.

## Unit tests written

### `data/remote/usda/UsdaResponseMapperTest.kt` (5 tests)
- All nutrients present returns complete result with empty missingFields
- Some missing nutrients returns nulls and correct missingFields set
- All nutrients missing returns all nulls and full missingFields set
- Null nutrient values treated as missing
- mapFoods correctly maps a list

### `data/remote/openfoodfacts/OffResponseMapperTest.kt` (6 tests)
- All nutrients present returns complete result
- Some missing nutrients returns nulls and correct missingFields
- All nutrients missing (null nutriments) returns all nulls
- Null product name falls back to code
- Blank product name falls back to code
- mapProducts correctly maps a list

### `data/repository/NutritionPlanRepositoryTest.kt` (5 tests)
- getCurrentPlan emits mapped domain model
- getCurrentPlan emits null when no entity
- getPlanForDate converts date and returns mapped plan
- getPlanForDate returns null when no plan exists
- savePlan inserts entity with correct values

### `data/repository/LogEntryRepositoryTest.kt` (5 tests)
- getEntriesForDate converts date to correct epoch millis range
- getEntriesForDate returns empty list when no entries
- getEntriesForRange converts dates correctly
- addEntry maps domain model to entity
- deleteEntry calls DAO with correct id

**Total: 20 tests, all passing.**

## Tests deleted post-session

Two test cases were removed during review for testing third-party or standard library behaviour rather than our own code:

1. **`LogEntryRepositoryTest` — `date to epoch conversion produces correct values for known date`**: Computed epoch millis directly using `java.time` and asserted properties of the result (e.g. `endMillis - startMillis == 86_399_999`). This verified that `java.time.LocalDate` and `ZoneOffset.UTC` work correctly, which is not our responsibility. The repository's actual date conversion behaviour is already covered by `getEntriesForDate converts date to correct epoch millis range`, which mocks the DAO with the expected millis and confirms the repository passes them through.

2. **`FoodSearchResultTest.kt` (entire file, 4 tests)**: `FoodSearchResult` is a plain data class with no logic — `missingFields` is a constructor parameter, not computed. Tests asserting `result.missingFields.isEmpty()` or `result.missingFields.size == 2` only verified Kotlin data class storage. Two further tests called `FoodSource.valueOf("USDA")` and checked `FoodSource.entries.size`, which test Java's enum API. The meaningful coverage — that mappers correctly compute and populate `missingFields` — is already provided by `UsdaResponseMapperTest` and `OffResponseMapperTest`.

---

## Fix Session Notes — Code Review 01

### Findings addressed

**Critical #1 — One-millisecond gap in day-boundary queries (fixed)**
Changed `LogEntryRepositoryImpl.getEntriesForDate` and `getEntriesForRange` to compute the upper bound as `date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()` instead of `date.atTime(23:59:59.999)`. The `LocalTime` import was removed. The `LogEntryRepositoryTest` expected-value calculations were updated to match.

**Critical #2 — Non-atomic recipe update/save (fixed)**
`RecipeRepositoryImpl` now injects `HungryWalrusDatabase` and wraps the multi-step DAO calls in `database.withTransaction {}`. Both `saveRecipe` and `updateRecipe` are now atomic. If any step fails, the transaction rolls back and the database is never left in a torn state.

**Major #3 — USDA API key sent twice (fixed)**
Removed `@Query("api_key")` parameter from `UsdaApiService.searchFoods`. The OkHttp interceptor in `NetworkModule` is now the sole path for appending the API key. The explicit `apiKey` argument was removed from `FoodLookupRepositoryImpl.searchUsda`.

**Major #4 — `FoodLookupRepositoryImpl` importing `NetworkModule` (fixed)**
Since the explicit `apiKey` parameter was removed from `searchUsda`, `FoodLookupRepositoryImpl` no longer needs `encryptedPrefs` or the `NetworkModule` import. Both were removed. The constructor no longer takes `SharedPreferences`.

**Major #5 — `RecipeRepository.saveRecipe` returns `Unit` (fixed)**
Interface changed to `suspend fun saveRecipe(...): Long`. `RecipeRepositoryImpl` now returns the DAO-generated ID from inside the `withTransaction` block.

**Minor #6 — `NutritionPlan.id` has no default (fixed)**
Added `val id: Long = 0` default to match the pattern used by `LogEntry`, `Recipe`, and `RecipeIngredient`.

**Minor #7 — `RecipeIngredient` missing pre-condition documentation (fixed)**
Added KDoc comment on `RecipeIngredient` documenting that all per-100g nutrition fields must be populated before construction, and that `FoodSearchResult` items with missing fields require caller-supplied estimates.

**Minor #8 — `OfflineException` package location (fixed)**
`OfflineException` now lives in `com.delve.hungrywalrus.domain`. `data/repository/OfflineException.kt` has been deleted. `FoodLookupRepositoryImpl` and `FoodLookupRepositoryImplTest` import from the `domain` package. ViewModels can catch `OfflineException` without taking a dependency on `data.repository`.

**Minor #9 — TOCTOU preflight network checks (fixed)**
Removed the `isNetworkAvailable()` pre-flight checks from `searchUsda` and `searchOpenFoodFacts`. These were a race (network could change between check and call), and the existing `IOException` catch blocks already handle the offline case correctly. The check is kept in `lookupBarcode` where it gates the cache-fallback path.

**Minor #10 — `UsdaFood.fdcId` typed as `Int` (fixed)**
Changed `fdcId: Int` to `fdcId: Long` in `UsdaDto.kt`. The `UsdaResponseMapper` already uses `fdcId` only in a string template, which works identically with `Long`. Test literals (e.g. `fdcId = 12345`) required no `L` suffix because Kotlin widens integer literals to `Long` in assignment contexts.

**Minor #11 — `coerceInputValues = true` in `Json` config (not changed)**
No DTOs use enum fields, so this setting has no current effect. Left in place to preserve existing behaviour; the review noted it is safe to keep.

### New tests added

**`FoodLookupRepositoryImplTest.kt` (11 tests)**
- `lookupBarcode` returns cached result without network call when cache is fresh
- `lookupBarcode` returns `Result.success(null)` when API returns `status == 0`
- `lookupBarcode` returns expired cache when device is offline
- `searchUsda` returns `Result.failure(OfflineException)` on `IOException`
- `searchUsda` returns failure with "Invalid API key" message on HTTP 403
- `searchUsda` returns failure with "Too many requests" message on HTTP 429
- `searchUsda` caches each result after successful search
- `searchOpenFoodFacts` caches results after successful search
- `searchOpenFoodFacts` returns `Result.failure(OfflineException)` on `IOException`
- Cache older than 30 days is treated as expired and network is attempted
- Cache within 30 days is not expired and network is not called

**`RecipeRepositoryImplTest.kt` (6 tests)**
- `saveRecipe` uses DAO-generated ID for ingredient inserts, not the `Recipe.id` field
- `saveRecipe` returns the DAO-generated recipe ID
- `updateRecipe` deletes old ingredients before inserting new ones (order verified with `coVerifyOrder`)
- `updateRecipe` inserts new ingredients with the recipe's ID
- `getRecipeWithIngredients` returns `null` in flow when recipe does not exist
- `getRecipeWithIngredients` returns mapped domain model when recipe exists

**`NutritionPlanRepositoryTest.kt` — strengthened `savePlan` assertion**
The `effectiveFrom > 0` check was replaced with `captured.effectiveFrom in beforeCall..afterCall`, bracketing the call with `System.currentTimeMillis()` before and after to verify the timestamp is within the actual call window.

**Total: 55 tests, all passing** (up from 20).

### Implementation notes for next session

- **`OfflineException` package move**: Before writing any ViewModel that catches `OfflineException`, delete `data/repository/OfflineException.kt` and move the class to `com.delve.hungrywalrus.domain`. Update the import in `FoodLookupRepositoryImpl` accordingly.
- **`RecipeRepositoryImpl` now requires `HungryWalrusDatabase`**: This is injected by Hilt via `DatabaseModule`. No changes to DI modules are required.
- **`FoodLookupRepositoryImpl` no longer takes `SharedPreferences`**: The `RepositoryModule` `@Binds` binding is unaffected (no constructor args declared there), but any manual construction in tests must be updated.
- **`RecipeRepository.saveRecipe` now returns `Long`**: The `CreateRecipeViewModel` should use this ID to navigate to the recipe detail screen after a successful save.

---

## Fix Session 02 — 2026-03-20: OfflineException package move

**Minor #8 completed** — The `OfflineException` package move that was deferred in Fix Session 01 is now done.

Changes made:
- `domain/OfflineException.kt` — placeholder replaced with the real class (`package com.delve.hungrywalrus.domain`), KDoc preserved.
- `data/repository/OfflineException.kt` — deleted.
- `data/repository/FoodLookupRepositoryImpl.kt` — added `import com.delve.hungrywalrus.domain.OfflineException`; the class was previously resolved by same-package lookup.
- `data/repository/FoodLookupRepositoryImplTest.kt` — added `import com.delve.hungrywalrus.domain.OfflineException`; same reason.

No new tests required — the `FoodLookupRepositoryImplTest` tests that assert `is OfflineException` already cover the moved class. All 55 tests pass after the change.

---

## Fix Session 03 — 2026-03-20: Code Review 01 Round 2 findings

### M1 (Major) — `searchUsda` silent empty results when API key not configured

**Product Owner Override: not fixed.** The review finding was overridden by the Product Owner: "The Domain layer prevents attempted lookup when the key is missing." No code change made. This decision is recorded here so the ViewModel layer knows not to rely on the repository to surface an unconfigured-key failure — gating must happen in the Domain or UI layer before `searchUsda` is called.

### m1 (Minor) — `getCurrentPlan` snapshot-at-collection-time behaviour

**Fixed.** Added KDoc comments to `NutritionPlanRepository.getCurrentPlan()` (interface) and `NutritionPlanRepositoryImpl.getCurrentPlan()` (impl) documenting that `System.currentTimeMillis()` is captured once at collection time. Room re-executes the query on database changes but the timestamp predicate is fixed to that initial value. The `DailyProgressViewModel` must re-collect the flow (e.g. on navigation) to pick up plans whose `effectiveFrom` falls after the original collection instant.

### m2 (Minor) — TOCTOU `isNetworkAvailable()` preflight in `lookupBarcode`

**Already fixed in the current codebase.** Inspection of `FoodLookupRepositoryImpl.kt` confirmed the `isNetworkAvailable()` call was not present — it had been removed in a prior session. However, `FoodLookupRepositoryImplTest` still held stale scaffolding from when the check existed: `Context` and `ConnectivityManager` fields, a `setNetworkAvailable()` helper, and a 4-argument constructor call that would not compile against the current 3-argument constructor. These were cleaned up:

- Removed `import android.content.Context`, `ConnectivityManager`, `Network`, `NetworkCapabilities`
- Removed `context` and `connectivityManager` fields from setUp
- Changed constructor call to `FoodLookupRepositoryImpl(usdaApiService, offApiService, foodCacheDao)`
- Removed `setNetworkAvailable()` helper
- Updated `lookupBarcode returns expired cache when device is offline` to throw `IOException` from the API mock instead of relying on the removed network check

### m3 (Minor) — `cacheResult` called for every search result item

**Fixed.** Removed `results.forEach { cacheResult(it) }` from both `searchUsda` and `searchOpenFoodFacts`. Caching is now deferred to the point where the user selects a specific food item (i.e. `lookupBarcode`), matching the architecture document's cache strategy (section 6.2). Two existing tests that asserted `coVerify(exactly = 1) { foodCacheDao.insert(any()) }` for search paths were updated to assert `coVerify(exactly = 0) { foodCacheDao.insert(any()) }` and renamed accordingly.

### New tests added

**`FoodLookupRepositoryImplTest.kt` — 5 new tests (11 → 16)**

- `searchOpenFoodFacts returns failure with mapped message on HttpException` — verifies the `mapHttpError` path for OFF search
- `searchOpenFoodFacts returns failure with generic message on unexpected Exception` — verifies the catch-all Exception path for OFF search
- `lookupBarcode returns failure on HttpException when no cache` — covers HTTP 5xx with no cached fallback
- `lookupBarcode populates missingFields when API response has null nutriments` — asserts all 4 fields are in `missingFields` for a null-nutriments barcode response
- `lookupBarcode caches entity with source OFF and barcode set` — verifies `cacheResult` entity content: `source == "OFF"` and `barcode == <scanned barcode>`

**`RecipeRepositoryImplTest.kt` — 2 new tests (6 → 8)**

- `getAllRecipes returns mapped domain list from DAO` — verifies entity-to-domain mapping for the recipe list flow
- `deleteRecipe calls DAO with correct id` — verifies `recipeDao.deleteById` is called with the correct ID

### Test run results

All data-layer tests passed:

- `FoodLookupRepositoryImplTest`: 16 passed
- `RecipeRepositoryImplTest`: 8 passed
- `NutritionPlanRepositoryTest`: 5 passed
- `LogEntryRepositoryTest`: 5 passed
- `UsdaResponseMapperTest`: 5 passed
- `OffResponseMapperTest`: 6 passed

**Total: 45 data-layer tests, 0 failures, 0 errors.**

---

## Fix Session Verification — 2026-03-20

All source-level fixes from the previous fix session were confirmed present in the codebase. No additional code changes were required. All tests were re-executed from scratch (forced rerun, no cache):

- `OffResponseMapperTest`: 6 passed
- `UsdaResponseMapperTest`: 5 passed
- `FoodLookupRepositoryImplTest`: 11 passed
- `LogEntryRepositoryTest`: 5 passed
- `NutritionPlanRepositoryTest`: 5 passed
- `RecipeRepositoryImplTest`: 6 passed

**Total: 38 data-layer tests, 0 failures, 0 errors.**

(Additional 17 tests from scaffold layer — `BottomNavItemTest`, `RoutesTest`, `FormatterTest` — also passed.)

---

## Fix Session 04 — 2026-03-20: Code Review 01 Round 3 findings

### m1 — `lookupBarcode` returns stale cache on `HttpException` (fixed)

Changed the `HttpException` catch block in `FoodLookupRepositoryImpl.lookupBarcode` to no longer fall back to a stale (expired) cached entry. The server was reachable, so the offline justification does not apply. New behaviour:

- HTTP 404 → `Result.success(null)` — product not found on the server, same semantics as `status == 0`
- Any other `HttpException` → `Result.failure(Exception(mapHttpError(e.code())))` — server error propagated to caller

The `IOException` path is unchanged: it still returns the stale cache when available, as the offline case is explicitly allowed by architecture section 6.2.

### m2 — DAO provider methods not `@Singleton` scoped (fixed)

Added `@Singleton` to all five DAO provider methods in `DatabaseModule` (`provideNutritionPlanDao`, `provideLogEntryDao`, `provideRecipeDao`, `provideRecipeIngredientDao`, `provideFoodCacheDao`). This is consistent with the `@Singleton` scoping used for `provideDatabase` and all providers in `NetworkModule`, eliminates unnecessary DAO wrapper object churn, and removes ambiguity for anyone tracing the DI graph.

### m3 — Missing `HttpException`-with-expired-cache test (fixed)

Added tests to `FoodLookupRepositoryImplTest` covering the corrected `HttpException` paths in `lookupBarcode`:

- `lookupBarcode returns failure on HttpException when cache is expired, not stale cache` — expired entity in cache, API throws HTTP 500; asserts `Result.isFailure` with "Service temporarily unavailable" message.
- `lookupBarcode returns success null on HTTP 404 even when cache is expired` — expired entity in cache, API throws HTTP 404; asserts `Result.success(null)`.

### Additional test gaps (fixed)

Per the review's "Remaining gaps" section:

- **`searchUsda returns failure with generic message on unexpected Exception`** — added to `FoodLookupRepositoryImplTest`. Makes coverage symmetric with the equivalent `searchOpenFoodFacts` test that was already present.

- **`getPlanForDate` full field assertions** — strengthened `getPlanForDate converts date to epoch millis and returns mapped plan` in `NutritionPlanRepositoryTest` to assert all mapped fields: `proteinTargetG`, `carbsTargetG`, `fatTargetG`, and `effectiveFrom` in addition to `id` and `kcalTarget`.

- **`getEntriesForDate` full field assertions** — strengthened `getEntriesForDate converts date to correct epoch millis range` in `LogEntryRepositoryTest` to assert `id`, `proteinG`, `carbsG`, `fatG`, and `timestamp` in addition to `foodName` and `kcal`.

### Test run results

All data-layer tests passed:

- `OffResponseMapperTest`: 6 passed
- `UsdaResponseMapperTest`: 5 passed
- `FoodLookupRepositoryImplTest`: 19 passed (was 16; +3 new tests)
- `LogEntryRepositoryTest`: 5 passed
- `NutritionPlanRepositoryTest`: 5 passed
- `RecipeRepositoryImplTest`: 8 passed

**Total: 48 data-layer tests, 0 failures, 0 errors.**

---

## Fix Session 05 — 2026-03-20: Code Review 01 Round 4 findings

### m1 — `lookupBarcode` catch-all `Exception` block still returns stale cache on non-network errors (fixed)

Removed the stale-cache fallback from the catch-all `Exception` block in `FoodLookupRepositoryImpl.lookupBarcode`. The block previously returned `Result.success(cached.toDomain())` when `cached != null` (including expired entries), which is the same class of problem that was fixed for `HttpException` in Fix Session 04. A serialisation or other unexpected error means the server was reachable enough to produce a response, so the offline justification for serving stale data does not apply.

**Before:**
```kotlin
} catch (e: Exception) {
    if (cached != null) {
        Result.success(cached.toDomain())
    } else {
        Result.failure(Exception("Could not read food data"))
    }
}
```

**After:**
```kotlin
} catch (e: Exception) {
    Result.failure(Exception("Could not read food data"))
}
```

### New test added

**`FoodLookupRepositoryImplTest.kt` — 1 new test (19 → 20)**

- `lookupBarcode returns failure on unexpected Exception even when cache is expired` — sets up an expired cache entity (31-day-old `cachedAt`), throws `RuntimeException("serialisation error")` from `offApiService.getProductByBarcode`, and asserts `result.isFailure` with "Could not read food data" message. Mirrors the pattern of `lookupBarcode returns failure on HttpException when cache is expired, not stale cache`.

### Test run results

All data-layer tests passed:

- `OffResponseMapperTest`: 6 passed
- `UsdaResponseMapperTest`: 5 passed
- `FoodLookupRepositoryImplTest`: 20 passed (was 19; +1 new test)
- `LogEntryRepositoryTest`: 5 passed
- `NutritionPlanRepositoryTest`: 5 passed
- `RecipeRepositoryImplTest`: 8 passed

**Total: 49 data-layer tests, 0 failures, 0 errors.**

The data layer is complete. All findings across all review rounds have been addressed. The layer is ready to support UI layer development.
