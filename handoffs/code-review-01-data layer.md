# Code Review: Session 01 — Data Layer (Round 5)
*Reviewed: 2026-03-20*

## Summary

This round reviews the data layer after Fix Session 05, which addressed the single remaining minor finding from Round 4: the catch-all `Exception` block in `FoodLookupRepositoryImpl.lookupBarcode` was still returning a stale expired cache entry on non-network, non-HTTP errors (e.g. a serialisation failure). That fix has been correctly applied. A companion test was added to verify the corrected behaviour.

All source files across the data layer — repositories, remote DTOs, mappers, DI modules, and domain models — were reviewed. Every finding from all prior rounds has been addressed. The DI graph is correctly structured, error handling is consistent across all error categories, and the module boundaries and package layout comply with the architecture document.

The test suite stands at 49 data-layer tests across 6 test classes. Test quality is high: mapping tests assert complete field sets, error-path tests cover all distinct `try/catch` branches, and cache expiry boundary conditions are explicitly tested. No new issues were found in any source file.

Severity counts: Critical: 0 / Major: 0 / Minor: 0

---

## Findings

### Critical

None.

---

### Major

None.

---

### Minor

None.

---

### Positive observations

- The Round 4 finding is correctly resolved. `FoodLookupRepositoryImpl.lookupBarcode` (lines 84–86) now returns `Result.failure(Exception("Could not read food data"))` unconditionally from the catch-all `Exception` block. The stale-cache fallback is gone, matching the behaviour of the already-corrected `HttpException` block and the reasoning in the architecture document's error table (section 8.4).

- The new test `lookupBarcode returns failure on unexpected Exception even when cache is expired` (lines 351–370 of `FoodLookupRepositoryImplTest.kt`) correctly exercises the fixed path: a 31-day-old cache entity is present, a `RuntimeException` is thrown from the API call, and the test asserts `result.isFailure` with message "Could not read food data". The fixture setup is identical in structure to the two `HttpException`-with-expired-cache tests added in the previous session, making the test suite internally consistent.

- `FoodLookupRepositoryImpl` has no stray dependencies: no `SharedPreferences`, no `Context`, no `NetworkModule` import. The constructor takes exactly the three collaborators it uses (`UsdaApiService`, `OffApiService`, `FoodCacheDao`), and all three are injected by Hilt via `RepositoryModule` and the provider methods in `NetworkModule` and `DatabaseModule`.

- The `OfflineException` class lives correctly in `com.delve.hungrywalrus.domain` (`domain/OfflineException.kt`, line 1), not in `data.repository`. Both `FoodLookupRepositoryImpl` (line 9) and `FoodLookupRepositoryImplTest` (line 10) import it from the domain package. ViewModels can catch `OfflineException` without a dependency on the data layer.

- `UsdaApiService.searchFoods` (lines 9–13) has no `api_key` parameter. The key is added solely by the OkHttp interceptor in `NetworkModule.provideUsdaOkHttpClient` (lines 84–94). There is no duplication of the key across the URL.

- All five DAO provider methods in `DatabaseModule` (lines 34–60) are annotated `@Singleton`, consistent with `provideDatabase` and all providers in `NetworkModule`. The DI graph is unambiguous.

- `LogEntryRepositoryImpl.getEntriesForDate` and `getEntriesForRange` (lines 17–29) compute the upper bound as `date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()`. This is a clean half-open interval with no one-millisecond gap at midnight boundaries.

- `RecipeRepositoryImpl.saveRecipe` and `updateRecipe` (lines 39–55) are wrapped in `database.withTransaction {}`. The write path is atomic; a failure at any step rolls back the entire operation.

- `RecipeRepositoryImpl.saveRecipe` returns the DAO-generated `Long` ID (line 44), matching the interface signature (line 11 of `RecipeRepository.kt`). `CreateRecipeViewModel` will be able to navigate to the recipe detail screen using this ID after a successful save.

- The `NutritionPlanRepository.getCurrentPlan` snapshot limitation is documented at both the interface level (lines 10–17 of `NutritionPlanRepository.kt`) and the implementation level (lines 16–21 of `NutritionPlanRepositoryImpl.kt`). The KDoc is precise and actionable for the ViewModel author.

- The `savePlan` test (lines 102–118 of `NutritionPlanRepositoryTest.kt`) brackets the call with `System.currentTimeMillis()` before and after to assert `effectiveFrom in beforeCall..afterCall`. This is a correct and robust pattern for testing timestamp-generating code without hardcoding epoch values.

---

## Test coverage assessment

The 49-test suite provides substantive and well-structured coverage of the data layer.

**`FoodLookupRepositoryImplTest` (20 tests)** covers every error path through all three public methods. `lookupBarcode` has tests for: fresh cache hit (no network); status-0 response; offline with expired cache (stale fallback permitted); network success with caching verified; cache expiry boundaries at 29 and 31 days; HTTP 500 with no cache; HTTP 500 with expired cache (failure, not stale cache); HTTP 404 with expired cache (success null); unexpected `RuntimeException` with expired cache (failure, not stale cache); and null nutriments populating all four `missingFields`. The `searchUsda` and `searchOpenFoodFacts` methods each have tests for `IOException` (yields `OfflineException`), `HttpException` with mapped message, unexpected `Exception`, and successful mapping without caching. All branches of `mapHttpError` that appear in error messages are tested (403, 429, 5xx). The `cacheResult` entity-content test verifies `source == "OFF"` and `barcode == scanned value`.

**`RecipeRepositoryImplTest` (8 tests)** covers both directions of `saveRecipe` (DAO-generated ID forwarded to ingredients; ID returned to caller), the delete-before-reinsert order in `updateRecipe` (verified with `coVerifyOrder`), `updateRecipe` ingredient `recipeId` propagation, `getAllRecipes` entity-to-domain mapping, `deleteRecipe` DAO delegation, and both nullable and non-null paths through `getRecipeWithIngredients`. The `mockkStatic`/`unmockkStatic` pairing for `withTransaction` is correctly cleaned up in `@After`.

**`NutritionPlanRepositoryTest` (5 tests)** and **`LogEntryRepositoryTest` (5 tests)** assert complete field sets for their primary mapping paths. All fields of `NutritionPlan` and `LogEntry` are covered in the respective conversion tests.

**`UsdaResponseMapperTest` (5 tests)** and **`OffResponseMapperTest` (6 tests)** together cover all permutations of present, missing, and null nutrient values for both APIs, the product-name fallback to barcode for null and blank names, and the list-mapping entry points.

Two minor gaps noted in Round 4 remain open but are not material:

- `addEntry` in `LogEntryRepositoryTest` (line 89) does not assert `captured.id`. Since `LogEntry.id` defaults to `0` and is passed through unchanged by `toEntity()`, this is a trivially correct field. The omission is acceptable given that the `@Insert` DAO contract is what controls the final stored ID.

- `getAllRecipes` in `RecipeRepositoryImplTest` (line 133) asserts `name` and `id` only, not the numeric totals or timestamps. The mapping is a direct field-by-field copy with no transformation logic, so the risk of a field transposition is low. The gap is acknowledged but does not represent meaningful uncovered behaviour.

Neither gap represents an untested code path or a correctness risk. The overall coverage is the strongest it has been across all five review rounds.

---

## Conclusion

The data layer is complete and clean. Every finding across all five review rounds has been addressed. The architecture document's constraints — correct DI scoping, atomic recipe writes, half-open epoch-millis date ranges, cache-then-network barcode strategy, error categorisation per section 8.4, and `OfflineException` in the domain package — are all satisfied in the current source. No blocking issues remain.

The layer is ready to support UI layer development without further revisits. The ViewModel authors should note the two items carried over from the session notes: `SettingsViewModel` must use `NetworkModule.ENCRYPTED_PREFS_FILE` and `NetworkModule.USDA_API_KEY_PREF` when accessing the encrypted preferences instance; and `CreateRecipeViewModel` should use the `Long` returned by `RecipeRepository.saveRecipe` to navigate to the recipe detail screen after a successful create.
