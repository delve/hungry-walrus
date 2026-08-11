# UI/UX Design Specification: Hungry Walrus

## 1. Design Foundations

### 1.1 Theme

Dark mode only. No light colour scheme exists. The Material 3 theme uses `darkColorScheme()` exclusively.

#### Colour Palette (Material 3 Dark Tokens)

| Token                    | Value       | Usage                                         |
|--------------------------|-------------|-----------------------------------------------|
| `background`             | #0E0E0E     | App background, scaffold                      |
| `surface`                | #1A1A1A     | Cards, sheets, dialogs                        |
| `surfaceVariant`         | #242424     | Secondary containers, input fields            |
| `surfaceContainerHigh`   | #2C2C2C     | Elevated surfaces, bottom nav                 |
| `primary`                | #8CB4F0     | Primary actions, active indicators, FAB       |
| `onPrimary`              | #0E0E0E     | Text/icons on primary colour                  |
| `onBackground`           | #E2E2E2     | Primary body text                             |
| `onSurface`              | #E2E2E2     | Text on surface                               |
| `onSurfaceVariant`       | #A0A0A0     | Secondary/label text                          |
| `outline`                | #444444     | Dividers, borders                             |
| `outlineVariant`         | #333333     | Subtle dividers                               |
| `error`                  | #F2837A     | Error text, overage indicators                |
| `onError`                | #0E0E0E     | Text on error colour                          |
| `tertiary`               | #8CD4A0     | Positive indicators (under target/on track)   |
| `secondaryContainer`     | #2A2A3A     | Selected states, chips                        |

#### Semantic Colours (Custom Tokens)

| Token                 | Value       | Usage                                            |
|-----------------------|-------------|--------------------------------------------------|
| `progressKcal`        | #8CB4F0     | Kilocalorie progress bar fill                    |
| `progressProtein`     | #F0C874     | Protein progress bar fill                        |
| `progressCarbs`       | #8CD4A0     | Carbohydrate progress bar fill                   |
| `progressFat`         | #D4A0D4     | Fat progress bar fill                            |
| `progressTrack`       | #2A2A2A     | Unfilled portion of all progress bars            |
| `overage`             | #F2837A     | Any value exceeding its target                   |

### 1.2 Typography

Material 3 default type scale. All weights use the system default sans-serif (Roboto on most devices).

| Style               | Size  | Weight   | Usage                                        |
|----------------------|-------|----------|----------------------------------------------|
| `displaySmall`       | 36sp  | Normal   | Not used                                     |
| `headlineLarge`      | 32sp  | Normal   | Not used                                     |
| `headlineMedium`     | 28sp  | Normal   | Not used                                     |
| `headlineSmall`      | 24sp  | Normal   | Screen titles in top bar                     |
| `titleLarge`         | 22sp  | Medium   | Confirmation screen nutrient values          |
| `titleMedium`        | 16sp  | Medium   | Section headers, card titles                 |
| `titleSmall`         | 14sp  | Medium   | List item primary text                       |
| `bodyLarge`          | 16sp  | Normal   | Input fields, primary content text           |
| `bodyMedium`         | 14sp  | Normal   | General body text, descriptions              |
| `bodySmall`          | 12sp  | Normal   | Captions, timestamps                         |
| `labelLarge`         | 14sp  | Medium   | Button text, tab labels                      |
| `labelMedium`        | 12sp  | Medium   | Chip text, small labels                      |
| `labelSmall`         | 11sp  | Medium   | Overline text, progress bar labels           |

### 1.3 Spacing System

Base unit: 4dp. All spacing uses multiples of this unit.

| Token    | Value | Usage                                                  |
|----------|-------|--------------------------------------------------------|
| `xs`     | 4dp   | Minimum spacing between dense inline elements          |
| `sm`     | 8dp   | Spacing between related elements within a group        |
| `md`     | 12dp  | Card internal padding, list item vertical padding      |
| `lg`     | 16dp  | Screen horizontal padding, section gaps                |
| `xl`     | 24dp  | Major section separation                               |

Card corner radius: 8dp. Button corner radius: 8dp. Input field corner radius: 4dp.

Per the design principle of high information density, padding is kept to the minimum that maintains visual separation and touch target compliance. Touch targets are minimum 48dp as per Material accessibility guidelines.

### 1.4 Formatting Conventions

All formatting is handled in the UI layer via a shared `Formatter` utility. Reference: architecture document section 18.

| Data type          | Format                  | Example           |
|--------------------|-------------------------|-------------------|
| Dates              | dd/MM/yyyy              | 19/03/2026        |
| Kilocalories       | Nearest whole, comma sep| 1,250 kcal        |
| Macronutrients     | Nearest 0.5g            | 12.5g             |
| Thousands          | Comma separator (UK)    | 2,500             |
| Weight             | Grams                   | 150g              |
| Energy unit        | kcal                    | kcal              |

---

## 2. Navigation Structure

### 2.1 Bottom Navigation Bar

The bottom navigation bar is visible on the four top-level destinations. It uses a Material 3 `NavigationBar` component rendered on `surfaceContainerHigh`.

| Position | Label      | Icon (Material Icons) | Route              |
|----------|------------|-----------------------|--------------------|
| 1        | Today      | `CalendarToday`       | `daily_progress`   |
| 2        | Recipes    | `MenuBook`            | `recipes`          |
| 3        | Summaries  | `BarChart`            | `summaries`        |
| 4        | Settings   | `Settings`            | `settings`         |

The bottom bar is **hidden** during the entire meal logging flow (`log/*` routes) and during recipe creation/editing (`recipes/create`, `recipes/edit/{id}`). These flows use the nested navigation graph described in the architecture (section 11).

Selected tab: `primary` colour icon and label. Unselected tabs: `onSurfaceVariant` colour.

### 2.2 Top App Bar

Each screen has a `TopAppBar` (Material 3 small top app bar, `surface` background):
- Top-level destinations: title text only, no back arrow.
- Sub-screens: back arrow (leading navigation icon) that pops the back stack.
- The meal logging flow screens show a close (X) icon instead of a back arrow. Tapping X shows a discard confirmation dialog ("Discard this entry?") and pops the entire `log/*` nested graph back to `daily_progress`.

### 2.3 Back Behaviour

| Context                                  | Back action                                    |
|------------------------------------------|------------------------------------------------|
| Top-level tab screen                     | System default (exit app or go to launcher)    |
| Sub-screen within a tab                  | Pop to parent tab screen                       |
| Meal logging flow (any step)             | Pop to previous step within the flow           |
| Meal logging flow (first step: method)   | Pop entire nested graph, return to daily_progress |
| Entry confirmation -> back               | Pop to weight entry (or manual entry)          |
| Recipe create/edit                       | Discard confirmation dialog, then pop          |

---

## 3. Screen Specifications

### 3.1 Daily Progress (`daily_progress`)

**Purpose**: Home screen. Shows today's nutritional intake versus the active plan, with a list of today's log entries and a prominent entry point for meal logging.

**ViewModel**: `DailyProgressViewModel`

**Layout**:

```
+----------------------------------------------+
| TopAppBar: "Today" | date (dd/MM/yyyy)       |
+----------------------------------------------+
| [Plan targets banner]                         |
|  Kcal: 1,250 / 2,000  Remaining: 750        |
|  [=========>                    ] kcal bar   |
|                                               |
|  Protein  45.0 / 150.0g                      |
|  [========>                     ] protein bar|
|                                               |
|  Carbs  120.0 / 250.0g                       |
|  [=============>                ] carbs bar  |
|                                               |
|  Fat  30.0 / 65.0g                           |
|  [=========>                    ] fat bar    |
+----------------------------------------------+
| Log entries (scrollable list)                 |
|  +------------------------------------------+|
|  | Chicken breast     320 kcal    [Delete]  ||
|  | P: 35.0g  C: 0.0g  F: 8.5g              ||
|  | 12:34                                     ||
|  +------------------------------------------+|
|  | Banana              89 kcal    [Delete]  ||
|  | P: 1.0g  C: 23.0g  F: 0.5g              ||
|  | 08:15                                     ||
|  +------------------------------------------+|
|  ...                                          |
+----------------------------------------------+
| [+ Log food] FAB                              |
+----------------------------------------------+
| Bottom Navigation Bar                         |
+----------------------------------------------+
```

**Elements**:

1. **Top app bar**: Title "Today" left-aligned. Current date displayed right-aligned in `bodySmall` / `onSurfaceVariant`. A text button "Plan" in `primary` colour at the trailing edge navigates to `settings`.

2. **Progress summary section** (non-scrollable, pinned above the list):
   - **Kilocalories row**: A single horizontal `LinearProgressIndicator` spanning full width. Fill colour: `progressKcal`. Track: `progressTrack`. Above the bar: left-aligned "X / Y kcal" in `titleMedium`, right-aligned "Remaining: Z kcal" in `bodyMedium` / `onSurfaceVariant`. If intake exceeds target, the remaining text changes to "Over: Z kcal" in `overage` colour, and the progress bar fill uses `overage` colour for the portion exceeding 100%.
   - **Macro rows**: Three separate full-width rows, one for each macro (Protein, Carbs, Fat), stacked vertically with `8dp` spacing between them. Each row contains: a label in `labelSmall` / `onSurfaceVariant` and value "X / Yg" in `bodyMedium` on the same line (label left-aligned, value right-aligned), followed below by a `LinearProgressIndicator` spanning full width with its respective semantic colour. Progress bars clamp visually at 100% but the numeric value shows the true amount.
   - If no plan is configured, this section shows a card: "No nutrition plan set. Tap to configure." The card is tappable and navigates to `settings`.

3. **Log entries list**: `LazyColumn` filling the remaining vertical space. Each item is a `Card` on `surface`:
   - Left side: food name in `titleSmall`, macro values below in `bodySmall` / `onSurfaceVariant` formatted as "P: Xg  C: Xg  F: Xg".
   - Right side: kilocalories in `titleSmall`, timestamp (HH:mm) in `bodySmall` / `onSurfaceVariant` below.
   - A trailing `IconButton` with `Delete` (trash) icon in `onSurfaceVariant`. Tapping it shows a confirmation dialog (see below).
   - Items are ordered by timestamp descending (most recent first).

4. **Delete confirmation dialog**: Material 3 `AlertDialog`. Title: "Delete entry?". Body: "{foodName} -- {kcal} kcal". Two buttons: "Cancel" (text button) and "Delete" (text button, `error` colour). On confirm, the entry is deleted via `DailyProgressViewModel` and the list updates reactively.

5. **FAB**: `FloatingActionButton` at bottom-end position, `primary` colour. Icon: `Add`. Label: none (icon only to save space). Tapping navigates to `log/method`.

**States**:

| State    | Behaviour                                                                 |
|----------|---------------------------------------------------------------------------|
| Loading  | Progress summary shows placeholder shimmer. List area shows `CircularProgressIndicator` centred. |
| Empty (no entries, plan exists) | Progress bars all at 0%. List area shows centred text: "No entries today. Tap + to log food." in `onSurfaceVariant`. |
| Empty (no plan) | Progress section replaced with the "No nutrition plan set" card. List shows same empty text. |
| Populated | As described above. |
| Error    | Not applicable -- data is from local Room database. If database read fails (extremely unlikely), show a full-screen error: "Could not load data. Please restart the app." |

---

### 3.2 Nutrition Plan

> **Removed.** The dedicated `plan` route and screen have been removed (architecture change P03). Nutrition plan management has been consolidated into the Settings screen. See Section 3.15.

---

### 3.3 Log Method Selection (`log/method`)

**Purpose**: Entry point for the meal logging flow. User chooses how they want to find or enter food data.

**ViewModel**: `AddEntryViewModel` (shared across the logging flow, scoped to the nested `log` navigation graph).

**Route**: `log/method`

**Layout**:

```
+----------------------------------------------+
| TopAppBar: X "Log Food"                      |
+----------------------------------------------+
| [  Search generic foods (USDA)            >] |
| [  Search branded products (OFF)          >] |
| [  Scan barcode                           >] |
| [  Enter manually                         >] |
| [  Log from recipe                        >] |
+----------------------------------------------+
```

**Elements**:

1. **Top app bar**: Close (X) icon at leading position. Tapping it pops the entire log flow back to `daily_progress` (no confirmation needed since no data has been entered yet). Title: "Log Food".

2. **Method list**: Five full-width list items, each a `Surface` on `surface` with `md` vertical padding. Each contains:
   - Leading icon in `onSurfaceVariant`: `Search` for USDA, `Search` for OFF, `QrCodeScanner` for barcode, `Edit` for manual, `MenuBook` for recipe.
   - Text label in `titleSmall`.
   - Trailing chevron icon `ChevronRight` in `onSurfaceVariant`.
   - Dividers (`outlineVariant`) between items.

3. **Navigation on tap**:
   - "Search generic foods (USDA)" -> `log/search/usda`. If no USDA API key is configured, this item shows a subtitle "API key required -- configure in Settings" in `onSurfaceVariant` / `bodySmall`, and tapping it navigates to `settings` instead.
   - "Search branded products (OFF)" -> `log/search/off`.
   - "Scan barcode" -> `log/barcode`.
   - "Enter manually" -> `log/manual`.
   - "Log from recipe" -> `log/recipe_select`.

**States**:

| State    | Behaviour                                                    |
|----------|--------------------------------------------------------------|
| No USDA key | USDA option shows subtitle and redirects to settings.     |
| No recipes | Recipe option shows subtitle "No recipes saved" in `onSurfaceVariant`. Tapping still navigates to `log/recipe_select` which shows empty state. |
| Default  | All options available as described.                          |

---

### 3.4 Food Search (`log/search/{source}`)

**Purpose**: Search for food items via USDA FoodData Central or Open Food Facts. The `{source}` path parameter is "usda" or "off".

**ViewModel**: `AddEntryViewModel` (shared).

**Route**: `log/search/{source}`

**Layout**:

```
+----------------------------------------------+
| TopAppBar: X "Search {USDA/Products}"        |
+----------------------------------------------+
| [Search field with clear button       ] [Go] |
+----------------------------------------------+
| Search results (scrollable list):             |
|  +------------------------------------------+|
|  | Chicken breast, raw                      ||
|  | Per 100g: 165 kcal                       ||
|  | P: 31.0g  C: 0.0g  F: 3.5g              ||
|  +------------------------------------------+|
|  | Chicken breast, cooked                   ||
|  | Per 100g: 239 kcal                       ||
|  | P: 34.0g  C: 0.0g  F: 10.5g             ||
|  +------------------------------------------+|
|  ...                                          |
+----------------------------------------------+
```

**Elements**:

1. **Top app bar**: Close (X) icon. Title: "Search USDA" (for source=usda) or "Search Products" (for source=off).

2. **Search field**: `OutlinedTextField` spanning most of the width. Keyboard type: text. Placeholder: "Food name...". Leading icon: `Search`. Trailing icon: `Clear` (X) to clear the text, visible only when text is non-empty. The field is auto-focused on screen entry so the keyboard appears immediately. An adjacent "Go" `IconButton` (or the keyboard IME action "Search") triggers the search.

3. **Search debouncing**: The search fires 300ms after the user stops typing (architecture section 17.2). A manual "Go" tap or IME search action fires immediately, cancelling any pending debounce.

4. **Results list**: `LazyColumn`. Each item is a `Card` on `surface`:
   - Food name in `titleSmall`.
   - Subtitle line: For USDA (generic) results: "Per 100g: {kcal} kcal". For OFF (branded) results: "Per package label: {kcal} kcal" (or "Per 100g" if the API returns per-100g data).
   - Macro values in `bodySmall` / `onSurfaceVariant`: "P: Xg  C: Xg  F: Xg".
   - If any nutritional value is missing (null from API), display "--" for that value and show a warning icon (`Warning`, `error` colour) at the trailing edge of the item.
   - Tapping an item selects it in `AddEntryViewModel` and navigates to `log/weight_entry`. If the item has missing values (`missingFields` non-empty), the flow navigates to `log/missing_values` before `log/weight_entry`.

**States**:

| State    | Behaviour                                                                 |
|----------|---------------------------------------------------------------------------|
| Initial  | Empty results area. Centred text: "Search for a food to get started." in `onSurfaceVariant`. |
| Loading  | `LinearProgressIndicator` below the search field (indeterminate). Results area unchanged or shows previous results dimmed. |
| Results  | List of results as described.                                             |
| No results | Centred text: "No results found. Try a different search or enter manually." with a text button "Enter manually" navigating to `log/manual`. |
| Error (offline) | Centred icon `CloudOff` and text: "No internet connection. Search is unavailable." Below: text button "Enter manually" -> `log/manual`. |
| Error (API) | Centred text with error message from ViewModel (e.g. "Service temporarily unavailable" or "Invalid API key"). Below: text button "Enter manually" -> `log/manual`. |
| Error (rate limit) | Text: "Too many requests. Please try again later." Below: text button "Enter manually" -> `log/manual`. |

---

### 3.5 Barcode Scanner (`log/barcode`)

**Purpose**: Scan a product barcode using the device camera. Queries Open Food Facts.

**ViewModel**: `AddEntryViewModel` (shared).

**Route**: `log/barcode`

**Layout**:

```
+----------------------------------------------+
| TopAppBar: X "Scan Barcode"                  |
+----------------------------------------------+
|                                               |
|                                               |
|          [Camera preview viewfinder]          |
|          [                         ]          |
|          [    Scanning area box    ]          |
|          [                         ]          |
|                                               |
|  Align barcode within the frame               |
|                                               |
+----------------------------------------------+
```

**Elements**:

1. **Top app bar**: Close (X) icon. Title: "Scan Barcode".

2. **Camera preview**: Full remaining screen area. Uses CameraX `PreviewView` wrapped in `AndroidView`. A semi-transparent overlay with a clear rectangular cutout in the centre guides the user to position the barcode.

3. **Instruction text**: "Align barcode within the frame" in `bodyMedium` / `onSurface`, positioned below the scanning area cutout.

4. **On successful scan**: Camera stops. A brief loading indicator appears centred over the preview ("Looking up product..."). The barcode is passed to `FoodLookupRepository.lookupBarcode()`.
   - If found: food data is loaded into `AddEntryViewModel`. If missing values exist, navigate to `log/missing_values`. Otherwise navigate to `log/weight_entry`.
   - If not found: show a bottom sheet overlay on the camera preview: "Product not found for barcode {barcode}." with two buttons: "Try again" (resets scanner) and "Enter manually" (navigates to `log/manual` with the barcode value retained as context).

5. **Torch toggle**: A small `IconButton` in the top-right corner of the camera preview. Icon: `FlashOn` / `FlashOff`. Toggles the camera torch for scanning in low light.

**Camera Permission Flow**:

1. On navigating to this screen, check `CAMERA` permission status.
2. If not yet requested: show the system permission dialog via `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission)`.
3. If granted: show camera preview.
4. If denied (first time): the system dialog handles this. The screen shows the "permission denied" state.
5. If permanently denied (`shouldShowRequestPermissionRationale` returns false after denial): show the "permission denied" state.

**States**:

| State    | Behaviour                                                                 |
|----------|---------------------------------------------------------------------------|
| Permission not granted | Full-screen message: "Camera access is needed to scan barcodes." Below: `FilledButton` "Grant Permission" triggers the system permission dialog. Below that: `TextButton` "Search by name instead" -> pops back to `log/method`. |
| Permission permanently denied | Full-screen message: "Camera permission was denied. To scan barcodes, enable camera access in your device settings." Below: `FilledButton` "Open Settings" launches app settings intent. Below that: `TextButton` "Search by name instead" -> pops back to `log/method`. |
| Scanning | Camera preview with overlay as described. |
| Looking up | Loading indicator over camera. "Looking up product..." text. |
| Product not found | Bottom sheet overlay as described. |
| Error (offline) | Bottom sheet: "No internet connection. Cannot look up barcode." Buttons: "Enter manually" -> `log/manual`. |

---

### 3.6 Manual Entry (`log/manual`)

**Purpose**: User enters food name and nutritional values directly. No API lookup.

**ViewModel**: `AddEntryViewModel` (shared).

**Route**: `log/manual`

**Layout**:

```
+----------------------------------------------+
| TopAppBar: X "Manual Entry"                  |
+----------------------------------------------+
| Food name                                     |
| [                                          ] |
|                                               |
| Kilocalories consumed                         |
| [                    ] kcal                  |
|                                               |
| Protein consumed                              |
| [                    ] g                     |
|                                               |
| Carbohydrates consumed                        |
| [                    ] g                     |
|                                               |
| Fat consumed                                  |
| [                    ] g                     |
|                                               |
| [          Next          ]                   |
+----------------------------------------------+
```

**Elements**:

1. **Top app bar**: Close (X) icon. Title: "Manual Entry".

2. **Food name field**: `OutlinedTextField`. Label: "Food name". Keyboard type: text. Required. Auto-focused on screen entry.

3. **Nutrition fields**: Four `OutlinedTextField` components for kcal, protein, carbs, fat. Each has:
   - Label indicating the values are for the amount consumed (e.g. "Kilocalories consumed"). No per-100g qualifier -- the user enters final as-consumed values.
   - Trailing unit suffix.
   - Keyboard type: decimal number.
   - Required. Validation: must be zero or positive. Inline error: "Enter a valid number".

4. **Next button**: Full-width `FilledButton`. Text: "Next". Disabled if any field is empty or invalid. On tap: stores the as-consumed values directly in `AddEntryViewModel` (source: `MANUAL`, no scaling performed) and navigates to `log/confirm`. The weight entry step (`log/weight_entry`) is skipped entirely for manual entries.

**Note on ingredient mode**: When this screen is used in ingredient-addition mode during recipe creation, the field labels change to indicate per-100g context ("Kilocalories per 100g", etc.) and a weight field is also shown. This is because ingredients require per-100g reference values for proportional scaling of portions. The ingredient sub-flow returns to the recipe creation screen after weight entry rather than proceeding to `log/confirm`.

**States**:

| State    | Behaviour                                                    |
|----------|--------------------------------------------------------------|
| Default  | All fields empty, Next disabled.                             |
| Validation error | Inline errors below invalid fields. Next disabled.    |
| Valid    | All fields filled with valid values. Next enabled.           |

---

### 3.7 Missing Values Prompt (`log/missing_values`)

**Purpose**: Shown when a food item from an API lookup has incomplete nutritional data. The user supplies estimates for missing fields.

**ViewModel**: `AddEntryViewModel` (shared).

**Route**: `log/missing_values`

**Layout**:

```
+----------------------------------------------+
| TopAppBar: X "Complete Nutrition Data"       |
+----------------------------------------------+
| "{Food name}" is missing some values.        |
| Please provide estimates.                     |
|                                               |
| [Only missing fields shown, e.g.:]           |
|                                               |
| Kilocalories (per 100g)                       |
| [                    ] kcal                  |
|                                               |
| Known values:                                 |
|  Protein: 31.0g per 100g                     |
|  Carbs: 0.0g per 100g                        |
|  Fat: 3.5g per 100g                          |
|                                               |
| [          Next          ]                   |
+----------------------------------------------+
```

**Elements**:

1. **Top app bar**: Close (X) icon. Title: "Complete Nutrition Data".

2. **Context text**: "{Food name} is missing some values. Please provide estimates." in `bodyMedium`.

3. **Missing value fields**: `OutlinedTextField` for each field in `missingFields` from the `FoodSearchResult`. Same styling and validation as manual entry fields. Label: "{Nutrient} (per 100g)".

4. **Known values section**: Read-only display of the values that were present from the API. Styled as a list with `bodyMedium` / `onSurfaceVariant`. This gives the user context for estimating.

5. **Next button**: Full-width `FilledButton`. Disabled until all missing fields are filled with valid values. On tap: merges the user-supplied values into the food data in `AddEntryViewModel` and navigates to `log/weight_entry`.

**States**:

| State    | Behaviour                                          |
|----------|----------------------------------------------------|
| Default  | Missing fields empty, known fields shown. Next disabled. |
| Valid    | All missing fields filled. Next enabled.           |

---

### 3.8 Recipe Selection (`log/recipe_select`)

**Purpose**: User picks a saved recipe to log a portion of.

**ViewModel**: `AddEntryViewModel` (shared).

**Route**: `log/recipe_select`

**Layout**:

```
+----------------------------------------------+
| TopAppBar: X "Select Recipe"                 |
+----------------------------------------------+
| Recipe list (scrollable):                     |
|  +------------------------------------------+|
|  | Chicken stir-fry             450g total  ||
|  | 890 kcal total                           ||
|  | P: 65.0g  C: 80.0g  F: 25.0g            ||
|  +------------------------------------------+|
|  ...                                          |
+----------------------------------------------+
```

**Elements**:

1. **Top app bar**: Close (X) icon. Title: "Select Recipe".

2. **Recipe list**: `LazyColumn`. Each item is a `Card` on `surface`:
   - Recipe name in `titleSmall`.
   - Total weight: "{totalWeightG}g total" in `bodySmall` / `onSurfaceVariant`, trailing.
   - Total kcal in `bodyMedium`.
   - Macros in `bodySmall` / `onSurfaceVariant`: "P: Xg  C: Xg  F: Xg".
   - Tapping selects the recipe in `AddEntryViewModel` and navigates to `log/weight_entry`.

**States**:

| State    | Behaviour                                                    |
|----------|--------------------------------------------------------------|
| Loading  | `CircularProgressIndicator` centred.                         |
| Empty    | Centred text: "No recipes saved. Create one from the Recipes tab." in `onSurfaceVariant`. |
| Populated | List as described.                                          |

---

### 3.9 Weight Entry (`log/weight_entry`)

**Purpose**: User enters the weight consumed. The screen shows a live preview of scaled nutritional values as the weight changes.

**ViewModel**: `AddEntryViewModel` (shared).

**Route**: `log/weight_entry`

**Layout**:

```
+----------------------------------------------+
| TopAppBar: X "Amount"                        |
+----------------------------------------------+
| {Food name}                                   |
|                                               |
| Weight consumed                               |
| [ - ]  [        150       ] g  [ + ]         |
|                                               |
| Quick select:                                 |
| [25g] [50g] [100g] [150g] [200g] [250g]     |
| [100%]  (only for packaged/recipe items)     |
|                                               |
| Scaled nutrition preview:                     |
| +--------------------------------------------+|
| | Kcal     Protein    Carbs      Fat         ||
| | 248      46.5g      0.0g       5.5g        ||
| +--------------------------------------------+|
|                                               |
| [          Confirm          ]                |
+----------------------------------------------+
```

**Elements**:

1. **Top app bar**: Close (X) icon. Title: "Amount".

2. **Food name**: Displayed in `titleMedium` at the top of the content area.

3. **Weight input field**: `OutlinedTextField` centred. Keyboard type: decimal number. Trailing unit suffix "g". The field is auto-focused on screen entry.

4. **+/- buttons**: `IconButton` on either side of the weight field. Each tap adjusts the value by 1g. The minus button does not allow the value to go below 1 (rejecting negative and zero values). Long-press accelerates adjustment (10g increments after 500ms hold).

5. **Quick select chips**: Horizontally scrollable `LazyRow` of `FilterChip` components. Values: 25g, 50g, 100g, 150g, 200g, 250g. Tapping a chip sets the weight field to that value. The currently active value (if matching a chip) is shown in selected state.

6. **100% button**: A separate `FilterChip` labelled "100%". Shown only when the food source is a recipe (sets weight to `recipe.totalWeightG`) or a packaged food with a defined serving size. For generic USDA/manual foods, this chip is hidden. Tapping sets the weight to the total/serving weight.

7. **Scaled nutrition preview**: A `Card` on `surfaceVariant` showing four columns:
   - Headers: "Kcal", "Protein", "Carbs", "Fat" in `labelSmall`.
   - Values below each header in `titleMedium`, formatted per conventions.
   - Updates reactively as the weight field changes. Uses the scaling formulas from architecture section 12.

8. **Confirm button**: Full-width `FilledButton`. Text: "Confirm". Disabled if the weight field is empty, zero, or invalid. On tap: navigates to `log/confirm`.

**Validation**: Weight must be a positive number greater than zero. Inline error: "Enter a valid weight" if non-numeric. The field rejects negative values -- if the user types a minus sign it is ignored.

**States**:

| State    | Behaviour                                          |
|----------|----------------------------------------------------|
| Default  | Weight field empty or pre-populated with 100g. Preview shows scaled values for 100g. |
| Valid weight entered | Preview updates in real-time. Confirm enabled. |
| Invalid weight | Preview shows "--" for all values. Confirm disabled. |

---

### 3.10 Entry Confirmation (`log/confirm`)

**Purpose**: Final review before saving a log entry. Large, clear display of what will be saved. Explicit confirm/edit actions as required by product decisions.

**ViewModel**: `AddEntryViewModel` (shared).

**Route**: `log/confirm`

**Layout**:

```
+----------------------------------------------+
| TopAppBar: X "Review Entry"                  |
+----------------------------------------------+
|                                               |
| {Food name}                                   |
|                                               |
| +--------------------------------------------+
| |         1,250 kcal                         |
| |                                            |
| |  Protein       Carbs         Fat           |
| |  46.5g         120.0g        30.0g         |
| +--------------------------------------------+
|                                               |
|                                               |
| [          Save Entry          ]  (large)    |
|                                               |
| [          Go Back             ]  (large)    |
|                                               |
+----------------------------------------------+
```

**Elements**:

1. **Top app bar**: Close (X) icon (discards and returns to daily progress). Title: "Review Entry".

2. **Food name**: `headlineSmall` / `onBackground`, centred.

3. **Nutrition summary card**: `Card` on `surface`, centred content.
   - Kilocalories prominently displayed in `titleLarge` / `primary`, centred.
   - Below: three columns for macros, each with label in `labelSmall` / `onSurfaceVariant` and value in `titleMedium` / `onSurface`.

4. **Save Entry button**: Full-width `FilledButton`, `primary` colour, minimum height 56dp (easy to hit). Text: "Save Entry" in `labelLarge`. On tap: `AddEntryViewModel` saves the `LogEntry` via the repository, then pops the entire `log/*` nested graph and returns to `daily_progress`. A brief `Snackbar` appears on `daily_progress`: "Entry saved".

5. **Go Back button**: Full-width `OutlinedButton`, minimum height 56dp. Text: "Go Back" in `labelLarge`. Navigates back to `log/weight_entry` (or `log/manual` if the entry was manual-only).

Both buttons are deliberately large per the product decision for easy-to-hit confirm/edit targets.

**States**:

| State    | Behaviour                                          |
|----------|----------------------------------------------------|
| Displayed | Shows final calculated values. Two buttons.       |
| Saving   | Save button shows `CircularProgressIndicator` inline, disabled. Go Back disabled. |
| Save error | Snackbar: "Failed to save. Please try again." Buttons re-enabled. (This is an edge case -- Room writes rarely fail.) |

---

### 3.11 Recipe List (`recipes`)

**Purpose**: View all saved recipes. Top-level tab destination.

**ViewModel**: `RecipeListViewModel`

**Route**: `recipes`

**Layout**:

```
+----------------------------------------------+
| TopAppBar: "Recipes"                         |
+----------------------------------------------+
| Recipe list (scrollable):                     |
|  +------------------------------------------+|
|  | Chicken stir-fry                         ||
|  | 890 kcal | 450g total                    ||
|  | P: 65.0g  C: 80.0g  F: 25.0g            ||
|  +------------------------------------------+|
|  ...                                          |
+----------------------------------------------+
| [+ Create Recipe] FAB                         |
+----------------------------------------------+
| Bottom Navigation Bar                         |
+----------------------------------------------+
```

**Elements**:

1. **Top app bar**: Title "Recipes". No back arrow (top-level destination).

2. **Recipe list**: `LazyColumn`. Each item is a `Card` on `surface`:
   - Recipe name in `titleSmall`.
   - Second line: "{kcal} kcal | {totalWeightG}g total" in `bodySmall` / `onSurfaceVariant`.
   - Third line: macros in `bodySmall` / `onSurfaceVariant`.
   - Tapping navigates to `recipes/detail/{id}`.

3. **FAB**: `FloatingActionButton` with `Add` icon. Navigates to `recipes/create`.

**States**:

| State    | Behaviour                                                    |
|----------|--------------------------------------------------------------|
| Loading  | `CircularProgressIndicator` centred.                         |
| Empty    | Centred text: "No recipes yet. Tap + to create one." in `onSurfaceVariant`. |
| Populated | List as described.                                          |

---

### 3.12 Recipe Detail (`recipes/detail/{id}`)

**Purpose**: View a single recipe with its ingredients and nutritional totals.

**ViewModel**: `RecipeDetailViewModel`

**Route**: `recipes/detail/{id}`

**Layout**:

```
+----------------------------------------------+
| TopAppBar: <- "{Recipe name}" | [Edit] [Del] |
+----------------------------------------------+
| Totals:                                       |
| +--------------------------------------------+|
| | 890 kcal | 450g total                      ||
| | P: 65.0g  C: 80.0g  F: 25.0g              ||
| +--------------------------------------------+|
|                                               |
| Ingredients:                                  |
|  Chicken breast         200g    330 kcal     |
|  Broccoli               150g     51 kcal     |
|  Olive oil               15g    133 kcal     |
|  Rice                    85g    376 kcal     |
|                                               |
| Created: 01/01/2026                           |
| Last updated: 15/02/2026                      |
+----------------------------------------------+
```

**Elements**:

1. **Top app bar**: Back arrow -> `recipes`. Title: recipe name. Trailing actions: `Edit` icon button -> `recipes/edit/{id}`. `Delete` icon button -> shows delete confirmation dialog.

2. **Totals card**: `Card` on `surfaceVariant`. Total kcal in `titleMedium`, total weight in `bodyMedium` / `onSurfaceVariant`. Macros row in `bodyMedium`.

3. **Ingredients list**: Non-scrolling list (part of the overall scrollable content via `LazyColumn`). Each row: ingredient name left-aligned (`bodyMedium`), weight centre-right (`bodySmall` / `onSurfaceVariant`), and scaled kcal for that ingredient right-aligned (`bodySmall`). Dividers between items.

4. **Timestamps**: Created and last updated dates in `bodySmall` / `onSurfaceVariant` at the bottom.

5. **Delete confirmation dialog**: `AlertDialog`. Title: "Delete recipe?". Body: "'{recipe name}' will be permanently deleted. Previously logged entries are not affected." Buttons: "Cancel" (text) and "Delete" (text, `error` colour). On confirm: deletes via `RecipeDetailViewModel`, navigates back to `recipes`, Snackbar: "Recipe deleted".

**States**:

| State    | Behaviour                                          |
|----------|----------------------------------------------------|
| Loading  | Shimmer placeholders.                              |
| Populated | As described.                                     |
| Error (recipe not found) | "Recipe not found." with back button. (Edge case if deleted from another entry point.) |

---

### 3.13 Create Recipe (`recipes/create`) and Edit Recipe (`recipes/edit/{id}`)

**Purpose**: Create a new recipe or edit an existing one. These share the same screen layout. When editing, the screen pre-populates with existing data. In both modes the user can add ingredients, remove ingredients, and edit any existing ingredient in-place by tapping its row.

**ViewModel**: `CreateRecipeViewModel` (see architecture sections 7.7 and 7.8).

**Routes**: `recipes/create`, `recipes/edit/{id}`

**Layout**:

```
+----------------------------------------------+
| TopAppBar: X "Create Recipe" / "Edit Recipe" |
+----------------------------------------------+
| Recipe name                                   |
| [                                          ] |
|                                               |
| Live totals:                                  |
| +--------------------------------------------+|
| | 890 kcal | 450g                            ||
| | P: 65.0g  C: 80.0g  F: 25.0g              ||
| +--------------------------------------------+|
|                                               |
| Ingredients:                                  |
|  +------------------------------------------+|
|  | Chicken breast   200g   330 kcal   [X]   ||  <- whole row tappable -> opens edit sheet
|  +------------------------------------------+|
|  | Broccoli         150g    51 kcal   [X]   ||
|  +------------------------------------------+|
|                                               |
| [+ Add Ingredient]                            |
|                                               |
| [          Save Recipe          ]            |
+----------------------------------------------+
```

**Elements**:

1. **Top app bar**: Close (X) icon. Tapping shows discard confirmation dialog: "Discard changes?" with "Discard" and "Keep editing" buttons. The dialog appears whenever any unsaved change exists, including in-memory ingredient edits made via the edit ingredient sheet (Section 3.13a). Title: "Create Recipe" or "Edit Recipe".

2. **Recipe name field**: `OutlinedTextField`. Label: "Recipe name". Required.

3. **Live totals card**: `Card` on `surfaceVariant`. Updates in real-time as ingredients are added, edited, or removed. Shows total kcal, total weight, and macros. When no ingredients are added, shows "0 kcal | 0g" with all macros at 0.0g. Totals are derived from the in-memory ingredient list in `CreateRecipeViewModel` (architecture section 7.7), so any change -- add, remove, or in-place edit -- propagates immediately without database I/O.

4. **Ingredient list**: `LazyColumn` section. Each item is a tappable row:
   - **Tap target**: the row itself is the primary tap target for opening the **Edit Ingredient** sheet (Section 3.13a). The row uses `Modifier.clickable` and renders a subtle `ripple` on press. The entire row content area is a single 56dp-minimum-height tap target spanning the full width minus the trailing remove button. This satisfies Material's 48dp minimum touch-target requirement comfortably and matches the touch-target sizing used by log entry rows on Daily Progress (Section 3.1).
   - **Row content**:
     - Ingredient name (`bodyMedium`), left-aligned.
     - Weight (`bodySmall` / `onSurfaceVariant`), centre.
     - Scaled kcal for that ingredient (`bodySmall`), right of weight.
   - **Trailing remove control**: `IconButton` with `Close` (X) icon in `onSurfaceVariant`. This sits to the right of the row content area. It is a distinct 48dp tap target with its own click area, and per Material guidelines its click handler does not propagate to the row's `clickable`. Tapping it removes the ingredient from the in-memory list immediately (no confirmation) and live totals update. This separation prevents accidental edit-vs-remove confusion: tapping the row body opens the edit sheet; tapping the X icon removes the ingredient.
   - The row's content description for accessibility services reads "Edit {ingredient name}, {weight}g, {kcal} kcal" so screen readers announce the tap action explicitly. The remove icon has content description "Remove {ingredient name}".
   - Dividers (`outlineVariant`) between rows.

5. **Add Ingredient button**: `OutlinedButton` full-width. Text: "+ Add Ingredient". On tap: opens a modal bottom sheet for method selection that reuses the food lookup methods (USDA search, OFF search, barcode scan, manual entry). After selecting a food item and entering a weight, the ingredient is added to the in-memory ingredient list in `CreateRecipeViewModel` and the bottom sheet sub-flow dismisses, returning to the recipe screen with totals updated. The confirmation screen used in meal logging is skipped for ingredient addition.

6. **Save Recipe button**: Full-width `FilledButton`. Text: "Save Recipe". Disabled if the recipe name is empty or no ingredients are present. On tap: saves via `CreateRecipeViewModel`. The recipe's total nutritional values are computed from the ingredients (sum of each ingredient's scaled values). Navigates back to `recipes` (or `recipes/detail/{id}` for edits). Snackbar: "Recipe saved".

**Ingredient addition sub-flow**: To minimise navigation complexity, the ingredient lookup uses the same search/manual entry screens as meal logging but with a flag in the navigation arguments indicating "ingredient mode". In ingredient mode:
- The confirmation screen is skipped.
- After weight entry, the ingredient data is returned to `CreateRecipeViewModel` via a saved state handle or shared ViewModel pattern.

**States**:

| State    | Behaviour                                          |
|----------|----------------------------------------------------|
| Create mode | Empty name, empty ingredient list, totals at zero. Save disabled. |
| Edit mode (loading) | Shimmer while loading existing recipe data. |
| Edit mode (loaded) | Name and ingredients pre-populated. Totals reflect existing ingredients. Tapping any ingredient row opens the Edit Ingredient sheet pre-populated with that ingredient's current values. |
| No ingredients | Totals card shows zeros. Save disabled. "Add ingredients to get started." helper text above the Add Ingredient button. |
| Has ingredients | Totals update live. Save enabled if name is also filled. Each ingredient row is tappable for in-place editing. |
| Unsaved in-memory edits | Live totals reflect the edited values immediately. The Save Recipe button remains enabled. The discard confirmation dialog on the Close (X) icon treats any in-memory edit as an unsaved change. |

---

### 3.13a Edit Ingredient Sheet (modal, rendered from Section 3.13)

**Purpose**: Allow the user to modify an existing ingredient on the recipe being created or edited. The sheet is pre-populated with the tapped ingredient's current values. Confirming applies the change to the in-memory ingredient list in `CreateRecipeViewModel` and returns to the recipe screen with live totals updated. Cancelling discards the changes for that ingredient.

This is the UI counterpart to the architectural feature described in `architecture.md` Section 7.7 (`CreateRecipeViewModel.editIngredient(id, newValues)`) and Section 7.8 (UI layer specification of the dialog). The requirement source is `requirements.md` Recipes section ("In recipe edit mode, the user can modify an existing ingredient in-place by tapping the ingredient row...").

**Form factor decision: modal bottom sheet (`ModalBottomSheet`)**

The edit UI is implemented as a Material 3 **modal bottom sheet** anchored to the bottom of the screen, not a full-screen destination and not an inline-editable row.

**Justification**:

1. **Touch target size and one-handed reachability.** The sheet places its form fields and primary action (Save) in the lower half of the screen, within the natural thumb reach zone for one-handed phone use. A full-screen destination pushes the primary action either to a top app bar (small target, high-reach zone) or to the very bottom of a long scrollable layout (small target relative to total scroll length). The sheet keeps the Save button at a consistent, large 56dp-minimum touch target in the easy-reach zone. This matches the meal logging flow's preference for large, easy-to-hit confirm buttons (Section 3.10, product decision on log entry confirmation).

2. **Consistency with the rest of the app's modal patterns.** The app already uses bottom sheets for:
   - The ingredient method-selection chooser when adding a new ingredient (Section 3.13 element 5).
   - The barcode "product not found" overlay (Section 3.5 element 4).
   These are all transient, dismissible-with-cancel, single-purpose surfaces. The edit ingredient sheet fits this pattern cleanly. By contrast, full-screen destinations in this app (e.g. `log/weight_entry`, `log/manual`) are nodes in a multi-step flow with their own back stack semantics. Editing a single ingredient is not a multi-step flow and should not push a destination onto the navigation back stack.

3. **Inline-editable rows rejected.** An inline approach (each ingredient row expands in place to reveal six editable fields) was considered and rejected for three reasons. First, inline editing of six fields per row would either bloat the row height substantially (hurting the high-information-density principle when the row is collapsed but not being edited) or require an awkward in-row keyboard input area that competes with the system IME. Second, tap targets for individual fields on a narrow inline row are difficult to size correctly without horizontal scrolling. Third, inline rows make Cancel ambiguous: if the user taps a different row mid-edit, is the first row's edit discarded or applied? A modal sheet has unambiguous Cancel and Save semantics.

4. **Full-screen destination rejected.** A full-screen destination was considered and rejected because (a) it adds a navigation back-stack entry for a small, transient form, complicating discard behaviour (the Close X on the recipe screen and the back arrow on the destination would each need their own discard logic), (b) it visually disconnects the user from the recipe screen they are editing, which makes the live-totals running figure invisible at the moment of editing, and (c) it is inconsistent with the existing modal pattern for ingredient-related actions on the same screen.

**Layout**:

```
+----------------------------------------------+
|              (handle indicator)               |
+----------------------------------------------+
| Edit Ingredient                       [X]    |  <- header row
+----------------------------------------------+
| Ingredient name                               |
| [ Chicken breast                          ]  |
|                                               |
| Weight in recipe                              |
| [ - ]  [        200       ] g  [ + ]         |
|                                               |
| Nutrition per 100g:                           |
|                                               |
| Kilocalories                                  |
| [        165         ] kcal                  |
|                                               |
| Protein                                       |
| [         31.0       ] g                     |
|                                               |
| Carbohydrates                                 |
| [          0.0       ] g                     |
|                                               |
| Fat                                           |
| [          3.5       ] g                     |
|                                               |
| Scaled for this ingredient (preview):         |
| +--------------------------------------------+|
| | 330 kcal | P: 62.0g | C: 0.0g | F: 7.0g  ||
| +--------------------------------------------+|
|                                               |
| [   Cancel   ]   [        Save        ]      |
+----------------------------------------------+
```

**Elements**:

1. **Sheet container**: Material 3 `ModalBottomSheet` on `surface`. The sheet is scrollable internally (its content uses a `Column` inside a vertically scrolling container) so it remains usable on small screens when the IME is visible. The sheet expands to roughly 90% of screen height by default and can be dragged down to dismiss; dragging to dismiss is treated as Cancel.

2. **Header row**: "Edit Ingredient" in `titleMedium`, left-aligned. A trailing `IconButton` with `Close` (X) icon dismisses the sheet as Cancel. The drag handle (Material 3 default) sits above the header.

3. **Ingredient name field**: `OutlinedTextField`. Label: "Ingredient name". Keyboard type: text. Pre-populated with the ingredient's current `foodName`. Editable for all ingredient sources (USDA, Open Food Facts, MANUAL) per the requirement that API-sourced names can be corrected without re-fetching.

4. **Weight field**: `OutlinedTextField` with leading `-` and trailing `+` `IconButton` controls, matching the styling of the weight entry screen (Section 3.9 element 4). Keyboard type: decimal. Trailing unit suffix "g". Pre-populated with the ingredient's current `weightG`. The minus button cannot reduce the value below 1g (rejecting zero and negative values). Long-press accelerates adjustment (10g increments after 500ms hold). Section label above: "Weight in recipe".

5. **Per-100g nutrition fields**: A section labelled "Nutrition per 100g:" in `labelSmall` / `onSurfaceVariant`, followed by four `OutlinedTextField` components for kcal, protein, carbs, fat. Each has:
   - Label naming the nutrient ("Kilocalories", "Protein", "Carbohydrates", "Fat").
   - Trailing unit suffix ("kcal" for kilocalories, "g" for macros).
   - Keyboard type: decimal number.
   - Pre-populated with the ingredient's current `kcalPer100g`, `proteinPer100g`, `carbsPer100g`, `fatPer100g`.
   - Required. Validation: kcal must be zero or greater; each macro must be zero or greater. Inline error: "Enter a valid number" for non-numeric input. The architecture (Section 7.8) explicitly notes that zero is permitted for any per-100g macro to allow, for example, a zero-fat ingredient.

   These fields are editable for all ingredient sources, per the requirement that API-sourced per-100g values can be corrected (API data is often incomplete or incorrect, especially for Open Food Facts) without removing and re-adding the ingredient. The sheet does not display any "source" badge on these fields -- the source is retained internally on the draft only and does not constrain edit behaviour.

6. **Scaled-for-this-ingredient preview**: A `Card` on `surfaceVariant`. Shows the four nutritional values scaled from the currently-entered per-100g values by the currently-entered weight, using the formula `scaled = (per100g / 100) * weight`. Header row in `labelSmall`, value row in `bodyMedium`. Updates reactively as any field changes. This preview gives the user immediate feedback on the impact of their edit before they commit, mirroring the live preview pattern used on the weight entry screen (Section 3.9 element 7).

7. **Cancel button**: `OutlinedButton`. Text: "Cancel". Minimum height 56dp. Dismisses the sheet without invoking `CreateRecipeViewModel.editIngredient(...)`. The in-memory ingredient list is unchanged and the recipe screen's live totals do not move. Cancel is also the action triggered by tapping the header X icon, dragging the sheet down to dismiss, and tapping outside the sheet on the scrim.

8. **Save button**: `FilledButton`. Text: "Save". Minimum height 56dp. Disabled while any field is invalid. On tap: invokes `CreateRecipeViewModel.editIngredient(id, IngredientEditValues(...))` with the current sheet field values. The sheet dismisses. The recipe screen's ingredient list and live totals update immediately because they observe the ViewModel's in-memory state.

   The two buttons appear side-by-side, with Cancel on the leading edge and Save on the trailing edge, both wrapped in a row with equal weight. This matches the platform convention of placing the affirmative action on the trailing edge.

**Validation**:

Validation rules match those used when adding an ingredient (architecture Section 7.8):

- **Ingredient name**: non-empty after trimming. Inline error: "Name is required" if empty.
- **Weight**: must be greater than zero. Inline error: "Enter a valid weight". Negative values are rejected on input.
- **Each per-100g macro (kcal, protein, carbs, fat)**: must be zero or greater. Inline error: "Enter a valid number" for non-numeric input.

While any field is invalid, the Save button is disabled and the scaled preview shows "--" for any affected value.

**Persistence boundary**:

Per architecture Section 7.8, confirming the sheet does **not** persist to Room. The change is applied only to the in-memory `StateFlow<List<RecipeIngredientDraft>>` in `CreateRecipeViewModel`. Persistence happens only when the user taps Save Recipe on the parent recipe screen (Section 3.13 element 6), at which point the existing delete-and-reinsert strategy in `RecipeIngredientDao` flushes the entire updated ingredient list in a single transaction (architecture Section 5.3, Section 7.7).

This means a user who edits an ingredient and then taps the recipe screen's Close (X) -- choosing "Discard" on the discard confirmation -- will lose the edit, consistent with the behaviour of any other unsaved change. The discard confirmation dialog (Section 3.13 element 1) appears whenever any in-memory change exists, including ingredient edits made via this sheet.

**Navigation targets**:

- Save -> dismiss sheet, return to recipe screen with totals updated.
- Cancel / X / drag-to-dismiss / scrim tap -> dismiss sheet, no state change.
- The sheet does not push a destination onto the `NavController` back stack; it is rendered by the recipe screen and dismissed by changing the `isSheetVisible` state in the recipe screen's composable.

**States**:

| State    | Behaviour                                          |
|----------|----------------------------------------------------|
| Opened (pre-populated) | All six fields populated with the tapped ingredient's current values. Scaled preview shows the current scaled values. Save is enabled because the initial state is by definition valid. |
| Field being edited (still valid) | Scaled preview updates reactively. Save remains enabled. |
| Validation error in one or more fields | The invalid field shows an inline error. The scaled preview shows "--" for any field that cannot be computed. Save is disabled. |
| Saving (in-flight) | Not applicable -- the operation is in-memory only and completes synchronously. The sheet dismisses immediately on Save tap. |
| Cancel pressed (with field changes) | The sheet dismisses immediately. The in-memory ingredient list is unchanged. No confirmation dialog is shown at this level because the recipe screen's outer discard confirmation already covers the broader case of abandoning the recipe edit session. Discarding a single in-progress sheet edit is low-stakes -- the user can simply re-tap the ingredient. |

**Loading/empty/error states**: Not applicable. The sheet operates entirely on in-memory state and has no asynchronous data dependencies, no empty state (it is only opened from a populated row), and no failure modes other than form validation handled above.

---

### 3.14 Rolling Summaries (`summaries`)

**Purpose**: Display cumulative nutritional intake versus cumulative plan targets over rolling 7-day and 28-day periods. Top-level tab destination.

**ViewModel**: `SummariesViewModel`

**Route**: `summaries`

**Layout**:

```
+----------------------------------------------+
| TopAppBar: "Summaries"                       |
+----------------------------------------------+
| [  7 Days  |  28 Days  ]  tab row            |
+----------------------------------------------+
| Period: 13/03/2026 -- 19/03/2026             |
| (today excluded -- updates after 20:00)      |
|                                               |
| Kilocalories                                  |
|  Intake: 12,500 / Target: 14,000             |
|  [===================>       ]                |
|  Remaining: 1,500 kcal                       |
|                                               |
| Protein                                       |
|  Intake: 750.0g / Target: 1,050.0g           |
|  [===============>           ]                |
|  Remaining: 300.0g                            |
|                                               |
| Carbohydrates                                 |
|  Intake: 1,200.0g / Target: 1,750.0g         |
|  [==============>            ]                |
|  Remaining: 550.0g                            |
|                                               |
| Fat                                           |
|  Intake: 420.0g / Target: 455.0g              |
|  [====================>     ]                 |
|  Remaining: 35.0g                             |
|                                               |
| Daily average:                                |
|  1,786 kcal | P: 107.0g | C: 171.5g | F: 60.0g |
+----------------------------------------------+
| Bottom Navigation Bar                         |
+----------------------------------------------+
```

**Elements**:

1. **Top app bar**: Title "Summaries". No back arrow (top-level).

2. **Tab row**: Material 3 `TabRow` with two tabs: "7 Days" and "28 Days". Selected tab uses `primary` indicator. Switching tabs reloads data for the respective period using the same rolling-window rules below.

3. **Period label**: "dd/MM/yyyy -- dd/MM/yyyy" in `bodySmall` / `onSurfaceVariant`. The displayed dates are derived from the rolling-window rule (see below) so that the user can see exactly which days are included.

4. **Rolling-window status hint**: A short, single-line caption immediately below the period label, in `labelSmall` / `onSurfaceVariant`. It communicates whether today is included in the window:
   - **Before 20:00 local time (today excluded)**: "Today excluded -- updates after 20:00". This explains to the user why their entries from earlier in the day are not yet reflected in the totals.
   - **From 20:00 local time onward (today included)**: "Includes today". A neutral confirmation that the period now extends through the current day.
   This hint is essential because the inclusion/exclusion of today is otherwise invisible -- without it, a user who logged a meal at 13:00 and then opened Summaries at 13:30 would be confused that their entry "did not count". The hint is rendered as plain caption text, not as a banner or warning, so it remains low-noise on screens where most users will not notice or care.

5. **Metric sections**: Four sections (kcal, protein, carbs, fat), each containing:
   - Label in `titleSmall`.
   - "Intake: X / Target: Y" in `bodyMedium`. The target is the sum of per-day plan targets across the same `[startDate, endDate]` range, accounting for plan changes via `getPlanForDate()` (architecture section 7.6). Target and intake sums are aligned over the same set of days, so excluding today removes both today's intake and today's target from the period.
   - `LinearProgressIndicator` with the metric's semantic colour. Track: `progressTrack`.
   - "Remaining: Z" in `bodySmall` / `onSurfaceVariant`. If over target: "Over: Z" in `overage` colour.

6. **Daily average row**: `Card` on `surfaceVariant` at the bottom. Shows the period total divided by the number of days included in the window (7 days for the 7-day tab, 28 days for the 28-day tab). Formatted: "{kcal} kcal | P: Xg | C: Xg | F: Xg" in `bodyMedium`.

**Rolling-window rule** (display-side summary of architecture section 7.6):

The summary period is computed each time the screen becomes visible. Let `now` = local device time and `today` = local device date.

- **If `now.hour < 20`**: the window ends at end-of-yesterday. The 7-day tab covers the 7 days ending yesterday; the 28-day tab covers the 28 days ending yesterday. Today is excluded.
- **If `now.hour >= 20`**: the window ends at end-of-today. The 7-day tab covers the 7 days ending today (inclusive); the 28-day tab covers the 28 days ending today (inclusive). Today is included.

The 20:00 cutoff is a fixed constant in this version (`SUMMARY_CUTOFF_HOUR = 20`, defined in the ViewModel layer). It is not user-configurable and there is no UI to change it.

**Refresh behaviour**:

The summaries screen recomputes the window and reloads data every time the user navigates to it (e.g. by tapping the Summaries tab, or by switching between the 7-day and 28-day sub-tabs). This means:
- Newly logged entries from the Daily Progress tab appear immediately on the next visit to Summaries.
- If the user opens the app before 20:00, logs entries through the evening, leaves the app open, and returns to Summaries after 20:00, the window correctly updates to include today on the next visit. The user does not need to restart the app.

No manual "refresh" affordance is shown. The reload happens automatically per architecture section 7.5.

**States**:

| State    | Behaviour                                                    |
|----------|--------------------------------------------------------------|
| Loading  | Shimmer placeholders for the period label, rolling-window hint, and all metrics. |
| No plan  | Metrics show intake values only. Target shows "No plan". Progress bars hidden. Text: "Set up a nutrition plan to see targets." The rolling-window hint and period label are still shown so the user knows which days are summarised. |
| No entries in period | All intake values show 0. Progress bars at 0%. Daily average row shows "0 kcal | P: 0.0g | C: 0.0g | F: 0.0g". The period label and rolling-window hint are still shown. |
| No history yet (new user, before 20:00) | Special variant of the "no entries" state. The period label shows e.g. "Period: dd/MM/yyyy -- dd/MM/yyyy" (the 7 or 28 days ending yesterday), and a small note appears in place of the daily average row: "No entries yet for this period. Today is not yet included -- check back after 20:00." This is the only state that explicitly calls out the cutoff, because for a brand-new user it is the most likely point of confusion. |
| Populated | As described above.                                          |

---

### 3.15 Settings (`settings`)

**Purpose**: USDA API key management and nutrition plan management. Top-level tab destination.

**ViewModel**: `SettingsViewModel` (handles both USDA key and nutrition plan; `PlanViewModel` has been merged in).

**Route**: `settings`

**Layout**:

```
+----------------------------------------------+
| TopAppBar: "Settings"                        |
+----------------------------------------------+
| USDA API Key                                  |
|                                               |
| Status: [Configured] or [Not set]            |
|                                               |
| [                                          ] |
| Enter your USDA FoodData Central API key.    |
| Get a free key at fdc.nal.usda.gov           |
|                                               |
| [Save Key]  [Clear Key]                      |
|                                               |
| -------------------------------------------- |
|                                               |
| Nutrition Plan                                |
|                                               |
| Effective from: 01/01/2026  (or No plan set) |
|                                               |
| Daily kilocalories                            |
| [       2,000        ] kcal                  |
|                                               |
| Protein                                       |
| [        150.0       ] g                     |
|                                               |
| Carbohydrates                                 |
| [        250.0       ] g                     |
|                                               |
| Fat                                           |
| [         65.0       ] g                     |
|                                               |
| [       Save Plan       ]                    |
|                                               |
| Changes apply from today forward.            |
| Historical data is not affected.             |
|                                               |
| -------------------------------------------- |
|                                               |
| About                                         |
| Hungry Walrus v1.0                            |
| Data stored locally on this device only.     |
+----------------------------------------------+
| Bottom Navigation Bar                         |
+----------------------------------------------+
```

**Elements**:

1. **Top app bar**: Title "Settings". No back arrow (top-level).

2. **API key section**:
   - Section header: "USDA API Key" in `titleMedium`.
   - Status indicator: A chip/badge showing "Configured" in `tertiary` colour if a key exists, or "Not set" in `onSurfaceVariant` if not.
   - `OutlinedTextField` for the API key. If a key is already stored, the field shows a masked value ("****...****") and clears on focus for re-entry. Keyboard type: text.
   - Helper text below the field: "Enter your USDA FoodData Central API key." in `bodySmall` / `onSurfaceVariant`.
   - A clickable link: "Get a free key at fdc.nal.usda.gov" in `primary`, opens the URL in the device browser.
   - Two buttons side by side:
     - "Save Key": `FilledButton`. Disabled if the field is empty. On tap: saves to `EncryptedSharedPreferences`. Snackbar: "API key saved".
     - "Clear Key": `OutlinedButton`. Only shown if a key is stored. On tap: confirmation dialog "Clear your USDA API key? USDA search will be disabled." On confirm: clears the key. Snackbar: "API key cleared".

3. **Nutrition Plan section**:
   - Section header: "Nutrition Plan" in `titleMedium`.
   - **Effective date line**: If a plan exists, show "Effective from: {dd/MM/yyyy}" in `bodySmall` / `onSurfaceVariant`. If no plan exists, show "No plan configured" in `bodySmall` / `onSurfaceVariant`.
   - **Input fields**: Four `OutlinedTextField` components:
     - "Daily kilocalories" with "kcal" suffix. Keyboard type: number (integer). Must be greater than zero.
     - "Protein" with "g" suffix. Keyboard type: decimal. Must be zero or greater.
     - "Carbohydrates" with "g" suffix. Keyboard type: decimal. Must be zero or greater.
     - "Fat" with "g" suffix. Keyboard type: decimal. Must be zero or greater.
     - If a current plan exists, fields pre-populate with current values. If no plan exists, fields are empty with example placeholder text.
     - Validation: kcal must be a positive integer greater than zero. Macro fields must be zero or positive. Invalid or empty fields show inline error text: "Enter a valid number".
   - **Save Plan button**: Full-width `FilledButton` in `primary`. Text: "Save Plan". Disabled if any field is empty or invalid. On tap: saves via `SettingsViewModel.savePlan()`, inserting a new `NutritionPlan` row with `effectiveFrom = now`. Snackbar: "Plan updated". The daily progress screen reflects the change immediately without requiring navigation away.
   - **Note text**: "Changes apply from today forward. Historical data is not affected." in `bodySmall` / `onSurfaceVariant`.

4. **About section**: Simple informational block. App name and version in `bodyMedium`. Privacy note: "Data stored locally on this device only." in `bodySmall` / `onSurfaceVariant`.

**States**:

| State    | Behaviour                                          |
|----------|----------------------------------------------------|
| Key stored | API key status shows "Configured". Field masked. Clear Key button visible. |
| No key | API key status shows "Not set". Field empty. Clear Key button hidden. |
| Key corrupted (EncryptedSharedPreferences failure) | Status shows "Not set". Snackbar: "Could not read stored key. Please re-enter." Field empty. |
| Plan exists | Plan fields pre-populated. Effective date shown. |
| No plan | Plan fields empty. Effective date line shows "No plan configured". |
| Plan validation error | Inline error text below invalid plan fields. Save Plan button disabled. |

---

## 4. Interaction Flows

### 4.1 Meal Logging -- Generic Food Search (USDA)

**Optimised for**: Minimum taps. Target: 5 taps + typing for a complete entry.

1. User taps **FAB (+)** on Daily Progress. -> `log/method`
2. User taps **"Search generic foods (USDA)"**. -> `log/search/usda`
3. User types food name. Results appear after 300ms debounce.
4. User taps a result. -> `log/weight_entry` (or `log/missing_values` first if data is incomplete).
5. User enters weight (or taps a quick-select chip). Preview updates live.
6. User taps **"Confirm"**. -> `log/confirm`
7. User taps **"Save Entry"**. -> Entry saved. Pops to `daily_progress`.

**Total taps** (best case, no missing values): FAB + USDA option + result item + weight chip + Confirm + Save = **6 taps + typing the search query**.

### 4.2 Meal Logging -- Branded Product Search (Open Food Facts)

Identical flow to 4.1 but the user selects "Search branded products (OFF)" in step 2 and is routed to `log/search/off`.

### 4.3 Meal Logging -- Barcode Scan

1. User taps **FAB (+)** on Daily Progress. -> `log/method`
2. User taps **"Scan barcode"**. -> `log/barcode`
3. Camera opens. User points at barcode. Auto-detected.
4. Product looked up. -> `log/weight_entry` (or `log/missing_values` first).
5. User enters weight or taps quick-select chip. Preview updates.
6. User taps **"Confirm"**. -> `log/confirm`
7. User taps **"Save Entry"**. -> Pops to `daily_progress`.

**Total taps** (best case, product found, no missing values): FAB + Barcode option + weight chip + Confirm + Save = **5 taps** (no typing if barcode is scanned and a chip is used for weight).

### 4.4 Meal Logging -- Manual Entry

1. User taps **FAB (+)** on Daily Progress. -> `log/method`
2. User taps **"Enter manually"**. -> `log/manual`
3. User fills in food name and exact nutritional values as consumed (kcal, protein, carbs, fat).
4. User taps **"Next"**. -> `log/confirm` (weight entry step is skipped entirely).
5. User taps **"Save Entry"**. -> Pops to `daily_progress`.

**Total taps**: FAB + Manual option + Next + Save = **4 taps + typing 5 fields**.

### 4.5 Meal Logging -- Recipe Portion

1. User taps **FAB (+)** on Daily Progress. -> `log/method`
2. User taps **"Log from recipe"**. -> `log/recipe_select`
3. User taps a recipe. -> `log/weight_entry` (pre-loaded with recipe nutrition data scaled proportionally).
4. User enters portion weight or taps 100% for the full recipe.
5. User taps **"Confirm"**. -> `log/confirm`
6. User taps **"Save Entry"**. -> Pops to `daily_progress`.

**Total taps**: FAB + Recipe option + recipe item + weight chip/100% + Confirm + Save = **6 taps**.

### 4.6 Recipe Creation

1. User taps **FAB (+)** on Recipe List. -> `recipes/create`
2. User enters recipe name.
3. User taps **"+ Add Ingredient"**. -> Method selection bottom sheet.
4. User selects a lookup method (search, barcode, or manual). -> Ingredient sub-flow.
5. User finds/enters a food item and enters its weight in the recipe.
6. Ingredient is added to the list. Live totals update. User returns to recipe screen.
7. User repeats steps 3-6 for additional ingredients.
8. User taps **"Save Recipe"**. -> Recipe saved. Navigates to `recipes`.

### 4.7 Recipe Editing

1. User navigates to `recipes/detail/{id}`.
2. User taps the **Edit** icon. -> `recipes/edit/{id}`
3. Recipe name and ingredients are pre-populated. Live totals reflect current state.
4. User can:
   - Remove an ingredient by tapping the trailing X icon on its row.
   - Add a new ingredient by tapping "+ Add Ingredient" (same sub-flow as creation).
   - Change the recipe name.
   - **Edit an existing ingredient in-place by tapping its row.** This opens the Edit Ingredient bottom sheet (Section 3.13a) pre-populated with that ingredient's current name, weight, and per-100g values. The user can change any of these fields. Tapping **Save** on the sheet applies the change to the in-memory ingredient list, updates the recipe screen's live totals immediately, and dismisses the sheet. Tapping **Cancel** (or the X icon, or dragging the sheet down, or tapping outside the sheet) discards the change and dismisses the sheet without modifying the ingredient. No database write occurs at this point -- the edit is held in memory until the user saves the whole recipe.
5. User taps **"Save Recipe"**. -> Recipe updated (all in-memory changes flushed in a single transaction). Navigates to `recipes/detail/{id}`.

### 4.8 Nutrition Plan Setup

1. User navigates to `settings` via the "Plan" button on Daily Progress top bar, or by tapping the "No nutrition plan set" card, or by tapping the Settings tab in the bottom navigation.
2. User scrolls to the Nutrition Plan section and fills in kcal, protein, carbs, fat targets.
3. User taps **"Save Plan"**. -> Saved. Snackbar: "Plan updated". Daily Progress updates reactively.

### 4.9 Log Entry Deletion

1. User is on Daily Progress viewing today's entries.
2. User taps the **Delete** (trash) icon on a log entry.
3. Confirmation dialog appears: "Delete entry? {foodName} -- {kcal} kcal".
4. User taps **"Delete"**. -> Entry deleted. List updates. Progress recalculated.

### 4.10 Viewing Rolling Summaries

1. User taps the **Summaries** tab in the bottom navigation. -> `summaries`.
2. On entry, `SummariesViewModel` evaluates the local time and computes the rolling window:
   - Before 20:00: window ends end-of-yesterday.
   - From 20:00: window ends end-of-today.
3. The screen shows the 7-day tab by default. Period label and rolling-window hint reflect the computed window.
4. User can tap the **28 Days** tab to switch to the 28-day window. The window is re-evaluated and data reloaded.
5. If the user leaves Summaries (e.g. switches to Daily Progress) and returns, the window is re-evaluated on each return. This covers two cases:
   - New log entries made on Daily Progress are reflected on the next visit.
   - If the local time has crossed 20:00 since the previous visit, the window expands to include today.

---

## 5. Shared UI Components

These components are defined in `ui/component/` and reused across screens.

### 5.1 NutritionProgressBar

A horizontal `LinearProgressIndicator` with a label row above it.

**Props**: label (String), current (Double), target (Double), unit (String), colour (Color).

**Behaviour**: Fills proportionally. If `current > target`, the bar fills to 100% and the numeric display changes to overage styling (e.g. "Over: 250 kcal" in `overage` colour).

**Usage**: Daily Progress (kcal and macro rows), Summaries (all four metrics).

### 5.2 NutritionSummaryRow

A compact horizontal row showing four nutritional values.

**Props**: kcal (Double), protein (Double), carbs (Double), fat (Double).

**Format**: "P: Xg  C: Xg  F: Xg" in `bodySmall` / `onSurfaceVariant`. Kcal shown separately or inline depending on context.

**Usage**: Search results, recipe list items, log entry items, recipe detail ingredients.

### 5.3 NutritionCard

A card displaying nutritional values in a four-column grid layout.

**Props**: kcal (Double), protein (Double), carbs (Double), fat (Double), prominent (Boolean).

**Layout**: When `prominent = true` (confirmation screen): kcal in `titleLarge` centred above three macro columns in `titleMedium`. When `prominent = false` (weight entry preview): all four in a single row with `labelSmall` headers and `bodyMedium` values.

**Usage**: Entry confirmation, weight entry preview, live recipe totals, Edit Ingredient sheet scaled preview.

### 5.4 ConfirmationDialog

A standard Material 3 `AlertDialog` for destructive actions.

**Props**: title (String), body (String), confirmText (String), confirmColour (Color, default `error`), onConfirm (callback), onDismiss (callback).

**Usage**: Delete entry, delete recipe, discard changes.

### 5.5 QuickWeightSelector

A horizontally scrollable row of `FilterChip` components for common weights.

**Props**: options (List<Int>), selectedValue (Int?), onSelect (callback), show100Percent (Boolean), hundredPercentWeight (Double?).

**Usage**: Weight entry screen.

### 5.6 FoodSearchResultItem

A list item card for food search results.

**Props**: name (String), kcalPer100g (Double?), protein (Double?), carbs (Double?), fat (Double?), source (FoodSource), hasMissingValues (Boolean), onClick (callback).

**Layout**: Name in `titleSmall`. Nutrition info in `bodySmall`. Warning icon if `hasMissingValues`.

**Usage**: Food search results list.

### 5.7 LogEntryItem

A list item card for daily log entries.

**Props**: foodName (String), kcal (Double), protein (Double), carbs (Double), fat (Double), timestamp (Long), onDelete (callback).

**Layout**: As described in Daily Progress section 3.1.

**Usage**: Daily Progress log entries list.

### 5.8 RollingWindowHint

A single-line caption used by the Summaries screen to communicate which days are included in the rolling window.

**Props**: includesToday (Boolean).

**Layout**: `labelSmall` / `onSurfaceVariant`, single line, left-aligned beneath the period label.
- When `includesToday = false`: text reads "Today excluded -- updates after 20:00".
- When `includesToday = true`: text reads "Includes today".

**Usage**: Summaries screen, below the period label on both the 7-day and 28-day tabs.

### 5.9 IngredientRow

A tappable list item used in the Create/Edit Recipe screen ingredient list.

**Props**: name (String), weightG (Double), kcal (Double), onClick (callback), onRemove (callback).

**Layout**: Row content (name, weight, kcal) occupies the primary tap area mapped to `onClick`. A trailing `IconButton` with `Close` icon is wired to `onRemove` and has a separate click area. Minimum row height 56dp. Content description on the primary tap area reads "Edit {name}, {weightG}g, {kcal} kcal"; content description on the remove icon reads "Remove {name}".

**Usage**: Create Recipe and Edit Recipe ingredient list (Section 3.13).

### 5.10 EditIngredientSheet

A modal bottom sheet for in-place editing of a recipe ingredient.

**Props**: initialValues (IngredientEditValues with name, weight, per-100g kcal/protein/carbs/fat), onSave (callback with new IngredientEditValues), onCancel (callback).

**Layout**: As described in Section 3.13a. Header, six input fields, scaled-for-this-ingredient preview, and a Cancel/Save button row.

**Usage**: Rendered from the Create Recipe and Edit Recipe screen when an ingredient row is tapped.

---

## 6. Edge Cases and Special Behaviours

### 6.1 First Launch

On first launch, no nutrition plan exists. The Daily Progress screen shows the "No nutrition plan set" card in place of the progress section. The user can still log entries (they are saved without plan context), but no progress bars are shown. The summaries screen shows intake only with a prompt to set up a plan.

### 6.2 No USDA API Key

USDA search is disabled in the log method selection screen. The option shows a subtitle indicating an API key is required. All other logging methods remain functional. This is not a blocking state.

### 6.3 Offline Mode

When the device has no internet connection:
- Food search screens show the offline error state with a link to manual entry.
- Barcode scanning can detect barcodes (ML Kit works offline) but the lookup fails. The cached barcode result (if available from a previous lookup within 30 days) is returned. Otherwise, the "product not found" state appears with a manual entry option.
- All local features (daily progress, recipes, summaries, plan editing, manual entry) work normally.
- Connectivity is checked via `ConnectivityManager.getNetworkCapabilities()`.

### 6.4 Plan Changes Mid-Period

When the user changes their nutrition plan, the new plan takes effect immediately. The summaries screen correctly sums per-day targets using the plan that was active on each day within the rolling window (via `getPlanForDate()`). This means a 7-day summary may reflect two different daily targets if the plan was changed within the window. The summaries screen does not display a notice about plan changes within the period -- the cumulative target simply reflects the correct summed value. Because the same rolling window is used for both intake and target sums, the inclusion or exclusion of today by the 20:00 cutoff is automatically consistent across the two values.

### 6.5 Incomplete API Data

When a food item from USDA or Open Food Facts is missing one or more of the four core nutritional values, the flow automatically routes through `log/missing_values` before reaching `log/weight_entry`. The user must fill in all missing values before proceeding. This is enforced by the `missingFields` property on `FoodSearchResult`.

For ingredients already added to a recipe, the user does **not** need to remove and re-add an ingredient to fix incomplete or incorrect API data. Tapping the ingredient row opens the Edit Ingredient sheet (Section 3.13a), where the name and all per-100g values are editable regardless of the ingredient's source.

### 6.6 Very Large Numbers

The formatting system uses UK locale with comma thousands separators. For extremely large kcal values (unlikely but possible with very heavy portions), the layout should not break. The `NutritionCard` and progress bar labels use flexible width containers. No truncation occurs; if text overflows, it wraps to a second line.

### 6.7 Rapid Entry

For users logging multiple items in quick succession, the return to `daily_progress` after saving an entry means the FAB is immediately available for the next entry. The flow is: Save -> pop to daily progress -> tap FAB -> start next entry. This is 1 extra tap between entries, which is the minimum possible given the confirmation requirement.

### 6.8 Data Retention

Log entries older than 2 years are automatically deleted by `DataRetentionWorker`. The user is never notified of this -- it happens silently in the background. Recipes are retained indefinitely and can be manually deleted by the user from the recipe detail screen.

### 6.9 Rolling Window Cutoff (20:00 boundary)

The Summaries screen excludes the current day from the rolling 7-day and 28-day windows until 20:00 local device time. From 20:00 onward today is included. This rule comes from the requirements (Rolling Summaries section) and is implemented per architecture section 7.6.

Design implications:

- The Summaries screen always shows a period label with concrete start and end dates, so the user can verify which days are covered without needing to know the rule.
- A short rolling-window hint sits below the period label (see section 3.14 element 4) saying either "Today excluded -- updates after 20:00" or "Includes today". This is the only place in the UI where the 20:00 cutoff is surfaced. It is intentionally low-noise -- a plain caption, not a banner or modal.
- The hint, period label, and totals all recompute on each visit to the screen (see section 3.14 refresh behaviour). A user who opens the app at 19:55, leaves it open, and returns to Summaries at 20:05 will see the window expand to include today on the second visit.
- A new user opening Summaries before 20:00 on their first day will see no totals (because yesterday and earlier had no entries). The "no history yet" state (section 3.14, states table) addresses this specific case with an explanatory note. After 20:00 on the same day, today's entries appear in the window.
- The Daily Progress screen is unaffected by the cutoff. It always reflects today's intake in real time. The cutoff is exclusively a summaries-screen concept.
- The 20:00 value is not user-configurable in this version. No settings affordance is shown for it. If a future version makes it configurable, the corresponding control would live in the Settings screen, but designing that UI is out of scope for v1.

---

## 7. Accessibility

While not a primary design focus, the following baseline accessibility measures apply:

- All touch targets are minimum 48dp x 48dp per Material guidelines.
- All icons have content descriptions for screen readers.
- Progress bars expose their value and range to accessibility services.
- Input fields have associated labels.
- Confirmation dialogs use semantic button roles (confirm/dismiss).
- Colour is never the sole means of conveying information -- all progress indicators also display numeric values.
- Text contrast ratios meet WCAG AA on the dark background (all `onBackground` and `onSurface` text on `background`/`surface` exceeds 4.5:1 ratio).
- The rolling-window hint on the Summaries screen is plain text and is read aloud by screen readers in natural reading order after the period label.
- The ingredient row on the Create/Edit Recipe screen exposes its primary tap action via an explicit content description ("Edit {name}, {weightG}g, {kcal} kcal") so screen reader users are not surprised by the otherwise-unannotated row-level tap target. The remove icon has its own distinct content description ("Remove {name}").

---

## 8. UX Issues Identified

The following notes document areas where the UX design and architecture may warrant further discussion. They are documented here as the design specification must remain consistent with the architecture document.

### 8.1 No Edit Capability for Log Entries

The architecture and requirements explicitly place log entry editing out of scope. The mitigation is the confirmation screen before saving. However, if a user saves an incorrect entry, they must delete it and re-enter from scratch. This is a known friction point for v1.

### 8.2 Recipe Ingredient Sub-Flow Navigation Complexity

Adding ingredients to a recipe reuses the food lookup flow. This means the recipe creation screen must manage a nested navigation sub-flow (method selection -> search/barcode/manual -> weight entry -> return). The architecture defines `CreateRecipeViewModel` as separate from `AddEntryViewModel`. The ingredient sub-flow must either use a separate ViewModel scoped to the ingredient addition, or pass data back via navigation result APIs (`SavedStateHandle`). The recommended approach is to use `SavedStateHandle` to pass the ingredient data back to `CreateRecipeViewModel` after the weight entry step.

### 8.3 Bottom Sheet vs Full-Screen for Ingredient Method Selection

The design specifies a bottom sheet for ingredient method selection during recipe creation, while the architecture shows full navigation routes for the log flow. The implementation may use either approach. A bottom sheet reduces the feeling of deep navigation nesting. A full-screen approach is consistent with the logging flow. The developer should prefer whichever approach minimises shared state complexity.

### 8.4 100% Button Applicability

The 100% quick-select button for packaged foods depends on whether the API response includes a defined serving/package size. Open Food Facts often includes `serving_size` but not always. USDA Foundation/SR Legacy data does not have a standard package size. The 100% button should only appear when a reference total weight is available (recipes always have `totalWeightG`; API results only when a serving size is present in the response). The architecture does not currently store a serving size in `FoodCache`. This means the 100% button will primarily be useful for recipe portions. If packaged food serving sizes are desired, a `servingSizeG` nullable field would need to be added to `FoodCache`.

### 8.5 Discoverability of the 20:00 Rolling-Window Cutoff

The 20:00 cutoff rule is non-obvious. Before 20:00, today's entries do not appear in the Summaries totals at all, which can look like a bug to a user who logged a meal an hour ago. The design mitigates this with:

1. An always-visible period label showing concrete start and end dates.
2. A short rolling-window hint immediately below it ("Today excluded -- updates after 20:00" / "Includes today").
3. A dedicated "no history yet" state for new users opening Summaries before 20:00 on their first day.

These measures are deliberately low-noise (a caption and a period label, not a banner). The trade-off is that some users may still be briefly confused on first encounter. If the product owner later reports that this is a recurring point of confusion, alternatives include:

- A small `Info` icon next to the hint that opens a popover explaining the rule.
- Showing today's running intake as a separate, visually distinct row above the main totals (e.g. "Today so far -- not yet included: 1,250 kcal").

Both would add visual noise and complexity, so they are not adopted by default. They are noted here for future consideration.

### 8.6 Row-Tap for Ingredient Edit vs Explicit Edit Affordance

The Edit Ingredient sheet is opened by tapping the ingredient row itself, not by an explicit "edit" icon. This was chosen because (a) the row is already a substantial 56dp+ tap target by virtue of its content, so it does not waste density on a redundant icon, and (b) the trailing X (remove) icon is the only secondary action and is well-separated visually. The trade-off is discoverability: a user who has never edited an ingredient must learn that the row is tappable. Mitigations: the row uses a `Modifier.clickable` with the default ripple, which gives a visual hint on press; the accessibility content description explicitly announces "Edit {name}, ..."; and the row is part of a list whose other instance (log entries on Daily Progress) does not currently have a similar tap action, so there is no inconsistent precedent. If discoverability proves a problem in usability testing, a small trailing `Edit` icon could be added without changing the tap behaviour.

---

## 9. Revision History

### Revision 2 -- 2026-03-22

Amendments based on updated requirements (P03, P04) and architecture Revision 1.

#### Changes

1. **Nutrition plan management moved to Settings (P03).** The dedicated `plan` route and `PlanViewModel` have been removed. Section 3.2 is replaced with a tombstone note. Section 3.15 (Settings) is expanded with a full Nutrition Plan sub-section including the plan input fields, effective date, save button, and validation rules. `SettingsViewModel` now owns both USDA API key management and plan management. References in Section 3.1 (Daily Progress) updated so the "Plan" button and "No nutrition plan set" card both navigate to `settings` instead of `plan`. The "Plan screen" row removed from the back-behaviour table in Section 2.3. Section 4.8 updated to reflect plan setup now occurs within the Settings screen.

2. **Manual entry accepts as-consumed values, skips weight entry (P04).** Section 3.6 (Manual Entry) updated: field labels changed from "per 100g" context to "as consumed" (e.g. "Kilocalories consumed"). The Next button now navigates directly to `log/confirm`, bypassing `log/weight_entry`. A note is added to clarify that in ingredient-addition mode (recipe creation), the same screen still presents per-100g labels and a weight field, as recipes require per-100g reference data for proportional scaling. Section 4.4 interaction flow updated to remove the weight entry step, reducing the tap count from 6 to 4 (plus typing).

### Revision 3 -- 2026-05-12

Amendments based on requirements Revision 1 (2026-05-12) and architecture Revision 2 (2026-05-12), which introduced the rolling-summary window cutoff at 20:00 local device time.

#### Changes

3. **Rolling-summary window cutoff surfaced on the Summaries screen (Section 3.14).** The screen now describes the window-computation rule in user-facing terms: before 20:00 local time the window ends end-of-yesterday; from 20:00 onward it ends end-of-today. The previously generic "period always ends on today" text has been replaced by a concrete rule consistent with architecture section 7.6. The period label's example was updated to reflect a "today excluded" window.

4. **New "rolling-window hint" caption added below the period label (Section 3.14, element 4).** A single-line `labelSmall` caption sits beneath the period label and reads either "Today excluded -- updates after 20:00" (before 20:00) or "Includes today" (from 20:00). This is the primary user-facing surface for the cutoff rule and is intentionally low-noise -- a caption rather than a banner.

5. **Refresh behaviour clarified on the Summaries screen (Section 3.14).** The screen now explicitly recomputes the rolling window and reloads data on every visit. The interaction flow handles two cases: new entries logged between visits, and crossing the 20:00 boundary while the app is open. No manual refresh affordance is shown.

6. **New "no history yet" state added to Summaries (Section 3.14, states table).** For a brand-new user opening Summaries before 20:00 on their first day, the screen shows an explanatory note in place of the daily-average row: "No entries yet for this period. Today is not yet included -- check back after 20:00." This is the only state that calls out the cutoff explicitly, because it is the most likely point of first-time confusion.

7. **New interaction flow added: Viewing Rolling Summaries (Section 4.10).** Describes the tap path, the window-computation step, tab switching between 7-day and 28-day views, and the re-evaluation that occurs on each return to the screen (covering both newly logged entries and 20:00-boundary crossings).

8. **New shared component: RollingWindowHint (Section 5.8).** Documents the low-noise caption component used on Summaries, including its two text variants based on the `includesToday` boolean prop.

9. **New edge case section: Rolling Window Cutoff (Section 6.9).** Consolidates the design implications of the 20:00 rule: always-visible period label with concrete dates, rolling-window hint, first-day new-user state, and a note that the Daily Progress screen is unaffected by the cutoff (it always reflects today's intake in real time). Also confirms that the cutoff is not user-configurable in v1.

10. **Section 6.4 (Plan Changes Mid-Period) refined.** Wording updated to refer to "the rolling window" rather than "the period" and to note explicitly that because intake and target sums share the same window, the cutoff applies consistently to both -- excluding today removes today's intake and today's target together.

11. **New UX issue: Discoverability of the 20:00 Cutoff (Section 8.5).** Documents the trade-off between low-noise design and discoverability for a non-obvious rule, lists the three mitigations adopted, and records two unadopted alternatives (info popover, "today so far" row) for future consideration if confusion proves recurring.

12. **Accessibility note added (Section 7).** Confirms that the new rolling-window hint is read aloud by screen readers in natural reading order after the period label.

### Revision 4 -- 2026-05-12

Amendments based on requirements Revision 2 (2026-05-12) and architecture Revision 3 (2026-05-12), which introduced in-place editing of recipe ingredients during recipe creation and editing.

#### Changes

13. **Section 3.13 (Create/Edit Recipe) updated for in-place ingredient editing.** Each ingredient row is now a tappable element whose primary tap target opens the Edit Ingredient sheet (Section 3.13a). The trailing X icon retains its existing remove-ingredient behaviour and is a separate tap target with its own click handler that does not propagate to the row. The row's accessibility content description explicitly announces "Edit {name}, {weight}g, {kcal} kcal". Row minimum height is 56dp. The discard confirmation dialog on the Close (X) icon now considers any in-memory ingredient edit as an unsaved change. The states table gains a new entry for "Unsaved in-memory edits" and an annotation on "Edit mode (loaded)" indicating that ingredient rows are tappable for in-place editing.

14. **New section 3.13a (Edit Ingredient Sheet).** A new sub-section documents the modal bottom sheet that opens when an ingredient row is tapped. The sheet is pre-populated with the ingredient's current name, weight, and per-100g values. All six fields are editable for all ingredient sources (USDA, Open Food Facts, MANUAL), per the requirement that API-sourced values can be corrected without re-fetching. The sheet includes a scaled-for-this-ingredient preview card that updates reactively as fields change. Validation rules match those used when adding an ingredient (non-empty name, weight > 0, per-100g macros >= 0). Save invokes `CreateRecipeViewModel.editIngredient(id, IngredientEditValues)` and dismisses; Cancel (via the X icon, drag-to-dismiss, scrim tap, or the Cancel button) dismisses without applying changes. Persistence to Room occurs only when the user saves the entire recipe, consistent with architecture Section 7.7 and Section 7.8.

15. **Form-factor justification: modal bottom sheet selected.** Section 3.13a includes an explicit justification for choosing a modal bottom sheet over a full-screen destination and over an inline-editable row. Key reasons: (a) the sheet keeps the primary Save action in the thumb-reach zone with a consistent 56dp touch target, matching the meal logging flow's preference for large easy-to-hit confirm buttons; (b) it is consistent with the app's existing modal patterns (ingredient method-selection sheet, barcode product-not-found bottom sheet) and avoids the back-stack complexity of a full-screen destination for a transient single-purpose form; (c) inline-row editing was rejected because six per-row fields hurts information density when collapsed, fights with the system IME when expanded, and creates ambiguous Cancel semantics when switching between rows mid-edit.

16. **New interaction flow: Recipe Editing updated (Section 4.7).** The flow now describes in-place ingredient editing as a first-class option alongside add and remove. The flow notes that the edit is held in memory until the user saves the whole recipe, and that Cancel (including scrim tap and drag-to-dismiss) discards the change for that single ingredient without affecting the rest of the in-memory state.

17. **New shared components: IngredientRow (Section 5.9) and EditIngredientSheet (Section 5.10).** `IngredientRow` documents the tappable list item with a separate trailing remove button, including the dual-content-description pattern for accessibility. `EditIngredientSheet` documents the modal bottom sheet as a reusable component scoped to the recipe screen, including its props (initial values, save callback, cancel callback) and the scaled-preview behaviour.

18. **NutritionCard usage extended (Section 5.3).** The scaled-for-this-ingredient preview inside the Edit Ingredient sheet reuses the existing `NutritionCard` component (in its non-prominent form). The Usage line of Section 5.3 has been updated to include the Edit Ingredient sheet.

19. **Section 6.5 (Incomplete API Data) updated.** A note has been added that for ingredients already added to a recipe, the user does not need to remove and re-add an ingredient to correct incomplete or incorrect API data; tapping the ingredient row opens the Edit Ingredient sheet where the name and all per-100g values are editable regardless of source. This matches the mitigation noted in architecture Section 17.4.

20. **New UX issue (Section 8.6): Row-Tap for Ingredient Edit vs Explicit Edit Affordance.** Documents the choice not to add a dedicated edit icon to each ingredient row, the discoverability trade-off, and the mitigations adopted (Material ripple on press, explicit accessibility content description). Notes that a small trailing edit icon could be added later without changing the tap behaviour if usability testing reveals a discoverability problem.

21. **Accessibility note added (Section 7).** Confirms that the ingredient row's primary tap action is exposed via an explicit content description so screen reader users are not surprised by the otherwise-unannotated row-level tap target, and that the remove icon retains its own distinct content description.
