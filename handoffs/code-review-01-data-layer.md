# Code Review: Session 01 — Data Layer

## Summary

The data layer implementation is structurally sound and closely follows the architecture document. Package layout, naming conventions, layering, DI wiring, and the domain/entity split are all correct. The mapper and repository tests are meaningful and test real behaviour. Two issues require attention before this layer is built upon: a logical contract inconsistency in the end-of-day boundary calculation that creates a one-millisecond gap, and a non-atomic write path in `RecipeRepositoryImpl.updateRecipe` that can leave the database in a torn state. Several smaller issues around API key duplication and missing transaction annotations are noted below.

---

## Findings

### Critical

**1. One-millisecond gap in day-boundary queries — `LogEntryRepositoryImpl.kt` lines 19–21, `LogEntryDao.kt` line 12**

`getEntriesForDate` computes `endOfDay` as `23:59:59.999` (i.e. epoch millis `T + 86_399_999`) and passes it to the DAO. The DAO query uses `timestamp < :endOfDay` (strict less-than). This means an entry timestamped at exactly `23:59:59.999` on the requested day is excluded, and an entry timestamped at `00:00:00.000` of the *next* day is also excluded — which is correct. However, the nanosecond component `999_000_000` rounds to millisecond value `999`, so the effective upper bound is the last millisecond of the day. The strict less-than `< endOfDay` then excludes that millisecond. An entry saved at precisely `23:59:59.999 UTC` will be missed.

The standard and unambiguous approach is to compute `startOfNextDay` and use `timestamp < startOfNextDay`. The developer session notes describe the current approach as intentional but do not acknowledge this edge case. Fix: change the end-of-day boundary to `date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()` and keep `<` exclusive. The same issue applies to `getEntriesForRange` at lines 29–31 of `LogEntryRepositoryImpl.kt` and the equivalent expectation in `LogEntryRepositoryTest.kt` lines 36–38 and 76–78.

**2. Non-atomic recipe update — `RecipeRepositoryImpl.kt` lines 42–46**

`updateRecipe` calls `recipeDao.update(...)`, then `ingredientDao.deleteByRecipeId(...)`, then `ingredientDao.insertAll(...)` as three separate operations. If the process is killed or a database error is thrown between the delete and the insert, the recipe exists in the database with no ingredients. Neither the `updateRecipe` function nor the DAO calls are wrapped in a `@Transaction`. This must be wrapped in a `withTransaction` block (Room's `database.withTransaction { }`) or the repository method must be annotated or delegated through a `@Transaction`-annotated DAO method. The same risk applies to `saveRecipe` at lines 36–39, where if `insertAll` fails after `insert` the recipe row exists without ingredients — although this is a less severe state since the recipe is new and can be retried.

---

### Major

**3. API key sent twice on every USDA search — `NetworkModule.kt` lines 84–93, `FoodLookupRepositoryImpl.kt` line 41, `UsdaApiService.kt` line 13**

The USDA OkHttp interceptor appends `api_key` as a query parameter unconditionally on every request. `FoodLookupRepositoryImpl.searchUsda` also reads the key from `EncryptedSharedPreferences` and passes it explicitly via the `@Query("api_key")` parameter on the Retrofit interface. This results in the URL containing `api_key` twice. The developer session notes document this and state "USDA API tolerates this (last value wins)" — but this is unverified behaviour that could change and it leaks the key redundantly in logs (even if only at `BODY` level in debug builds). The architecturally intended path is the interceptor. The `apiKey` parameter should be removed from `UsdaApiService.searchFoods` and the explicit `apiKey` argument removed from the `searchUsda` call. This is a deviation from the architecture that the notes flag as needing resolution, and it should be resolved before the ViewModel layer is built (which will call `searchUsda` and may be confused by the dual-key design).

**4. `FoodLookupRepositoryImpl` imports `NetworkModule` — `FoodLookupRepositoryImpl.kt` line 13**

The repository imports `com.delve.hungrywalrus.di.NetworkModule` directly to reference `NetworkModule.USDA_API_KEY_PREF`. A repository in the `data` layer depending on a `di` layer class is an inverted dependency. The `di` layer exists to wire the `data` layer together; the `data` layer must not depend on `di`. The constant `USDA_API_KEY_PREF` should be moved to a location accessible to both layers — either a constants file in `data` or passed in as a constructor parameter string. The developer notes acknowledge this design but frame it as a convenience; it nonetheless creates an architectural violation that will make the data layer harder to test in isolation.

**5. `RecipeRepository.saveRecipe` returns `Unit`, discarding the generated ID — `RecipeRepository.kt` line 11, developer notes**

The architecture's `RecipeDao.insert` returns `Long` (the generated ID). `RecipeRepositoryImpl.saveRecipe` captures that ID internally to insert ingredients but does not surface it to callers. The developer notes flag this: "If the create-recipe flow needs the ID after saving, the interface may need adjustment." The `CreateRecipeViewModel` will almost certainly need the ID to navigate to the recipe detail screen after creation. Changing the interface signature later will require updating every call site and any test doubles. The interface should be changed to `suspend fun saveRecipe(...): Long` now.

---

### Minor

**6. `NutritionPlan` domain model lacks a default for `id` — `NutritionPlan.kt` line 4**

`LogEntry`, `Recipe`, and `RecipeIngredient` all have `val id: Long = 0` as a default, which allows call sites to omit `id` when constructing new domain objects. `NutritionPlan` has no default for `id`, requiring callers to supply `id = 0` explicitly when constructing a new plan for saving. This is inconsistent with the other domain models and will be an annoyance in `PlanViewModel`. Add `val id: Long = 0` to match the pattern.

**7. `RecipeIngredient` domain model uses non-nullable `Double` for nutrition fields — `RecipeIngredient.kt` lines 8–11**

The architecture specifies that `RecipeIngredient` stores `kcalPer100g`, `proteinPer100g`, `carbsPer100g`, and `fatPer100g` as `Double` (non-nullable). This is intentional per the schema (ingredients must have complete nutrition data to be saved), but it means that if a `FoodSearchResult` with missing fields is used to populate an ingredient, the caller must supply estimates before constructing the domain object. This constraint is correct but is not enforced at the repository boundary. Document this pre-condition in a KDoc comment on the class or the relevant repository method so the next session's ViewModel developer is aware.

**8. `OfflineException` placed in `data.repository` package — `OfflineException.kt`**

`OfflineException` is thrown by `FoodLookupRepositoryImpl` and is expected to be caught and mapped to UI state by the ViewModel. Its current package (`data.repository`) means the ViewModel layer must import from `data.repository` to reference it in a `catch` or `is` check. The architecture's `data.repository` package is an implementation detail; exception types that cross layer boundaries belong in the `domain` layer or a `data` top-level package. This is low-risk now but will create an awkward import dependency in the ViewModel layer.

**9. `isNetworkAvailable()` check is a TOCTOU race — `FoodLookupRepositoryImpl.kt` lines 116–122**

The connectivity check is performed before the network call, but the network state can change between the check and the call. The existing `IOException` catch blocks will handle the actual failure correctly, making this a cosmetic redundancy rather than a correctness bug. However, for `searchUsda` and `searchOpenFoodFacts`, an `IOException` thrown during the call is caught and re-wrapped as `OfflineException` (line 46, line 63) even though the error may not be an offline condition (it could be a refused connection, a timeout, or a DNS failure). The architecture document specifies that `IOException` maps to the "No network" case, which is consistent with the current implementation, but the pre-flight connectivity check is superfluous and adds a small risk of short-circuiting before cache-first fallback logic is applied. The pre-flight check should be removed from `searchUsda` and `searchOpenFoodFacts`; the `IOException` catch already handles offline correctly. It is appropriate to keep the check in `lookupBarcode` where it gates the cache-fallback path.

**10. `UsdaFood.fdcId` typed as `Int` — `UsdaDto.kt` line 12**

The USDA FoodData Central API documents `fdcId` as an integer but values exceed `Int.MAX_VALUE` (2,147,483,647) for some food types (e.g. Branded Foods IDs). The field should be `Long` to be safe. Even though the architecture restricts data types to `"Foundation,SR Legacy"` which have smaller IDs currently, a defensive `Long` prevents a runtime `NumberFormatException` or silent overflow if ID ranges expand.

**11. `coerceInputValues = true` in `Json` configuration — `NetworkModule.kt` line 42**

`coerceInputValues` forces enum fields to their default value when an unrecognised string is received. The DTOs do not use any enum fields, so this setting has no effect now. It could silently mask unexpected API changes in future DTOs. It is safe to leave in place but worth noting.

---

## Test coverage gaps

**`FoodLookupRepositoryImpl` — no tests written.** This is the most complex class in the session: it handles cache-then-network logic, connectivity checks, HTTP error mapping, barcode-specific cache fallback, and two separate error paths. The following behaviours are completely untested:

- `lookupBarcode` returns a cached result when a valid (non-expired) entry exists, without making a network call.
- `lookupBarcode` returns `Result.success(null)` when the API returns `status == 0`.
- `lookupBarcode` returns an expired cached result when the device is offline and no fresh result is available.
- `searchUsda` returns `Result.failure(OfflineException)` when the network is unavailable.
- `searchUsda` returns `Result.failure` with "Invalid API key" message on HTTP 403.
- `searchUsda` returns `Result.failure` with "Too many requests" message on HTTP 429.
- `searchOpenFoodFacts` caches results after a successful search.
- Cache expiry: `isCacheExpired` returns `true` for entries older than 30 days and `false` for newer ones.

These should be tested using `MockWebServer` or by mocking the Retrofit service and `FoodCacheDao` with MockK, plus a mock `ConnectivityManager` via the `Context`.

**`RecipeRepositoryImpl` — no tests written.** The delete-and-reinsert strategy in `updateRecipe` and the `id = 0` override in `toEntity(overrideRecipeId)` should have test coverage. Specifically:
- `saveRecipe` uses the DAO-generated recipe ID when inserting ingredients (not the `Recipe.id` value).
- `updateRecipe` deletes old ingredients before inserting new ones.
- `getRecipeWithIngredients` returns `null` wrapped in `Flow` when the recipe does not exist.

**`NutritionPlanRepositoryTest` — `savePlan` effectiveFrom assertion is weak.** Line 110 asserts `captured.effectiveFrom > 0`, which would pass even if `effectiveFrom` were set to `1L`. The assertion should verify the timestamp is within a small window of `System.currentTimeMillis()`, e.g. `assert(captured.effectiveFrom >= beforeCall && captured.effectiveFrom <= afterCall)` using timestamps captured before and after the call.

---

## Approved deviations

**`RecipeRepository.getRecipeWithIngredients` returns `Flow<RecipeWithIngredients?>` (nullable).** The architecture specified `Flow<RecipeWithIngredients>` (non-nullable). The nullable return is correct: the DAO returns `Flow<RecipeWithIngredients?>` because Room cannot guarantee the recipe exists, and surfacing `null` to the detail screen is the right way to handle navigation to a deleted recipe. The deviation is valid and acceptable.

**`NetworkModule.ENCRYPTED_PREFS_FILE` and `NetworkModule.USDA_API_KEY_PREF` exposed as public constants.** The architecture did not specify where these string constants should live. Making them public on `NetworkModule` allows the `SettingsViewModel` to reference them without duplication. The approach is pragmatic. The concern about `FoodLookupRepositoryImpl` importing `NetworkModule` is noted separately as a finding above; that specific usage is the part that needs correction, not the constants themselves.
