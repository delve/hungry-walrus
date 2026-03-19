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
| Plan screen                              | Pop to daily_progress                          |

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
|  [=========>          ] progress bar          |
|                                               |
|  Protein   Carbs      Fat                    |
|  45.0/150.0g 120.0/250.0g 30.0/65.0g        |
|  [=====>  ] [======> ] [====>   ]            |
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

1. **Top app bar**: Title "Today" left-aligned. Current date displayed right-aligned in `bodySmall` / `onSurfaceVariant`. A text button "Plan" in `primary` colour at the trailing edge navigates to `plan`.

2. **Progress summary section** (non-scrollable, pinned above the list):
   - **Kilocalories row**: A single horizontal `LinearProgressIndicator` spanning full width. Fill colour: `progressKcal`. Track: `progressTrack`. Above the bar: left-aligned "X / Y kcal" in `titleMedium`, right-aligned "Remaining: Z kcal" in `bodyMedium` / `onSurfaceVariant`. If intake exceeds target, the remaining text changes to "Over: Z kcal" in `overage` colour, and the progress bar fill uses `overage` colour for the portion exceeding 100%.
   - **Macro row**: Three equal-width columns, one for each macro (Protein, Carbs, Fat). Each column contains: a label in `labelSmall` / `onSurfaceVariant`, a value "X / Yg" in `bodyMedium`, and a thin `LinearProgressIndicator` with its respective semantic colour. Progress bars clamp visually at 100% but the numeric value shows the true amount.
   - If no plan is configured, this section shows a card: "No nutrition plan set. Tap to configure." The card is tappable and navigates to `plan`.

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

### 3.2 Nutrition Plan (`plan`)

**Purpose**: View and edit daily nutrition targets. Accessed from daily progress screen. Infrequent operation -- favour clarity.

**ViewModel**: `PlanViewModel`

**Route**: `plan`

**Layout**:

```
+----------------------------------------------+
| TopAppBar: <- "Nutrition Plan"               |
+----------------------------------------------+
| Current plan (if set):                        |
|  Effective from: 01/01/2026                  |
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
| Note: Changes apply from today forward.      |
| Historical data is not affected.             |
+----------------------------------------------+
```

**Elements**:

1. **Top app bar**: Back arrow navigates to `daily_progress`. Title: "Nutrition Plan".

2. **Effective date line**: If a plan exists, show "Effective from: {date}" in `bodySmall` / `onSurfaceVariant`. If no plan exists, show "No plan configured" in `bodyMedium` / `onSurfaceVariant`.

3. **Input fields**: Four `OutlinedTextField` components, each with:
   - Label above: "Daily kilocalories", "Protein", "Carbohydrates", "Fat".
   - Trailing text: unit label ("kcal" or "g") as a suffix inside the field.
   - Keyboard type: decimal number (kcal field uses number-only since targets are integers per the entity definition; macro fields allow decimals).
   - If a current plan exists, fields pre-populate with current values.
   - If no plan exists, fields are empty with placeholder text showing example values.
   - Validation: all fields must be positive numbers. Empty fields or non-numeric input shows inline error text below the field: "Enter a valid number".

4. **Save button**: Full-width `FilledButton` in `primary`. Text: "Save Plan". Disabled (greyed out) if any field is empty or invalid. On tap, saves via `PlanViewModel.savePlan()` which inserts a new `NutritionPlan` row with `effectiveFrom = now`. Shows a brief `Snackbar`: "Plan updated" and navigates back to `daily_progress`.

5. **Note text**: `bodySmall` / `onSurfaceVariant`. Static informational text.

**States**:

| State    | Behaviour                                                    |
|----------|--------------------------------------------------------------|
| Loading  | Fields show shimmer placeholders.                            |
| No plan  | Fields empty, effective date line shows "No plan configured".|
| Plan exists | Fields populated with current values.                     |
| Validation error | Inline error text below invalid fields. Save button disabled. |

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
| Kilocalories (per 100g)                       |
| [                    ] kcal                  |
|                                               |
| Protein (per 100g)                            |
| [                    ] g                     |
|                                               |
| Carbohydrates (per 100g)                      |
| [                    ] g                     |
|                                               |
| Fat (per 100g)                                |
| [                    ] g                     |
|                                               |
| [          Next          ]                   |
+----------------------------------------------+
```

**Elements**:

1. **Top app bar**: Close (X) icon. Title: "Manual Entry".

2. **Food name field**: `OutlinedTextField`. Label: "Food name". Keyboard type: text. Required. Auto-focused on screen entry.

3. **Nutrition fields**: Four `OutlinedTextField` components for kcal, protein, carbs, fat. Each has:
   - Label indicating "per 100g" context.
   - Trailing unit suffix.
   - Keyboard type: decimal number.
   - Required. Validation: must be zero or positive. Inline error: "Enter a valid number".

4. **Next button**: Full-width `FilledButton`. Text: "Next". Disabled if any field is empty or invalid. On tap: stores the per-100g reference values in `AddEntryViewModel` (source: `MANUAL`) and navigates to `log/weight_entry`.

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

**Purpose**: Create a new recipe or edit an existing one. These share the same screen layout. When editing, the screen pre-populates with existing data.

**ViewModel**: `CreateRecipeViewModel`

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
|  | Chicken breast   200g   330 kcal   [X]   ||
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

1. **Top app bar**: Close (X) icon. Tapping shows discard confirmation dialog: "Discard changes?" with "Discard" and "Keep editing" buttons. Title: "Create Recipe" or "Edit Recipe".

2. **Recipe name field**: `OutlinedTextField`. Label: "Recipe name". Required.

3. **Live totals card**: `Card` on `surfaceVariant`. Updates in real-time as ingredients are added or removed. Shows total kcal, total weight, and macros. When no ingredients are added, shows "0 kcal | 0g" with all macros at 0.0g.

4. **Ingredient list**: `LazyColumn` section. Each item shows:
   - Ingredient name (`bodyMedium`).
   - Weight (`bodySmall` / `onSurfaceVariant`).
   - Scaled kcal for that ingredient (`bodySmall`).
   - Trailing `IconButton` with `Close` (X) icon to remove the ingredient. Removal is immediate (no confirmation) and live totals update.

5. **Add Ingredient button**: `OutlinedButton` full-width. Text: "+ Add Ingredient". On tap: opens a bottom sheet or navigates to a sub-flow that reuses the food lookup methods (USDA search, OFF search, barcode scan, manual entry). This sub-flow is identical to the log method selection but scoped to adding an ingredient:
   - After selecting a food item and entering a weight, the ingredient (with per-100g reference values and weight) is added to the in-memory ingredient list in `CreateRecipeViewModel`.
   - The sub-flow does NOT show the entry confirmation screen. It returns directly to the recipe creation screen after weight entry.
   - Navigation: `recipes/create` opens a modal bottom sheet for method selection -> food search or manual entry -> weight entry for ingredient -> returns to recipe screen with ingredient added.

6. **Save Recipe button**: Full-width `FilledButton`. Text: "Save Recipe". Disabled if recipe name is empty or no ingredients are added. On tap: saves via `CreateRecipeViewModel`. The recipe's total nutritional values are computed from the ingredients (sum of each ingredient's scaled values). Navigates back to `recipes` (or `recipes/detail/{id}` for edits). Snackbar: "Recipe saved".

**Ingredient addition sub-flow**: To minimise navigation complexity, the ingredient lookup uses the same search/manual entry screens as meal logging but with a flag in the navigation arguments indicating "ingredient mode". In ingredient mode:
- The confirmation screen is skipped.
- After weight entry, the ingredient data is returned to `CreateRecipeViewModel` via a saved state handle or shared ViewModel pattern.

**States**:

| State    | Behaviour                                          |
|----------|----------------------------------------------------|
| Create mode | Empty name, empty ingredient list, totals at zero. Save disabled. |
| Edit mode (loading) | Shimmer while loading existing recipe data. |
| Edit mode (loaded) | Name and ingredients pre-populated. Totals reflect existing ingredients. |
| No ingredients | Totals card shows zeros. Save disabled. "Add ingredients to get started." helper text above the Add Ingredient button. |
| Has ingredients | Totals update live. Save enabled if name is also filled. |

---

### 3.14 Rolling Summaries (`summaries`)

**Purpose**: Display cumulative nutritional intake versus cumulative plan targets over 7-day and 28-day periods. Top-level tab destination.

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

2. **Tab row**: Material 3 `TabRow` with two tabs: "7 Days" and "28 Days". Selected tab uses `primary` indicator. Switching tabs reloads data for the respective period.

3. **Period label**: "dd/MM/yyyy -- dd/MM/yyyy" in `bodySmall` / `onSurfaceVariant`. The period always ends on today and extends backward 7 or 28 days.

4. **Metric sections**: Four sections (kcal, protein, carbs, fat), each containing:
   - Label in `titleSmall`.
   - "Intake: X / Target: Y" in `bodyMedium`. The target is the sum of per-day plan targets across the period (accounting for plan changes via `getPlanForDate()`).
   - `LinearProgressIndicator` with the metric's semantic colour. Track: `progressTrack`.
   - "Remaining: Z" in `bodySmall` / `onSurfaceVariant`. If over target: "Over: Z" in `overage` colour.

5. **Daily average row**: `Card` on `surfaceVariant` at the bottom. Shows the period total divided by the number of days. Formatted: "{kcal} kcal | P: Xg | C: Xg | F: Xg" in `bodyMedium`.

**States**:

| State    | Behaviour                                                    |
|----------|--------------------------------------------------------------|
| Loading  | Shimmer placeholders for all metrics.                        |
| No plan  | Metrics show intake values only. Target shows "No plan". Progress bars hidden. Text: "Set up a nutrition plan to see targets." |
| No entries in period | All intake values show 0. Progress bars at 0%. Daily average: 0. |
| Populated | As described.                                               |

---

### 3.15 Settings (`settings`)

**Purpose**: USDA API key management. Top-level tab destination.

**ViewModel**: `SettingsViewModel`

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

3. **About section**: Simple informational block. App name and version in `bodyMedium`. Privacy note: "Data stored locally on this device only." in `bodySmall` / `onSurfaceVariant`.

**States**:

| State    | Behaviour                                          |
|----------|----------------------------------------------------|
| Key stored | Status shows "Configured". Field masked. Clear Key button visible. |
| No key | Status shows "Not set". Field empty. Clear Key button hidden. |
| Key corrupted (EncryptedSharedPreferences failure) | Status shows "Not set". Snackbar: "Could not read stored key. Please re-enter." Field empty. |

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
3. User fills in food name, kcal, protein, carbs, fat (all per 100g).
4. User taps **"Next"**. -> `log/weight_entry`
5. User enters weight or taps quick-select chip. Preview updates.
6. User taps **"Confirm"**. -> `log/confirm`
7. User taps **"Save Entry"**. -> Pops to `daily_progress`.

**Total taps**: FAB + Manual option + Next + weight chip + Confirm + Save = **6 taps + typing 5 fields**.

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
4. User can: remove ingredients (tap X), add new ingredients (same sub-flow as creation), or change the recipe name.
5. User taps **"Save Recipe"**. -> Recipe updated. Navigates to `recipes/detail/{id}`.

### 4.8 Nutrition Plan Setup

1. User navigates to plan screen via "Plan" button on Daily Progress (or from the "No nutrition plan set" card).
2. User fills in kcal, protein, carbs, fat targets.
3. User taps **"Save Plan"**. -> Saved. Snackbar confirmation. Returns to Daily Progress.

### 4.9 Log Entry Deletion

1. User is on Daily Progress viewing today's entries.
2. User taps the **Delete** (trash) icon on a log entry.
3. Confirmation dialog appears: "Delete entry? {foodName} -- {kcal} kcal".
4. User taps **"Delete"**. -> Entry deleted. List updates. Progress recalculated.

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

**Usage**: Entry confirmation, weight entry preview, live recipe totals.

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

When the user changes their nutrition plan, the new plan takes effect immediately. The summaries screen correctly sums per-day targets using the plan that was active on each day within the period (via `getPlanForDate()`). This means a 7-day summary may reflect two different daily targets if the plan was changed within the last 7 days. The summaries screen does not display a notice about plan changes within the period -- the cumulative target simply reflects the correct summed value.

### 6.5 Incomplete API Data

When a food item from USDA or Open Food Facts is missing one or more of the four core nutritional values, the flow automatically routes through `log/missing_values` before reaching `log/weight_entry`. The user must fill in all missing values before proceeding. This is enforced by the `missingFields` property on `FoodSearchResult`.

### 6.6 Very Large Numbers

The formatting system uses UK locale with comma thousands separators. For extremely large kcal values (unlikely but possible with very heavy portions), the layout should not break. The `NutritionCard` and progress bar labels use flexible width containers. No truncation occurs; if text overflows, it wraps to a second line.

### 6.7 Rapid Entry

For users logging multiple items in quick succession, the return to `daily_progress` after saving an entry means the FAB is immediately available for the next entry. The flow is: Save -> pop to daily progress -> tap FAB -> start next entry. This is 1 extra tap between entries, which is the minimum possible given the confirmation requirement.

### 6.8 Data Retention

Log entries older than 2 years are automatically deleted by `DataRetentionWorker`. The user is never notified of this -- it happens silently in the background. Recipes are retained indefinitely and can be manually deleted by the user from the recipe detail screen.

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
