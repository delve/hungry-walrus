# Developer Notes: Session 00 -- Project Scaffolding

## What was built

Complete Android project scaffolding for the Hungry Walrus app under `./app/`.

### Gradle configuration
- `build.gradle.kts` (root) -- plugin declarations using version catalog aliases
- `app/build.gradle.kts` -- full dependency list matching architecture section 15, with KSP for Room and Hilt, Compose BOM, Retrofit, CameraX, ML Kit, WorkManager, security-crypto, and all test dependencies
- `settings.gradle.kts` -- plugin management and dependency resolution with Google and Maven Central repositories
- `gradle.properties` -- JVM args, AndroidX, parallel builds, non-transitive R classes
- `gradle/libs.versions.toml` -- complete version catalog with all libraries, plugins, and version references
- `gradle/wrapper/` -- Gradle 8.11.1 wrapper (JAR, properties, gradlew script)

### Project structure
Full package directory tree created matching architecture section 4:
- `com.delve.hungrywalrus.di/` -- DatabaseModule (fully implemented), NetworkModule (stub), RepositoryModule (stub)
- `com.delve.hungrywalrus.data.local/` -- HungryWalrusDatabase, all 5 entities, all 5 DAOs (fully implemented with queries from architecture section 5.3), RecipeWithIngredients @Relation class
- `com.delve.hungrywalrus.data.local.converter/` -- empty package (no converters needed per architecture)
- `com.delve.hungrywalrus.data.remote.usda/` -- placeholder
- `com.delve.hungrywalrus.data.remote.openfoodfacts/` -- placeholder
- `com.delve.hungrywalrus.data.repository/` -- placeholder
- `com.delve.hungrywalrus.domain.model/` -- NutritionValues, FoodSearchResult, FoodSource, NutritionField (fully implemented)
- `com.delve.hungrywalrus.domain.usecase/` -- placeholder
- `com.delve.hungrywalrus.ui.navigation/` -- Routes, BottomNavItem, HungryWalrusNavHost (all implemented with placeholder screens)
- `com.delve.hungrywalrus.ui.screen/{dailyprogress,addentry,foodsearch,barcodescan,manualentry,recipes,createrecipe,plan,summaries,settings}/` -- placeholders
- `com.delve.hungrywalrus.ui.component/` -- placeholder
- `com.delve.hungrywalrus.ui.theme/` -- Color.kt, Type.kt, Theme.kt, Spacing.kt (fully implemented)
- `com.delve.hungrywalrus.util/` -- Formatter.kt (fully implemented)
- `com.delve.hungrywalrus.worker/` -- DataRetentionWorker (fully implemented)

### AndroidManifest.xml
- INTERNET permission (manifest-only)
- CAMERA permission (runtime)
- Camera hardware feature declared as not required
- Application entry point: HungryWalrusApp
- Single activity: MainActivity

### Application class (HungryWalrusApp.kt)
- @HiltAndroidApp annotation
- Implements Configuration.Provider for WorkManager + HiltWorkerFactory
- Schedules DataRetentionWorker (24h periodic, 1h initial delay, KEEP policy)

### MainActivity
- @AndroidEntryPoint annotation
- Edge-to-edge enabled
- Scaffold with NavigationBar (bottom bar)
- Bottom bar visible only on top-level destinations (Today, Recipes, Summaries, Settings)
- Bottom bar hidden during log flow and recipe create/edit (per design spec section 2.1)

### Theme
- Dark-only color scheme using darkColorScheme() with all tokens from design spec section 1.1
- Custom semantic colours for progress bars (ProgressKcal, ProgressProtein, ProgressCarbs, ProgressFat, ProgressTrack, Overage)
- Typography scale matching design spec section 1.2
- Spacing tokens matching design spec section 1.3

### DI modules
- DatabaseModule: fully implemented (provides database singleton and all 5 DAOs)
- NetworkModule: stub with TODO comments describing what to implement
- RepositoryModule: stub with TODO comments

### Room database
- HungryWalrusDatabase with all 5 entities registered
- Schema version 1, exportSchema = true
- Schema directory configured at `$projectDir/schemas`
- All 5 DAOs with complete method signatures and Room queries
- RecipeIngredientEntity has ForeignKey to RecipeEntity with CASCADE delete and index on recipeId
- FoodCacheDao uses OnConflictStrategy.REPLACE for insert

### ProGuard/R8 rules
- Room, Retrofit, OkHttp, Kotlinx Serialization, Hilt, ML Kit rules

### local.properties
- Points to local Android SDK
- Documents that USDA API key is stored at runtime via EncryptedSharedPreferences, not in this file

## Deviations from architecture document

### compileSdk 36 instead of 35
The architecture specifies compileSdk 35 (Android 15). However, the build environment only has Android SDK platform 36.1 installed, and no sdkmanager is available to install platform 35. Using compileSdk 36 with targetSdk 35 is fully compatible -- compileSdk only affects which APIs are visible at compile time, while targetSdk controls runtime behaviour. This is a non-breaking deviation. A future session can adjust this if platform 35 becomes available.

### AGP version 8.7.3
The architecture specifies AGP 8.7.x. The version catalog uses 8.7.3, which is the latest patch in the 8.7 series.

### Specific patch versions chosen
Where the architecture specified `x` minor versions (e.g. "2.1.x", "2.8.x"), specific versions were chosen based on current latest stable releases. All are within the specified ranges.

## Known gaps / what next sessions should tackle

1. **Data layer (repositories)** -- Repository interfaces and implementations need to be created in `data/repository/`. The RepositoryModule needs @Binds methods.
2. **Network layer** -- USDA and Open Food Facts Retrofit service interfaces, DTOs, response mappers, and the NetworkModule @Provides methods.
3. **Domain layer (use cases)** -- ScaleNutritionUseCase, ComputeRollingSummaryUseCase, ValidateFoodDataUseCase.
4. **UI screens** -- All screen composables and ViewModels are placeholders.
5. **Shared UI components** -- NutritionProgressBar, NutritionCard, ConfirmationDialog, etc.
6. **EncryptedSharedPreferences wrapper** -- For USDA API key storage.
7. **ConnectivityManager helper** -- For checking network availability.
8. **Launcher icon** -- Currently a placeholder circle. Needs a proper app icon.

## Unit tests written

4 test files, all passing:

1. **FormatterTest** (9 tests) -- Covers date formatting (UK dd/MM/yyyy), kcal formatting (rounding, comma separator), macro formatting (0.5g rounding), and numeric rounding functions.
2. **RoutesTest** (5 tests) -- Verifies parameterised route builders (logSearch, recipeDetail, recipeEdit) and start destination constant.
3. **BottomNavItemTest** (3 tests) -- Verifies correct count (4 items), correct routes, and correct labels for bottom navigation items.
4. **FoodSearchResultTest** (4 tests) -- Verifies domain model data classes, enum values for FoodSource (3 values) and NutritionField (4 values), and missingFields semantics.

## Build verification

- `./gradlew assembleDebug` -- BUILD SUCCESSFUL
- `./gradlew testDebugUnitTest` -- BUILD SUCCESSFUL, 21 tests, 0 failures
- Room schema exported to `app/schemas/` (version 1)
- No compilation warnings (after fixing deprecated MenuBook icon)
