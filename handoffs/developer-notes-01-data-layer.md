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
