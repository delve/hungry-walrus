# Technical Architecture: Hungry Walrus

## 1. Overview

Hungry Walrus is a local-first Android nutrition tracking app built with Kotlin and Jetpack Compose. It has no backend -- all user data lives on the device. Network access is used solely for food data lookups via two external APIs: USDA FoodData Central and Open Food Facts.

This document defines the app architecture, data models, API integration patterns, navigation structure, dependency injection approach, and key trade-offs. It serves as the primary reference for the Designer and Developer agents.

---

## 2. Target SDK

- **minSdk**: 29 (Android 10)
- **targetSdk**: 35 (Android 15)
- **compileSdk**: 35

Rationale: Android 15 (API 35) is the current stable release as of early 2026. Targeting it ensures access to the latest platform behaviours and satisfies Google Play's target API level requirements. The min SDK of 29 provides access to scoped storage, `ConnectivityManager.getNetworkCapabilities()`, and all modern Jetpack APIs without compatibility workarounds.

**Important**: The `compileSdk` value in `app/build.gradle.kts` must match this specification (35). Using a higher value (e.g. 36) may produce build warnings with the current Android Gradle Plugin version and is not sanctioned by this architecture.

---

## 3. Architecture Pattern

The app follows **single-activity architecture** with Jetpack Compose Navigation and the **MVVM** (Model-View-ViewModel) pattern, structured into distinct layers.

```
+-----------------+
|   UI Layer      |   Composable screens + ViewModels
+-----------------+
        |
+-----------------+
|  Domain Layer   |   Use cases (thin, optional where logic is trivial)
+-----------------+
        |
+-----------------+
|  Data Layer     |   Repositories, Room DAOs, Retrofit services, cache logic
+-----------------+
```

### Layer responsibilities

- **UI Layer**: Composable screens observe ViewModel state via `StateFlow`. ViewModels invoke repository methods and expose UI state as immutable data classes. All display formatting (rounding, date formatting, locale) happens here.
- **Domain Layer**: Contains use cases only where business logic is non-trivial (e.g. scaling nutrition values to weight, validating completeness of API data, computing rolling summaries with plan history). Simple CRUD flows pass through directly from ViewModel to repository.
- **Data Layer**: Repositories abstract the data sources. Each repository decides whether to read from Room (cache/local data) or the network. Retrofit handles API calls. Room handles persistence.

Key principles:
- ViewModels never hold references to Activities, Fragments, or Compose contexts.
- Room is the single source of truth for persisted data. API results are cached to Room before being surfaced to the UI.
- Unidirectional data flow: UI emits events to ViewModel, ViewModel updates state, UI observes state.

---

## 4. Module Structure

The app uses a single-module architecture. The codebase is not large enough to benefit from multi-module build parallelism, and a single module avoids the overhead of cross-module dependency management for a small team.

### Package Layout

```
com.delve.hungrywalrus/
  di/                  -- Hilt modules (DatabaseModule, NetworkModule, RepositoryModule)
  data/
    local/
      dao/             -- Room DAOs
      entity/          -- Room entity classes
      converter/       -- Room type converters (if needed)
      HungryWalrusDatabase.kt
    remote/
      usda/            -- USDA Retrofit service interface, DTOs, response mapper
      openfoodfacts/   -- Open Food Facts Retrofit service interface, DTOs, response mapper
    repository/        -- Repository implementations
  domain/
    model/             -- Domain models (NutritionPlan, FoodSearchResult, etc.)
    usecase/           -- Use case classes (scaling, validation, summary computation)
  ui/
    navigation/        -- NavHost, route definitions, bottom nav config
    screen/
      dailyprogress/   -- Daily progress screen + ViewModel
      addentry/        -- Meal logging flow screens + ViewModel
      foodsearch/      -- Food search screen (USDA / OFF) + ViewModel
      barcodescan/     -- Barcode scanner screen
      manualentry/     -- Manual food entry screen
      recipes/         -- Recipe list + detail screens + ViewModel
      createrecipe/    -- Recipe creation/editing screen + ViewModel
      summaries/       -- Rolling summary screen (7-day / 28-day tabs) + ViewModel
      settings/        -- USDA API key management + nutrition plan management screen + ViewModel
    component/         -- Shared UI components (progress bars, nutrition cards, confirmation dialogs)
    theme/             -- Material 3 dark theme, colours, typography
  util/                -- Formatting helpers (UK date, number rounding, locale)
  worker/              -- WorkManager workers (DataRetentionWorker)
  HungryWalrusApp.kt   -- Application class (@HiltAndroidApp)
  MainActivity.kt      -- Single activity
```

---

## 5. Database Schema (Room)

### 5.1 Entity-Relationship Diagram

```
+------------------+
|  NutritionPlan   |
+------------------+
| id (PK)          |
| kcalTarget       |
| proteinTargetG   |
| carbsTargetG     |
| fatTargetG       |
| effectiveFrom    |
+------------------+

+------------------+
|    LogEntry      |
+------------------+
| id (PK)          |
| foodName         |
| kcal             |
| proteinG         |
| carbsG           |
| fatG             |
| timestamp        |
+------------------+

+------------------+        1:N        +---------------------+
|     Recipe       | -----------------> |  RecipeIngredient   |
+------------------+                    +---------------------+
| id (PK)          |                    | id (PK)             |
| name             |                    | recipeId (FK)       |
| totalWeightG     |                    | foodName            |
| totalKcal        |                    | weightG             |
| totalProteinG    |                    | kcalPer100g         |
| totalCarbsG      |                    | proteinPer100g      |
| totalFatG        |                    | carbsPer100g        |
| createdAt        |                    | fatPer100g          |
| updatedAt        |                    +---------------------+
+------------------+

+------------------+
|   FoodCache      |
+------------------+
| cacheKey (PK)    |
| foodName         |
| kcalPer100g?     |  -- nullable
| proteinPer100g?  |  -- nullable
| carbsPer100g?    |  -- nullable
| fatPer100g?      |  -- nullable
| source           |
| barcode?         |  -- nullable
| cachedAt         |
+------------------+
```

### 5.2 Entity Definitions

#### NutritionPlan

| Column           | Type    | Notes                                |
|------------------|---------|--------------------------------------|
| id               | Long    | Auto-generated primary key           |
| kcalTarget       | Int     | Daily kilocalorie target             |
| proteinTargetG   | Double  | Daily protein target in grams        |
| carbsTargetG     | Double  | Daily carbohydrate target in grams   |
| fatTargetG       | Double  | Daily fat target in grams            |
| effectiveFrom    | Long    | Epoch millis (UTC)                   |

The current plan is the row with the latest `effectiveFrom` that is not in the future. When a user updates their plan, a new row is inserted with `effectiveFrom = now`. This preserves historical plan data for summary calculations across periods where the plan changed.

**Validation rules**: `kcalTarget` must be greater than zero. `proteinTargetG`, `carbsTargetG`, and `fatTargetG` must be zero or greater. These rules are enforced at the ViewModel level before saving.

#### LogEntry

| Column     | Type    | Notes                                     |
|------------|---------|-------------------------------------------|
| id         | Long    | Auto-generated primary key                |
| foodName   | String  | Display name (food or recipe name)        |
| kcal       | Double  | Final calculated kilocalories             |
| proteinG   | Double  | Final calculated protein in grams         |
| carbsG     | Double  | Final calculated carbohydrates in grams   |
| fatG       | Double  | Final calculated fat in grams             |
| timestamp  | Long    | Epoch millis (UTC) when the entry was created |

Log entries store only final calculated values (already scaled to the consumed weight, or entered directly for manual entries). No foreign key to Recipe or FoodCache -- this makes entries fully self-contained and immune to recipe edits or cache eviction, as required.

**Important**: The consumed weight is not stored in the log entry. For API-sourced and recipe-based entries, weight is used only to scale the per-100g reference values during entry creation. For manual entries, the user provides final values directly and no weight is involved. Once the nutritional values are determined and saved, any intermediate weight input is discarded.

#### Recipe

| Column         | Type    | Notes                          |
|----------------|---------|--------------------------------|
| id             | Long    | Auto-generated primary key     |
| name           | String  | User-given recipe name         |
| totalWeightG   | Double  | Sum of all ingredient weights  |
| totalKcal      | Double  | Derived total kilocalories     |
| totalProteinG  | Double  | Derived total protein          |
| totalCarbsG    | Double  | Derived total carbohydrates    |
| totalFatG      | Double  | Derived total fat              |
| createdAt      | Long    | Epoch millis (UTC)             |
| updatedAt      | Long    | Epoch millis (UTC)             |

The totals are denormalised (pre-computed from ingredients) to avoid recalculating on every read. They are recomputed whenever the recipe is saved or edited.

#### RecipeIngredient

| Column         | Type    | Notes                                   |
|----------------|---------|-----------------------------------------|
| id             | Long    | Auto-generated primary key              |
| recipeId       | Long    | Foreign key to Recipe (CASCADE delete)  |
| foodName       | String  | Ingredient display name                 |
| weightG        | Double  | Weight of this ingredient in grams      |
| kcalPer100g    | Double  | Reference nutrition per 100g            |
| proteinPer100g | Double  | Reference nutrition per 100g            |
| carbsPer100g   | Double  | Reference nutrition per 100g            |
| fatPer100g     | Double  | Reference nutrition per 100g            |

Ingredients store per-100g reference values and weight. This means the recipe can be recalculated if an ingredient weight is changed during editing, without re-fetching from the API.

#### FoodCache

| Column         | Type     | Notes                                        |
|----------------|----------|----------------------------------------------|
| cacheKey       | String   | Primary key. Composite of source and ID.     |
| foodName       | String   | Display name from API                        |
| kcalPer100g    | Double?  | Kilocalories per 100g. Nullable for missing. |
| proteinPer100g | Double?  | Protein per 100g. Nullable for missing.      |
| carbsPer100g   | Double?  | Carbs per 100g. Nullable for missing.        |
| fatPer100g     | Double?  | Fat per 100g. Nullable for missing.          |
| source         | String   | "USDA" or "OFF"                              |
| barcode        | String?  | Nullable. Populated for barcode lookups.     |
| cachedAt       | Long     | Epoch millis (UTC) when cached               |

**Cache key strategy**: For USDA results, the key is `usda:{fdcId}`. For Open Food Facts results, the key is `off:{product_code}`. This ensures uniqueness across sources.

**Cache duration**: 30 days. Food nutrition data changes infrequently. A 30-day window balances freshness against unnecessary network calls and API load.

**Cache eviction**: A daily WorkManager job deletes rows where `cachedAt` is older than 30 days (see Section 14).

**Nullable nutrition fields**: Nutrition values are nullable to faithfully represent incomplete API data. When a cached food has null values, the UI prompts the user to supply estimates before the entry can be saved, matching the requirement for handling incomplete API data.

### 5.3 Key Queries (DAO Methods)

#### NutritionPlanDao

- `getCurrentPlan(now: Long): Flow<NutritionPlan?>` -- returns the plan with the latest `effectiveFrom <= now`.
- `getPlanForDate(date: Long): NutritionPlan?` -- suspend function, not a Flow. Used by summary calculations to get the plan that was active on a specific date.
- `insert(plan: NutritionPlan)` -- save a new plan.

#### LogEntryDao

- `getEntriesForDate(startOfDay: Long, endOfDay: Long): Flow<List<LogEntry>>` -- daily progress view.
- `getEntriesForRange(start: Long, end: Long): Flow<List<LogEntry>>` -- rolling summaries.
- `insert(entry: LogEntry)` -- save a new log entry.
- `deleteById(id: Long)` -- single entry deletion with confirmation.
- `deleteOlderThan(threshold: Long)` -- data retention cleanup (entries older than 2 years).

#### RecipeDao

- `getAll(): Flow<List<Recipe>>` -- recipe list screen.
- `getById(id: Long): Flow<Recipe?>` -- single recipe lookup.
- `insert(recipe: Recipe): Long` -- returns the generated ID.
- `update(recipe: Recipe)` -- update recipe metadata and totals.
- `deleteById(id: Long)` -- user-initiated deletion. Cascades to RecipeIngredient.

#### RecipeIngredientDao

- `getByRecipeId(recipeId: Long): Flow<List<RecipeIngredient>>` -- ingredients for a recipe.
- `insertAll(ingredients: List<RecipeIngredient>)` -- batch insert.
- `deleteByRecipeId(recipeId: Long)` -- used during recipe editing (delete-and-reinsert strategy for simplicity).

#### FoodCacheDao

- `get(cacheKey: String): FoodCache?` -- cache lookup by composite key.
- `getByBarcode(barcode: String): FoodCache?` -- barcode-specific cache lookup.
- `insert(entry: FoodCache)` -- cache a result. Uses `OnConflictStrategy.REPLACE` to update stale entries.
- `deleteOlderThan(threshold: Long)` -- cache eviction.

### 5.4 Database Configuration

- Database name: `hungry_walrus.db`
- Export schema: `true` (for migration tracking and verification).
- Destructive migration: **not allowed**. All future schema changes must include explicit `Migration` objects.
- Initial schema version: 1.
- No type converters are required. All fields are primitives, Strings, or nullable Doubles. Timestamps are stored as `Long` epoch millis to avoid converter complexity.

---

## 6. Repository Layer

### 6.1 Repository Interfaces

#### NutritionPlanRepository

```
getCurrentPlan(): Flow<NutritionPlan?>
getPlanForDate(date: LocalDate): NutritionPlan?
savePlan(kcal: Int, proteinG: Double, carbsG: Double, fatG: Double)
```

#### LogEntryRepository

```
getEntriesForDate(date: LocalDate): Flow<List<LogEntry>>
getEntriesForRange(start: LocalDate, end: LocalDate): Flow<List<LogEntry>>
addEntry(entry: LogEntry)
deleteEntry(id: Long)
```

Date-to-epoch conversion happens inside the repository implementation, keeping the interface clean of epoch millis details.

#### RecipeRepository

```
getAllRecipes(): Flow<List<Recipe>>
getRecipeWithIngredients(id: Long): Flow<RecipeWithIngredients>
saveRecipe(recipe: Recipe, ingredients: List<RecipeIngredient>)
updateRecipe(recipe: Recipe, ingredients: List<RecipeIngredient>)
deleteRecipe(id: Long)
```

`RecipeWithIngredients` is a Room `@Relation` data class that bundles a `Recipe` with its `List<RecipeIngredient>`.

#### FoodLookupRepository

```
searchUsda(query: String): Result<List<FoodSearchResult>>
searchOpenFoodFacts(query: String): Result<List<FoodSearchResult>>
lookupBarcode(barcode: String): Result<FoodSearchResult?>
```

### 6.2 Cache-Then-Network Strategy (FoodLookupRepository)

1. **Barcode lookups**: Check `FoodCacheDao.getByBarcode(barcode)` first. If a valid (non-expired) cached entry exists, return it immediately. Otherwise, query the Open Food Facts API.
2. **Text searches**: Always hit the network. Individual food items from search results are not pre-cached; they are cached when the user selects a specific item and its per-100g data is resolved.
3. **On network success**: Cache each resolved food item in `FoodCache` using `OnConflictStrategy.REPLACE`.
4. **On network failure (IOException / offline)**: Return cached data if available, even if expired. If no cache exists, return `Result.failure()` with a descriptive error. The ViewModel maps this to a UI state that displays a clear error message suggesting manual entry.
5. **On server error (HttpException)**: Do **not** fall back to stale cached data. An `HttpException` indicates the server was reachable, so the offline justification does not apply. Handle specific HTTP status codes as defined in Section 8.4:
   - HTTP 404 on barcode lookup: return `Result.success(null)` (product not found).
   - HTTP 4xx/5xx: return `Result.failure()` with the appropriate user-facing error message.
   This distinction between `IOException` (offline, stale cache acceptable) and `HttpException` (server reachable, stale cache not acceptable) must be maintained consistently.

### 6.3 Domain Models

Domain models are separate from Room entities to keep the UI layer decoupled from persistence details.

```
data class NutritionValues(
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double
)

data class FoodSearchResult(
    val id: String,
    val name: String,
    val source: FoodSource,
    val kcalPer100g: Double?,
    val proteinPer100g: Double?,
    val carbsPer100g: Double?,
    val fatPer100g: Double?,
    val missingFields: Set<NutritionField>
)

enum class FoodSource { USDA, OPEN_FOOD_FACTS, MANUAL }
enum class NutritionField { KCAL, PROTEIN, CARBS, FAT }
```

`missingFields` is derived: any field that is null is included in the set. If the set is non-empty, the UI must prompt the user to supply estimates before saving.

---

## 7. ViewModel and UI Layer Design

### 7.1 ViewModel Conventions

- Each screen or closely related flow has one ViewModel.
- ViewModels are annotated with `@HiltViewModel` and receive dependencies via constructor injection.
- UI state is exposed as a single `StateFlow<ScreenUiState>` per screen. The state is modelled as a sealed interface with substates (Loading, Content, Error) or as a data class with optional error fields.
- One-off events (navigation triggers, snackbar messages) use `Channel<UiEvent>` collected as a `Flow` in the composable.
- The add-entry flow shares a ViewModel scoped to the nested navigation graph to maintain state across the multi-step flow.

### 7.2 ViewModel Inventory

| ViewModel                  | Screens                                         | Responsibilities                                                  |
|----------------------------|--------------------------------------------------|-------------------------------------------------------------------|
| `DailyProgressViewModel`   | Daily Progress                                  | Load today's entries + active plan; compute totals and remaining; handle entry deletion. Must observe the plan as a reactive Flow so that a newly created plan or log entry is reflected immediately without requiring navigation. |
| `AddEntryViewModel`        | Log Method, Food Search, Barcode Scan, Manual Entry, Weight Entry, Missing Values, Entry Confirmation | Manage selected food state across multi-step flow; compute scaled values from weight input; accept direct values for manual entry (no scaling); prompt for missing values; save entry |
| `RecipeListViewModel`      | Recipe List                                     | Load all recipes; handle recipe deletion with confirmation        |
| `RecipeDetailViewModel`    | Recipe Detail                                   | Load single recipe with ingredients                               |
| `CreateRecipeViewModel`    | Create/Edit Recipe                              | Manage ingredient list; compute live running totals; save/update recipe. Must support ingredients sourced from USDA, Open Food Facts searches, barcode scan, and manual entry. |
| `SummariesViewModel`       | Summaries (7-day and 28-day tabs)               | Load entries for rolling periods; compute cumulative totals; load per-day plan targets and sum them for the period; display both cumulative intake and cumulative plan targets. Must reload data each time the screen becomes visible (not use a stale snapshot from a previous visit). |
| `SettingsViewModel`        | Settings                                        | Read/write USDA API key from EncryptedSharedPreferences; load current nutrition plan; validate and save updated plan |

### 7.3 Manual Entry Flow

When the user selects manual entry, the flow is different from API-sourced entries:

1. The user enters a food name and the exact nutritional values (kcal, protein, carbs, fat) as consumed.
2. No weight input is requested. No per-100g scaling is performed.
3. The entered values are stored directly in the `LogEntry` as-is.
4. The confirmation screen still shows the values for review before saving.

This applies both when logging a standalone food item and when adding an ingredient to a recipe. When adding a manual ingredient to a recipe, the user enters the ingredient name, weight, and per-100g nutritional values (since the recipe needs per-100g reference data for proportional scaling of portions).

### 7.4 Display Formatting

All display formatting is handled in the UI layer:

- **Macronutrients** (protein, carbs, fat): rounded to nearest 0.5g. Formula: `Math.round(value * 2.0) / 2.0`. Displayed as e.g. "12.5g" or "30.0g".
- **Kilocalories**: rounded to nearest whole number. Formula: `Math.round(value).toInt()`. Displayed as e.g. "1,250 kcal".
- **Dates**: `dd/MM/yyyy` format using `java.time.format.DateTimeFormatter` with `Locale.UK`.
- **Numbers with thousands**: formatted with comma separator using `Locale.UK` (e.g. "1,250" not "1.250").
- **Theme**: dark mode only. The theme does not include a light colour scheme. Use `darkColorScheme()` exclusively in the Material 3 theme definition.

Stored values in Room retain full `Double` precision to avoid compounding rounding errors across summaries.

### 7.5 Reactivity Requirements

Certain screens must be reactive to data changes occurring elsewhere in the app:

- **DailyProgressViewModel**: The plan observation (`getCurrentPlan()`) must be a continuously collected `Flow`, not a one-shot `.first()` snapshot. This ensures that when a user creates or updates their nutrition plan from Settings, the daily progress screen reflects the change immediately upon returning, without requiring a full screen re-creation.

- **SummariesViewModel**: The summary data must be reloaded each time the summaries screen becomes visible (e.g. when the user switches to the Summaries tab from another tab). Using `.first()` to collect a single snapshot at ViewModel creation time is insufficient -- if the user logs a meal on the Daily Progress tab and then switches to Summaries, the summary must include the newly logged entry. The recommended approach is to trigger `loadSummary()` from a `LaunchedEffect` keyed to the screen's lifecycle or tab selection, so it fires on every visit.

---

## 8. API Integration

### 8.1 USDA FoodData Central

**Base URL**: `https://api.nal.usda.gov/fdc/v1/`

**Authentication**: API key passed as a query parameter (`api_key`).

**Endpoints used**:

| Endpoint         | Method | Purpose                     |
|------------------|--------|-----------------------------|
| `foods/search`   | GET    | Search for foods by keyword |

**Request parameters for search**:
- `query`: Search terms entered by the user.
- `dataType`: `"Foundation,SR Legacy"` -- limits results to generic/natural foods, excluding branded items (branded items come from Open Food Facts instead).
- `pageSize`: 25 -- limits results per request.
- `api_key`: The stored API key.

**Response mapping**: The response contains `foods[]` where each food has `fdcId`, `description`, and `foodNutrients[]`. The mapper extracts nutrients by ID:
- Energy: nutrient ID 1008 (kcal per 100g).
- Protein: nutrient ID 1003 (g per 100g).
- Carbohydrates: nutrient ID 1005 (g per 100g).
- Total fat: nutrient ID 1004 (g per 100g).

If a nutrient is absent from the response, the corresponding field in `FoodSearchResult` is set to `null` and the field is added to `missingFields`.

### 8.2 Open Food Facts

**Base URL**: `https://world.openfoodfacts.org/`

**Authentication**: None required. A `User-Agent` header is required per the API terms of use.

**Endpoints used**:

| Endpoint                     | Method | Purpose                |
|------------------------------|--------|------------------------|
| `cgi/search.pl`              | GET    | Search by product name |
| `api/v2/product/{barcode}`   | GET    | Barcode lookup         |

**Search request parameters**:
- `search_terms`: Query string.
- `search_simple`: 1.
- `action`: "process".
- `json`: 1.
- `page_size`: 25.
- `fields`: `code,product_name,nutriments` -- request only the fields we need.

**Barcode lookup**: The response contains a `product` object with `code`, `product_name`, and `nutriments`.

**Response mapping**: Nutrient keys from the `nutriments` object:
- `energy-kcal_100g` -> kcal per 100g.
- `proteins_100g` -> protein per 100g.
- `carbohydrates_100g` -> carbs per 100g.
- `fat_100g` -> fat per 100g.

Missing nutrient values are mapped to `null`, same as USDA.

### 8.3 Retrofit Configuration

Two separate Retrofit instances (one per API), each with its own `OkHttpClient`:

- **JSON parsing**: Kotlinx Serialization with `retrofit2-kotlinx-serialization-converter`. Chosen over Gson for Kotlin-native null safety, no-reflection performance, and first-party Kotlin support. DTOs use `@Serializable`.
- **Timeouts**: 15 seconds connect, 15 seconds read, 15 seconds write.
- **USDA client**: An OkHttp interceptor reads the API key from `EncryptedSharedPreferences` and appends it as the `api_key` query parameter.
- **Open Food Facts client**: An OkHttp interceptor adds the `User-Agent` header: `HungryWalrus/1.0 (Android; contact@delve.dev)`.
- **Logging**: An `HttpLoggingInterceptor` at `BODY` level is added in debug builds only.

### 8.4 Error Handling

All network calls are wrapped in `try/catch` at the repository level. Errors are categorised:

| Error type         | Handling                                                                                         |
|--------------------|--------------------------------------------------------------------------------------------------|
| No network (IOException) | Check cache first. If no cached result, return `Result.failure(OfflineException)`.        |
| HTTP 400/403       | Return `Result.failure()` with a user-friendly message (e.g. "Invalid API key" for 403).        |
| HTTP 404 (barcode) | Return `Result.success(null)`. UI shows "product not found" with manual entry option.           |
| HTTP 429           | Return `Result.failure()` with a "too many requests, try again later" message.                   |
| HTTP 5xx           | Return `Result.failure()` with a "service temporarily unavailable" message. Do not fall back to stale cached data. |
| Timeout            | Treat as network error (IOException). Same cache-then-error flow.                                |
| Malformed response | Return `Result.failure()` with a "could not read food data" message. Log the parsing exception.  |
| Barcode not found  | For Open Food Facts (status 0 in response): return `Result.success(null)`. UI shows "product not found" with manual entry option. |

**Important distinction**: `IOException` (device is offline or network is unreachable) permits fallback to stale/expired cached data, because no fresher data can be obtained. `HttpException` (server responded with an error) does **not** permit fallback to stale cached data, because the server was reachable and the error may indicate the product no longer exists (404) or a transient server issue (5xx). Serving stale data in these cases would be misleading.

Network connectivity is checked using `ConnectivityManager.getNetworkCapabilities()` (available on API 29+).

The ViewModel translates `Result.failure()` into UI error state. When offline with no cache, the error message explicitly suggests the user enter nutritional values manually.

---

## 9. USDA API Key Storage

### 9.1 Approach: EncryptedSharedPreferences

The USDA API key is stored using **EncryptedSharedPreferences** from `androidx.security:security-crypto`.

**Flow**:
1. On first launch (or if no key is stored), USDA search is disabled in the UI. The settings screen prompts the user to enter their API key.
2. The key is stored in `EncryptedSharedPreferences` using AES-256 encryption backed by the Android Keystore.
3. The key is read at runtime and injected into the USDA OkHttp interceptor.
4. The user can update or clear the key from the settings screen.

**Configuration**:
- Master key: `MasterKey.DEFAULT_MASTER_KEY_ALIAS` with `AES256_GCM` spec.
- Key encryption scheme: `AES256_SIV`.
- Value encryption scheme: `AES256_GCM`.

### 9.2 Trade-offs Considered

| Approach                          | Verdict      | Reasoning                                                              |
|-----------------------------------|--------------|------------------------------------------------------------------------|
| Hardcoded in source               | Rejected     | Exposed in APK. Trivially extractable via decompilation.               |
| BuildConfig / local.properties    | Rejected     | Still compiled into the APK binary. Better than plain text but not secure. |
| Plain SharedPreferences           | Rejected     | Stored in cleartext XML on disk. Exposed via backup extraction.        |
| **EncryptedSharedPreferences**    | **Selected** | Hardware-backed encryption via Android Keystore. Key never in plaintext on disk. User provides their own key, so no shared secret at build time. |
| Android Keystore directly         | Rejected     | More complex API for the same outcome. EncryptedSharedPreferences wraps Keystore internally. |

### 9.3 Resilience

The `security-crypto` library has been in alpha for an extended period but is stable in practice. If decryption fails (e.g. due to Keystore corruption after a device reset), the implementation wraps access in a try-catch, clears the corrupted data, and prompts the user to re-enter their API key. This is acceptable because the USDA API key is freely re-obtainable.

The app is fully functional without a USDA API key -- Open Food Facts search, barcode scanning, and manual entry remain available.

---

## 10. Barcode Scanning

### 10.1 Recommended Library: Google ML Kit Barcode Scanning

**Library**: `com.google.mlkit:barcode-scanning` (bundled model variant).

### 10.2 Rationale

| Library                     | Verdict      | Reasoning                                                                |
|-----------------------------|--------------|--------------------------------------------------------------------------|
| **ML Kit (bundled)**        | **Selected** | Works offline (no network needed for scanning). No Google Play Services dependency. Supports all common 1D/2D barcode formats (EAN-13, EAN-8, UPC-A, UPC-E). Well-maintained by Google. CameraX integration via ImageAnalysis. |
| ML Kit (unbundled/GMS)      | Rejected     | Requires Google Play Services, which may not be present on all devices.  |
| ZXing                       | Rejected     | Largely in maintenance mode. Requires more boilerplate for CameraX integration. Less performant. |
| Dynamsoft                   | Rejected     | Commercial licence. Unnecessary for this use case.                       |

### 10.3 Camera Integration

- Use **CameraX** (`androidx.camera:camera-camera2`, `androidx.camera:camera-lifecycle`, `androidx.camera:camera-view`) for the camera preview.
- The barcode scanner screen uses a `PreviewView` wrapped in an `AndroidView` composable.
- An `ImageAnalysis` use case feeds camera frames to `BarcodeScanner.process()`.
- Camera permission (`android.permission.CAMERA`) is requested at runtime using `rememberLauncherForActivityResult` with `ActivityResultContracts.RequestPermission`.

### 10.4 Scanning Flow

1. User taps "Scan barcode" from the add-entry screen.
2. App requests camera permission if not already granted. If denied, show a message explaining why camera access is needed and offer a "go to settings" link. Barcode scanning is disabled; the user can still search by product name.
3. Camera preview opens with ML Kit analyser attached.
4. On barcode detection, the camera stops and the barcode value is passed to `FoodLookupRepository.lookupBarcode()`.
5. If found, the food details are shown (with prompts for any missing nutritional values).
6. If not found, the user sees a "product not found" message with the option to enter values manually.

---

## 11. Navigation Structure

### 11.1 Navigation Framework

**Jetpack Compose Navigation** (`androidx.navigation:navigation-compose`) with a single `NavHost` in `MainActivity`.

### 11.2 Screen Inventory and Routes

| Route                      | Screen                    | Description                                                  |
|----------------------------|---------------------------|--------------------------------------------------------------|
| `daily_progress`           | Daily Progress            | Home screen. Today's intake vs plan. List of today's entries. Running totals. Start destination. |
| `log/method`               | Log Method Selection      | Choose: recipe, USDA search, OFF search, barcode, manual.   |
| `log/search/{source}`      | Food Search               | Search USDA or OFF. `source` is "usda" or "off".            |
| `log/barcode`              | Barcode Scanner           | Camera viewfinder. Auto-navigates on scan.                   |
| `log/manual`               | Manual Entry              | Enter food name and nutrition values directly. No weight required. |
| `log/recipe_select`        | Recipe Selection          | Pick a saved recipe to log a portion of.                     |
| `log/weight_entry`         | Weight Entry              | Enter weight consumed. Shows scaled nutrition preview. Used for API-sourced and recipe-based entries only. |
| `log/missing_values`       | Missing Values Prompt     | Shown when API data is incomplete. User fills gaps.          |
| `log/confirm`              | Entry Confirmation        | Validation summary. Confirm or go back to edit.              |
| `recipes`                  | Recipe List               | All saved recipes with delete option.                        |
| `recipes/detail/{id}`      | Recipe Detail             | View a recipe and its ingredients. Options to edit or delete.|
| `recipes/create`           | Create Recipe             | Add ingredients, see live running totals.                    |
| `recipes/edit/{id}`        | Edit Recipe               | Edit an existing recipe.                                     |
| `summaries`                | Rolling Summaries         | Tabs for 7-day and 28-day views. Shows both intake and plan targets. |
| `settings`                 | Settings                  | USDA API key management and nutrition plan management.       |

### 11.3 Navigation Flow Diagram

```
                        +------------------+
                        | Daily Progress   |  <-- Start destination
                        +------------------+
                       /         |          \
                      v          v           v
               +----------+ +-------+ +--------+
               | Log      | |Recipes| |Summary |
               | Method   | +-------+ +--------+
               +----------+    |
              / | | \ \        +-> Recipe Detail
             v  v v  v  v          +-> Edit Recipe
  +------+ +---+ +---+ +------+ +--------+
  | USDA | |OFF| |Bar| |Manual| |Recipe  |
  |Search| |Srch| |code| |Entry | |Select  |
  +------+ +---+ +---+ +------+ +--------+
      \      |    |        |          /
       v     v    v        |         v
    +-----------------+    |   +-----------------+
    |  Weight Entry   |    |   |  Weight Entry   |
    +-----------------+    |   +-----------------+
            |              |          |
   (if missing values)     |          |
            v              |          |
    +-----------------+    |          |
    | Missing Values  |    |          |
    +-----------------+    |          |
            |              |          |
            v              v          v
    +-------------------------------------------+
    |          Entry Confirmation                |
    +-------------------------------------------+
                        |
                        v
    +-------------------------------------------+
    |       Daily Progress (pop back)           |
    +-------------------------------------------+

Bottom Navigation:
  [Daily Progress]  [Recipes]  [Summaries]  [Settings]
```

Note: Manual entry skips the Weight Entry step and proceeds directly to Entry Confirmation after the user enters the food name and nutritional values.

### 11.4 Bottom Navigation

A bottom navigation bar is present on the four top-level destinations:
- **Daily Progress** (home icon) -- default selected
- **Recipes** (book icon)
- **Summaries** (chart icon)
- **Settings** (gear icon)

The nutrition plan is managed within the Settings screen. The meal logging flow (`log/*`) is a nested navigation graph that hides the bottom bar, providing a focused multi-step experience.

---

## 12. Nutrition Value Scaling

### 12.1 Scaling from Per-100g Reference

When a user enters a weight for a food item sourced from an API:

```
scaledValue = (valuePer100g / 100.0) * weightG
```

### 12.2 Scaling from a Recipe Portion

When a user enters a weight for a recipe portion:

```
scaledValue = (recipeTotalValue / recipeTotalWeightG) * portionWeightG
```

### 12.3 Manual Entry (No Scaling)

When a user manually enters nutritional values, no scaling is performed. The entered values are the final values stored in the `LogEntry`. The user is responsible for entering the values as consumed.

### 12.4 Storage

The final scaled values (or directly entered values for manual entries) are stored in the `LogEntry`. The per-100g reference values remain in `FoodCache` (for API-sourced items) and `RecipeIngredient` (for recipe ingredients). The consumed weight is not stored in the log entry.

---

## 13. Dependency Injection: Hilt

### 13.1 Choice and Rationale

**Hilt** (Dagger-Hilt) is the dependency injection framework.

| Framework       | Verdict      | Reasoning                                                                   |
|-----------------|--------------|-----------------------------------------------------------------------------|
| **Hilt**        | **Selected** | First-party Jetpack integration. Built-in `@HiltViewModel` eliminates factory boilerplate. Compile-time DI graph validation catches wiring errors at build time. Native WorkManager integration via `@HiltWorker`. Well-documented for single-module apps. |
| Koin            | Rejected     | Runtime resolution means DI errors surface at runtime, not compile time. Simpler setup, but the safety trade-off is not justified.               |
| Manual DI       | Rejected     | Feasible for a small app but grows tedious as dependencies increase. No compile-time graph validation. No lifecycle-aware scoping.                |

### 13.2 Hilt Modules

**DatabaseModule** (`@InstallIn(SingletonComponent::class)`):
- Provides the Room `HungryWalrusDatabase` instance as a singleton.
- Provides each DAO from the database instance. All DAO provider methods must be annotated `@Singleton` to be consistent with the rest of the DI configuration and avoid unnecessary object creation.

**NetworkModule** (`@InstallIn(SingletonComponent::class)`):
- Provides the USDA `OkHttpClient` and `Retrofit` instance.
- Provides the Open Food Facts `OkHttpClient` and `Retrofit` instance.
- Provides each API service interface.
- Uses `@Named` qualifiers to distinguish USDA vs OFF Retrofit instances.

**RepositoryModule** (`@InstallIn(SingletonComponent::class)`):
- Binds repository interfaces to their implementations.

**WorkerModule**:
- `@HiltWorker` annotation on `DataRetentionWorker` enables assisted injection of `WorkerParameters` and dependencies.

---

## 14. Background Work: Data Retention

### 14.1 WorkManager Configuration

A single `PeriodicWorkRequest` registered from `HungryWalrusApp.onCreate()`:

- **Worker class**: `DataRetentionWorker` extending `CoroutineWorker`, annotated with `@HiltWorker`.
- **Interval**: 24 hours.
- **Initial delay**: 1 hour (avoid running immediately on every app launch).
- **Constraints**: None. The cleanup queries are lightweight database operations.
- **Enqueue policy**: `ExistingPeriodicWorkPolicy.KEEP` -- do not replace if already scheduled.

### 14.2 Tasks Performed

1. Delete `LogEntry` rows where `timestamp < (now - 730 days)` (2 years).
2. Delete `FoodCache` rows where `cachedAt < (now - 30 days)`.

Both operations are idempotent. Retry with default exponential backoff is safe.

The worker must **never** delete recipes. Recipes are retained indefinitely per the requirements and are only removed via explicit user action.

---

## 15. Key Libraries and Versions

| Library                              | Version         | Purpose                          |
|--------------------------------------|-----------------|----------------------------------|
| Kotlin                               | 2.1.x           | Language                         |
| Compose BOM                          | 2025.02.00+     | UI framework (aligned versions)  |
| Compose Compiler                     | (Kotlin plugin) | Integrated into Kotlin 2.x       |
| Navigation Compose                   | 2.8.x           | Screen navigation                |
| Room                                 | 2.7.x           | Local database                   |
| Hilt                                 | 2.53.x          | Dependency injection             |
| hilt-navigation-compose              | 1.2.x           | ViewModel injection in Compose   |
| Retrofit                             | 2.11.x          | HTTP client                      |
| OkHttp                               | 4.12.x          | HTTP transport + interceptors    |
| Kotlinx Serialization                | 1.7.x           | JSON serialisation               |
| retrofit2-kotlinx-serialization-converter | 1.0.0      | Retrofit + Kotlinx Serialization |
| ML Kit Barcode Scanning              | 17.3.x          | Barcode scanning (bundled model) |
| CameraX                              | 1.4.x           | Camera preview for scanning      |
| WorkManager                          | 2.10.x          | Background data retention tasks  |
| security-crypto                      | 1.1.0-alpha06   | EncryptedSharedPreferences       |
| Kotlin Coroutines                    | 1.9.x           | Async operations                 |
| Material 3                           | Via Compose BOM | Dark theme UI components         |
| KSP                                  | Matches Kotlin  | Annotation processing (Room, Hilt) |
| Android Gradle Plugin                | 8.7.x           | Build tooling                    |
| JDK                                  | 17              | Build environment                |

---

## 16. Permissions

| Permission                   | Type          | Purpose                                    |
|------------------------------|---------------|--------------------------------------------|
| `android.permission.CAMERA`  | Runtime       | Barcode scanning via CameraX + ML Kit      |
| `android.permission.INTERNET`| Manifest only | API calls to USDA and Open Food Facts      |

No other permissions are needed. The app does not access location, contacts, storage, microphone, or any other sensitive data.

---

## 17. Technical Risks and Trade-offs

### 17.1 Single-module vs multi-module

**Decision**: Single module.
**Risk**: As the app grows, build times increase.
**Mitigation**: The package structure already mirrors a modular layout. Migration to multi-module is straightforward if needed. For the current feature set, build times are negligible.

### 17.2 API rate limits

**Risk**: USDA FoodData Central has a rate limit of 1,000 requests per hour per API key. Open Food Facts requests responsible usage.
**Mitigation**: The 30-day food cache significantly reduces repeat lookups. Search input debouncing (300ms delay before firing API call) prevents excessive requests during typing. Results are limited to 25 per query.

### 17.3 EncryptedSharedPreferences stability

**Risk**: The `security-crypto` library remains in alpha. On some devices, Android Keystore corruption can cause unrecoverable exceptions.
**Mitigation**: Access is wrapped in try-catch. On failure, the stored key is cleared and the user is prompted to re-enter it. The stored value is a free API key, not highly sensitive credentials. The risk is low and the recovery path is simple.

### 17.4 Open Food Facts data quality

**Risk**: Open Food Facts is community-contributed. Nutrition data may be missing, incorrect, or inconsistent.
**Mitigation**: The missing-value prompt requirement ensures the user always reviews and completes nutrition data before saving. The app does not blindly trust API data.

### 17.5 Camera permission denial

**Risk**: User permanently denies camera permission, breaking barcode scanning.
**Mitigation**: Show a clear message explaining why camera access is needed and offer a "go to settings" deep link. The user can always search by product name as an alternative. The app is fully functional without camera access.

### 17.6 No log entry editing

**Risk**: Users who make mistakes must delete and re-create entries.
**Mitigation**: This is explicitly out of scope for v1. The entry confirmation screen (review before save) reduces the frequency of errors. The data model (flat `LogEntry` with no foreign keys) does not preclude adding edit functionality in a future version.

### 17.7 Plan history for summary calculations

**Risk**: If the user changes their plan mid-period, the 7-day and 28-day summaries need to account for different targets on different days.
**Mitigation**: The `NutritionPlan` table stores `effectiveFrom` dates. The summary calculation queries the plan that was effective on each day within the period and sums per-day targets to produce the period total target. The `getPlanForDate()` DAO method supports this.

### 17.8 Database migration strategy

**Risk**: Schema changes in future versions require Room migrations. Incorrect migrations can cause data loss.
**Mitigation**: Export Room schemas (`exportSchema = true`) for verification. Write explicit `Migration` objects for every schema change. Destructive migration is never allowed. The initial schema is kept simple to minimise early migration needs.

### 17.9 Offline-first reliability

**Risk**: Users in areas with poor connectivity may be unable to look up foods.
**Mitigation**: Cached results cover previously looked-up foods for 30 days. The manual entry flow is always available regardless of connectivity. Error messages clearly direct users to manual entry when offline and no cached result exists.

---

## 18. Formatting Conventions

As specified in the requirements and CLAUDE.md:

| Convention              | Format                              | Implementation                     |
|-------------------------|-------------------------------------|------------------------------------|
| Date display            | dd/MM/yyyy (e.g. 19/03/2026)       | `DateTimeFormatter` with `Locale.UK` |
| Thousands separator     | Comma (e.g. 1,250)                 | `NumberFormat` with `Locale.UK`    |
| Weight unit             | Grams (g)                          | Suffix "g"                         |
| Energy unit             | Kilocalories (kcal)                | Suffix "kcal"                      |
| Language                | English only                       | No i18n infrastructure             |
| Macronutrient rounding  | Nearest 0.5g                       | `Math.round(v * 2.0) / 2.0`       |
| Kilocalorie rounding    | Nearest whole number               | `Math.round(v).toInt()`            |

A `Formatter` utility object centralises all formatting logic to ensure consistency across the app.

---

## 19. Testing Strategy (Architectural Guidance)

While test implementation details are outside the scope of this document, the architecture supports testability:

- **Repositories** are defined as interfaces, enabling fake implementations in ViewModel tests.
- **ViewModels** receive dependencies via constructor injection, enabling unit testing with fakes.
- **DAOs** can be tested using Room's in-memory database builder. The `room-testing` dependency is included for this purpose. In-memory database tests are recommended for verifying SQL query correctness, particularly day-boundary queries in `LogEntryDao` and the plan-for-date ordering query in `NutritionPlanDao`.
- **API services** can be tested using `MockWebServer` (OkHttp).
- **Use cases** are pure functions/classes with no Android dependencies, trivially unit testable.
- **Mappers** (DTO to domain) are pure functions, trivially unit testable.

---

## 20. Revision History

### Revision 1 -- 2026-03-22

Amendments based on QA report (2026-03-21), data layer code review (Session 01, Round 3), and UI layer code review (Session 03, Pass 13, including Product Owner findings P01--P07).

#### Requirements changes

1. **Manual entry no longer requires weight input (P04).** The requirements previously stated "All food entries require a weight input from the user." This has been revised: API-sourced and recipe-based entries still require weight for scaling, but manual entries accept final nutritional values directly without a weight input. The user enters the exact values consumed. This change is reflected in the Meal Logging section of requirements.md and in Sections 7.3, 11.2, 11.3, and 12.3 of this architecture document.

2. **Nutrition plan managed from Settings screen, not a dedicated screen (P03).** The `plan` route has been removed from the navigation structure. The nutrition plan view/edit functionality is consolidated into the Settings screen. The `PlanViewModel` is merged into `SettingsViewModel`. The bottom navigation no longer needs a separate entry point for the plan -- it was already not in the bottom nav (it was accessed from Daily Progress), but now it is explicitly part of Settings.

3. **Daily progress must show running totals (P05).** The requirements already specified "Displays the current day's total intake versus the plan" but this has been made more explicit: the daily progress screen must show a running total of kcal, protein, carbs, and fat consumed today. This was present in the architecture but the QA report found it missing from the implementation.

4. **Summaries must display plan target values alongside intake (P06).** The requirements for rolling summaries already mentioned "cumulative plan targets" but this has been emphasised. The architecture now explicitly states that `SummariesViewModel` must display both cumulative intake and cumulative plan targets.

5. **Summaries must refresh on each visit (P07, Bug 5).** The requirements now explicitly state that summary data must refresh when the user navigates to the summaries screen. The architecture adds Section 7.5 (Reactivity Requirements) specifying that `SummariesViewModel` must reload data on each screen visit rather than using a stale `.first()` snapshot.

6. **"No plan" notice must update reactively (P01).** The requirements now specify that the "no plan" notice on the daily progress screen must disappear immediately when a plan is saved, without requiring navigation. The architecture adds a reactivity requirement for `DailyProgressViewModel` in Section 7.5.

7. **Plan validation rules clarified (Bug 3).** The requirements now explicitly state that kcal targets must be greater than zero, while macronutrient targets must be zero or greater. This resolves the ambiguity found by QA where the asymmetric validation (kcal rejects zero, macros accept zero) was undocumented.

#### Architecture changes

8. **compileSdk enforced as 35 (Bug 2).** Added an explicit note in Section 2 that the `compileSdk` in `app/build.gradle.kts` must match the architecture specification of 35. The implementation had diverged to 36, producing AGP compatibility warnings.

9. **HttpException vs IOException cache fallback clarified (m1 from data layer code review).** Section 6.2 now distinguishes between `IOException` (offline -- stale cache fallback permitted) and `HttpException` (server reachable -- stale cache fallback not permitted). Section 8.4 has been updated with the same distinction. Previously, the architecture described "network failure" generically without distinguishing these cases, leading to the implementation incorrectly falling back to stale cache on `HttpException`.

10. **DAO providers must be @Singleton scoped (m2 from data layer code review).** Section 13.2 now explicitly states that all DAO provider methods in `DatabaseModule` must be annotated `@Singleton`.

11. **Recipe ingredient flow must support Open Food Facts sources (P02).** The `CreateRecipeViewModel` entry in Section 7.2 now explicitly states it must support ingredients sourced from both USDA and Open Food Facts. The QA report found that adding branded foods (from OFF) to a recipe was broken -- the weight entry step did not function for OFF-sourced ingredients.

12. **DataRetentionWorker recipe non-deletion contract documented (Bug 4).** Section 14.2 now explicitly states that the worker must never delete recipes. While the implementation was already correct (no `RecipeDao` injection), this contract was not documented in the architecture, leaving it vulnerable to accidental regression.

13. **In-memory Room database testing recommended.** Section 19 now recommends in-memory Room database tests for SQL query correctness, particularly for day-boundary and plan-ordering queries, based on the QA coverage assessment.
