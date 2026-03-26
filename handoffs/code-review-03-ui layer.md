# Code Review: Session 03 — UI Layer

**Reviewer**: Code Review Agent
**Date**: 2026-03-25
**Session reviewed**: Developer Session 03 — UI Layer Review
**Files reviewed**: All files under `app/src/main/java/com/delve/hungrywalrus/ui/`
and `app/src/main/java/com/delve/hungrywalrus/MainActivity.kt`

---

## Summary

Session 03 reviewed the full UI layer against Architecture Revision 1 and the Design Specification,
and fixed one real code gap in `ManualEntryScreen.kt` (ingredient mode). The fix is correctly
implemented and aligns with both the architecture and design documents. The wider UI layer is
well-structured, follows the MVVM pattern consistently, and the Hilt dependency injection wiring
is correct throughout.

Overall assessment: the layer is in good shape with no architectural violations. There are several
lower-priority issues — primarily a logic ordering bug in `ManualEntryScreen`, a missing Snackbar
on the `RecipeDetailScreen`, a UI state gap in `RecipeSelectScreen`, and some minor design-spec
deviations.

**Second pass (2026-03-25)**: C01, W01, W02, W04, and O07 have all been resolved. W03 was not
addressed — the developer fix notes describe a different issue under that label. W06 was accepted
as-is by the developer. One new observation (O11) was added for a minor inconsistency in the
navigation host.

**Third pass (2026-03-25)**: W03, W06, and O11 have all been resolved by fix-pass 2. The fixes
are correctly implemented and verified. One new observation (O12) was added for a minor
double-query in `buildDailyPlans`. All other findings remain at their existing statuses.
No new critical or warning findings were identified.

**Fourth pass (2026-03-26)**: O05 and O12 have both been resolved by fix-pass 3. The `isDirty`
flag in `CreateRecipeViewModel` is correctly implemented and the redundant Room query in
`SummariesViewModel.buildDailyPlans` has been eliminated. Two new observations (O13, O14) have
been added. No new critical or warning findings were identified.

**Fifth pass (2026-03-26)**: No findings have been fixed since the fourth pass. All remaining open
observations (O01–O04, O06, O08–O10, O13, O14) have been explicitly reviewed; those with developer
rationale accepted in fix-pass 3 are marked Deferred. O13 and O14, which have no accepted rationale
on record, are also marked Deferred given their low severity. One new warning (W07) has been added
for a functional bug in the `WeightEntryScreen` +/- increment buttons when the weight field contains
a decimal value.

**Sixth pass (2026-03-26)**: W07, O13, and O14 have all been resolved by fix-pass 4. The
`toDoubleOrNull()?.roundToInt()` replacement is correctly applied at all three call sites in
`WeightEntryScreen.kt` and both `LaunchedEffect(viewModel)` changes are in place. Three new
ViewModel tests verify the decimal weight behaviour. No new findings identified on fresh pass.
All remaining open findings are Deferred with accepted rationale; no critical or warning findings
remain open.

---

## Critical Issues

### C01 — `ManualEntryScreen`: `setDirectEntry` called before `setWeight` leaves inconsistent state on invalid weight input

**Status**: Resolved

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/manualentry/ManualEntryScreen.kt`, lines 181–194

In the ingredient-mode button handler the call order is:

```kotlin
viewModel.setDirectEntry(name, kcal, protein, carbs, fat)   // writes weightG = "100"
if (isIngredientMode && onIngredientAdded != null) {
    viewModel.setWeight(weightInput)                         // overwrites weightG
    onIngredientAdded()
}
```

`setDirectEntry` (in `AddEntryViewModel` line 224) sets `weightG = "100"` unconditionally as a
sentinel. `setWeight` then overwrites it with the user-supplied value. Because `allValid` guards
the button this sequence only executes when `weightInput` is a valid positive number, so the
observable outcome is correct in the success path.

However, if `setDirectEntry` is ever called in a context where `setWeight` is not subsequently
called (e.g. a future refactor removes the guard, or the `onIngredientAdded` lambda is null while
`isIngredientMode` is still true), the sentinel value "100" would silently persist. More
immediately, `getIngredientData` reads `state.weightG`, which at the moment `setDirectEntry`
returns is already "100" — if anything reads that intermediate state on the same thread tick it
sees stale data.

The safer call order is: call `setWeight(weightInput)` first, then `setDirectEntry`. This matches
the intent documented in the session notes ("calls `setWeight()` before `onIngredientAdded()`")
but the actual code calls `setDirectEntry` first, which then resets `weightG` back to "100" before
`setWeight` restores it.

**Resolution**: `setDirectEntry` now accepts an optional `weight` parameter (default `"100"`).
The `ManualEntryScreen` button handler passes `weight = weightInput` in ingredient mode, and the
single call computes `scaledNutrition` atomically using the supplied weight. The separate
`setWeight` call is eliminated. Two new tests verify the behaviour.

---

## Warnings

### W01 — `RecipeDetailScreen`: Snackbar "Recipe deleted" is never shown

**Status**: Resolved

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/recipes/RecipeDetailScreen.kt`, lines 57–65

The `LaunchedEffect` that collects `RecipeDetailUiEvent.RecipeDeleted` calls `onNavigateBack()`
immediately without displaying a Snackbar. The design specification (section 3.12) requires:
"On confirm: deletes via `RecipeDetailViewModel`, navigates back to `recipes`, Snackbar: 'Recipe deleted'."

The developer session notes acknowledge this as a minor deviation and accept it, reasoning that the
Snackbar would not be visible after navigation. That reasoning has merit for the current
implementation where the Snackbar host is inside the detail screen's `Scaffold`. However the
specification is clear, and the same pattern used in `CreateRecipeScreen` (show snackbar, then
navigate) demonstrates that the brief display before navigation is intentional product behaviour.
This should be raised for a product decision rather than silently accepted by the developer.

**Resolution**: `snackbarHostState.showSnackbar("Recipe deleted")` is now called before
`onNavigateBack()` in the `RecipeDeleted` branch (line 61). The snackbar host is wired into
the screen's `Scaffold` at line 96. Fix is correct.

### W02 — `RecipeSelectScreen`: Loading state not implemented; stale empty state shown during initialisation

**Status**: Resolved

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/addentry/RecipeSelectScreen.kt`, lines 56–72

`RecipeSelectScreen` observes `viewModel.recipes`, which is a `StateFlow` backed by
`SharingStarted.WhileSubscribed(5_000)` with an initial value of `emptyList()`. Before Room emits
the first result there is a brief window where `recipes.isEmpty()` is `true`, so the "No recipes
saved" empty-state text flashes on screen even when recipes exist. The design specification
(section 3.8) defines a distinct Loading state with a `CircularProgressIndicator`. The current
code collapses Loading and Empty into the same branch, which deviates from the spec.

**Resolution**: `RecipesState` sealed interface introduced in `AddEntryViewModel`. Initial
`StateFlow` value is `RecipesState.Loading`; emissions are mapped to `RecipesState.Loaded`.
`RecipeSelectScreen` now uses a `when` expression showing a `CircularProgressIndicator` for the
`Loading` branch. Two new tests cover both states.

### W03 — `EntryConfirmScreen`: `events` channel collected inside `LaunchedEffect(Unit)` without cancellation safety

**Status**: Resolved

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/addentry/EntryConfirmScreen.kt`, line 42

The `events` flow is a `Channel.receiveAsFlow()`. Collecting it inside `LaunchedEffect(Unit)` means
the collector is created once per composition entry and is never re-started if `viewModel` changes
identity (which can happen when the ViewModel scope changes — see the conditional-scope pattern in
`HungryWalrusNavHost`). If the `LaunchedEffect` exits because composition leaves the screen and
returns, any buffered events emitted while the collector was absent will be delivered on re-entry,
which could trigger unexpected navigation. The established pattern in this codebase (e.g.
`CreateRecipeScreen`, `RecipeDetailScreen`, `SettingsScreen`) also uses `LaunchedEffect(Unit)`, but
those ViewModels are scoped to specific composable lifetimes so re-entry is less likely. For
`EntryConfirmScreen`, which sits at the end of a graph that can be re-entered, this pattern warrants
a note.

**Note**: The developer fix-pass notes describe a "W03" as "WeightEntryScreen back-navigation
inconsistency" and mark it deferred. That does not correspond to this finding. The code in
`EntryConfirmScreen.kt` is unchanged from the first pass and the concern here remains open.

**Resolution**: Changed `LaunchedEffect(Unit)` to `LaunchedEffect(viewModel)` at line 42 of
`EntryConfirmScreen.kt`. The effect is now cancelled and restarted whenever the ViewModel instance
changes, consistent with the defensive pattern. Confirmed in current code.

### W04 — `BarcodeScanScreen`: `setTargetResolution` is deprecated in CameraX 1.3+

**Status**: Resolved

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/barcodescan/BarcodeScanScreen.kt`, line 239

```kotlin
.setTargetResolution(Size(1280, 720))
```

`ImageAnalysis.Builder.setTargetResolution()` was deprecated in CameraX 1.3 in favour of
`setResolutionSelector`. The architecture mandates CameraX 1.4.x (section 15). At 1.4.x this API
still functions but produces deprecation warnings and may be removed in a future release. This
should be migrated to `ResolutionSelector` with a `ResolutionStrategy`.

**Resolution**: Replaced with `setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(...).build())`
using `ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER`. Required imports added.

### W05 — `DailyProgressScreen`: progress section shown without a progress bar for kilocalories when plan exists but total is zero

**Status**: Ignored — investigated and withdrawn. The code correctly matches the design spec's "Empty (no entries, plan exists)" state. No action required.

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressScreen.kt`, lines 192–199

When a plan exists but no entries have been logged today, the screen shows the Kcal `NutritionProgressBar`
at the top alongside three macro bars. This matches the design spec. However `NutritionProgressBar` at
`target > 0` and `current == 0` renders an empty bar and "Remaining: X kcal". The design spec (section 3.1)
says the empty state text "No entries today. Tap + to log food." should appear in the list area. It does
appear in the list area correctly. The progress bars are also shown (which matches the spec for the
"Empty (no entries, plan exists)" state). This is not a bug, but it is worth confirming that the
progress section above the empty-list text matches spec intent — it does, since the design shows
"all at 0%" for this state.

Withdrawing as a finding — the code is correct. No action required.

### W06 — `SummariesViewModel`: `buildDailyPlans` makes N suspend calls in a loop on the main coroutine dispatcher

**Status**: Resolved

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/summaries/SummariesViewModel.kt`, lines 88–98

`buildDailyPlans` iterates over each date in the period (up to 28 iterations) and calls
`planRepo.getPlanForDate(date)` for each, which is a suspend function that issues a Room database
query. These calls are sequential. For 28 days this means 28 sequential Room calls per summary load.
Each call is fast, but the cumulative overhead is avoidable. There is no architectural violation
here, but the pattern does not align with the architecture's preference for efficient data access.
This is flagged as a warning rather than a critical issue because the latency is negligible on device.

**Resolution**: Replaced the sequential loop with `coroutineScope { }` + parallel `async {}` blocks.
All per-date queries and the fallback-plan query are launched concurrently and then awaited. A new
test `buildDailyPlans queries planRepo for all dates in the 7-day period` verifies the parallel
implementation covers all dates. Confirmed in current code at lines 88–98 of `SummariesViewModel.kt`.

### W07 — `WeightEntryScreen`: +/- increment buttons use `toIntOrNull()` causing weight field to reset to "1" on decimal input

**Status**: Resolved

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/addentry/WeightEntryScreen.kt`, lines 107 and 135

```kotlin
// Decrement button (line 107)
val current = uiState.weightG.toIntOrNull() ?: 0
if (current > 1) {
    viewModel.setWeight((current - 1).toString())
}

// Increment button (line 135)
val current = uiState.weightG.toIntOrNull() ?: 0
viewModel.setWeight((current + 1).toString())
```

The weight field accepts `KeyboardType.Decimal` and the input filter at line 121 permits the `.`
character. When the user types a decimal value such as "100.5" then taps the `+` button:
`toIntOrNull()` returns `null`, the fallback `?: 0` is used, and `setWeight("1")` is called. The
weight field is overwritten with "1", discarding the user's decimal input silently.

The `-` button has the same issue: `current` becomes 0, the `if (current > 1)` guard fires, and
no decrement occurs — so the `-` button appears to do nothing when the field holds a decimal value.
The `+` button behaviour is a functional regression that destroys user input; the `-` behaviour is
merely unresponsive. Both should use `toDoubleOrNull()` (or `toDoubleOrNull()?.let { it.toInt() }`
after rounding) to be consistent with the rest of the weight handling in this screen.

Note that the same `toIntOrNull()` pattern at line 58 (used only for chip highlight selection) was
previously flagged as O14 and described there as cosmetic. The `+`/`-` button instances at lines
107 and 135 are more severe because they actively corrupt the weight value rather than merely
failing to highlight a chip.

**Resolution**: All three `toIntOrNull()` calls in `WeightEntryScreen.kt` replaced with
`toDoubleOrNull()?.roundToInt()` (lines 62, 111, 139). The `kotlin.math.roundToInt` import is
present. The fix also resolves O14 as a side effect. Three new tests in `AddEntryViewModelTest`
verify the decimal weight handling via the ViewModel's `setWeight` function.

---

## Observations

### O01 — `ManualEntryScreen`: use of `Button` instead of `FilledButton`

**Status**: Deferred — consistent across all screens in the codebase. In Compose Material 3, `Button` is the filled button component. Developer rationale from fix-pass 3 is sound; no functional gap.

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/manualentry/ManualEntryScreen.kt`, line 180

The design specification (section 3.6) specifies a `FilledButton` for the Next/Add Ingredient
button. The code uses `Button`, which is the Material 3 filled button component (its type is
`FilledButton` in Material 3 semantics). In Compose Material 3, `Button` renders identically to
what the spec calls `FilledButton`. This is not a functional issue but it is worth confirming that
the same convention is used consistently. Checking other screens: `WeightEntryScreen`,
`EntryConfirmScreen`, and `MissingValuesScreen` all use `Button` for the primary action. The usage
is consistent across the codebase.

### O02 — `DailyProgressScreen`: top app bar layout packs date into the `title` slot

**Status**: Deferred — layout quality observation only; no functional defect and no architectural violation. The current implementation achieves the visual intent. Developer rationale from fix-pass 3 is sound.

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressScreen.kt`, lines 84–103

The title slot contains a `Row` with "Today" on the left and the date on the right, plus a `TextButton`
"Plan" in the `actions` slot. The design spec (section 3.1) describes the date as right-aligned
in `bodySmall / onSurfaceVariant` and the "Plan" text button at the trailing edge. The current
implementation achieves the visual intent, but squeezing the date into the `title` slot of a
`TopAppBar` rather than the `actions` slot means the date could be truncated on narrow displays
where the actions slot already contains the Plan button. The architecture does not prescribe specific
`TopAppBar` slot usage, so this is an observation only.

### O03 — `LogMethodScreen`: USDA unavailable subtitle differs slightly from design spec

**Status**: Deferred — documented accepted deviation. "tap to configure" is more accurate than the spec's "configure in Settings" given the in-flow dialog behaviour. Developer rationale from fix-pass 3 is sound.

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/addentry/LogMethodScreen.kt`, line 83

The subtitle shown when no USDA key is configured is:
`"API key required -- tap to configure"`

The design specification (section 3.3) states:
`"API key required -- configure in Settings"`

The session notes document this as an accepted deviation (in-flow dialog rather than navigating to
Settings). The subtitle wording should match the actual behaviour — since the tap shows an in-flow
dialog rather than navigating to Settings, "tap to configure" is more accurate. No further action
required; documented for completeness.

### O04 — `BarcodeScanScreen`: hardcoded `200.dp` offset for instruction text positioning

**Status**: Deferred — layout polish issue only; no functional defect. Relative positioning requires the cutout rect at runtime, which is a non-trivial change. Developer rationale from fix-pass 3 is sound.

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/barcodescan/BarcodeScanScreen.kt`, line 336

```kotlin
modifier = Modifier
    .align(Alignment.Center)
    .padding(top = 200.dp)
```

The instruction text "Align barcode within the frame" is positioned with a hardcoded 200dp top
padding relative to the centre of the Box. On different screen sizes and aspect ratios this text may
not align below the scanning cutout as intended. The spec (section 3.5) states the text should be
"positioned below the scanning area cutout". A relative positioning approach tied to the cutout
dimensions would be more robust. This is a layout quality issue rather than a functional defect.

### O05 — `CreateRecipeScreen`: discard dialog does not fire when in edit mode with unmodified data

**Status**: Resolved

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/createrecipe/CreateRecipeScreen.kt`, lines 115–122

```kotlin
if (uiState.ingredients.isNotEmpty() || uiState.recipeName.isNotBlank()) {
    showDiscardDialog = true
} else {
    onClose()
}
```

When the user opens an existing recipe for editing, `ingredients` will be non-empty and
`recipeName` will be non-blank after loading, so the discard dialog fires even if the user makes
no changes. The spec (section 3.13) says: "Tapping shows discard confirmation dialog: 'Discard
changes?'". The intent is to show the dialog when there are unsaved changes, not always when
the form is non-empty. In practice this is a minor UX issue for the edit flow — the dialog
appears one extra time even on a clean edit-then-close. Tracking dirty state explicitly would
address this.

**Resolution**: `isDirty: Boolean = false` added to `CreateRecipeUiState`. The three user-mutation
functions (`setRecipeName`, `addIngredient`, `removeIngredient`) set `isDirty = true`. `loadExistingRecipe`
does not set `isDirty`, so it stays `false` after loading. `CreateRecipeScreen` close-button handler
now checks `uiState.isDirty` at line 116. Confirmed in current code. Three new tests added in
`CreateRecipeViewModelTest`.

### O06 — `SummariesScreen`: `LaunchedEffect(Unit)` triggers `reloadSummary()` causing a visible Loading flash on every tab visit

**Status**: Deferred — architecturally required behaviour (section 7.5 mandates reload on each visit). Eliminating the flash would require keeping stale state visible during load, which is a deliberate product trade-off. Developer rationale from fix-pass 3 is sound.

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/summaries/SummariesScreen.kt`, lines 43–45

`reloadSummary()` sets `_uiState.value = SummariesUiState.Loading` before starting the new collection
job. On every tab switch the user sees a brief `CircularProgressIndicator`. This is architecturally
required (per architecture section 7.5 — data must reload on each visit), but the loading flash
could be avoided by keeping the previous state visible while loading begins. This is a UX polish
observation, not a correctness issue.

### O07 — `SettingsScreen`: `VisualTransformation` applied when `keyInput` is empty and key exists, but placeholder already shows masked key

**Status**: Resolved

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/settings/SettingsScreen.kt`, lines 154–158

```kotlin
visualTransformation = if (keyInput.isEmpty() && uiState.hasKey) {
    PasswordVisualTransformation()
} else {
    VisualTransformation.None
}
```

When a key is stored and `keyInput` is empty, `PasswordVisualTransformation` is applied to an
empty field, which has no visible effect (nothing to transform). The masked key is shown via the
`placeholder`. This logic is harmless but slightly confusing — `PasswordVisualTransformation` on an
empty field does nothing, so the condition could be simplified to `VisualTransformation.None`
always, relying only on the placeholder for masked display when empty.

**Resolution**: `visualTransformation` is now `VisualTransformation.None` unconditionally (line 153).
The unused `PasswordVisualTransformation` import has been removed.

### O08 — `AddEntryViewModel`: `setDirectEntry` stores user-entered values as `kcalPer100g` / per-100g fields on the `FoodSearchResult`

**Status**: Deferred — architecture section 7.3 explicitly allows this sentinel pattern. The comment in the code explains the invariant. Developer rationale from fix-pass 3 is sound; no change required.

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/addentry/AddEntryViewModel.kt`, lines 209–227

For a regular (non-ingredient) manual entry the user enters final as-consumed values, but
`setDirectEntry` stores them in `FoodSearchResult.kcalPer100g` etc. (fields named "per 100g").
This is documented with a comment explaining the sentinel weight of "100" makes the scaling
maths work out. The approach is functional, but the naming mismatch between the field semantics
("per 100g") and what is actually stored ("consumed total") is a hidden assumption that could
cause confusion for future maintainers. This is an observation for future consideration — the
architecture explicitly allows this pattern (section 7.3) so no change is required now.

### O09 — `BarcodeScanScreen`: camera setup done inside `AndroidView` `factory` lambda rather than a `SideEffect` or `LaunchedEffect`

**Status**: Deferred — style observation only. The current implementation is correct: `factory` runs once, `torchOn` is handled via a separate `LaunchedEffect`, and `scanning` reads correctly at callback time. Developer rationale from fix-pass 3 is sound.

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/barcodescan/BarcodeScanScreen.kt`, lines 224–288

The entire `ProcessCameraProvider` listener, `Preview`, and `ImageAnalysis` setup is nested inside
the `factory` lambda of `AndroidView`. The `factory` lambda runs only once (on first composition),
which is the correct behaviour for CameraX setup. However, any changes to the `scanning` or `torchOn`
state variables that are captured by the lambda will not reflect updates after the factory runs —
`torchOn` is handled via a separate `LaunchedEffect` on `cameraRef.value` so that is fine. The
`scanning` flag is read inside the analyser callback (line 243) and is a `var` captured by the
lambda; because it is a `Boolean` primitive captured by reference at the call site this should
reflect the current value at callback time on Android. However, for future readability it would be
clearer to use a `MutableState<Boolean>` for `scanning` rather than a `rememberSaveable` `var`, to
make the reactive capture intent explicit. This is a style observation.

### O10 — Minor: `RecipeSelectScreen` missing `TopBar` padding on list top item

**Status**: Deferred — consistent with other list screens in the codebase. Design spec does not mandate a top spacer. Developer rationale from fix-pass 3 is sound.

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/addentry/RecipeSelectScreen.kt`, lines 72–116

The `LazyColumn` in `RecipeSelectScreen` applies `padding(padding)` from scaffold but has no top
`Spacer` before the first recipe card. Other list screens (e.g. `RecipeListScreen`) similarly omit
a top spacer, so this is consistent within the codebase. The design spec does not explicitly mandate
a top spacer. Flagging for awareness only.

### O11 — `HungryWalrusNavHost`: `LOG_WEIGHT_ENTRY` composable duplicates `recipeBackStackEntryOrNull()` logic inline

**Status**: Resolved

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/navigation/HungryWalrusNavHost.kt`, lines 317–319

```kotlin
val recipeBackStackEntry = remember(backStackEntry) {
    navController.findBackStackEntry(Routes.RECIPE_CREATE)
        ?: navController.findBackStackEntry(Routes.RECIPE_EDIT)
}
```

The private helper `recipeBackStackEntryOrNull()` exists precisely to encapsulate this pattern
(line 43). Six other composables in the same file call `navController.recipeBackStackEntryOrNull()`
correctly. The `LOG_WEIGHT_ENTRY` composable at line 317 inlines an equivalent two-call expression
directly instead of using the helper. If the recipe routes ever change (e.g. a third recipe edit
variant is added), this inline copy would not be updated automatically. The fix is to replace the
inline expression with a call to `recipeBackStackEntryOrNull()`, consistent with the rest of the
file.

**Resolution**: Replaced the two-call inline expression with a single call to
`navController.recipeBackStackEntryOrNull()` at line 318 of `HungryWalrusNavHost.kt`. Confirmed
in current code.

### O12 — `SummariesViewModel.buildDailyPlans`: `today` date is queried twice when it falls within the period window

**Status**: Resolved

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/summaries/SummariesViewModel.kt`, lines 93–97

```kotlin
val fallbackPlan = async { planRepo.getPlanForDate(today) }
val dates = generateSequence(start) { d -> d.plusDays(1).takeUnless { it.isAfter(end) } }.toList()
val planResults = dates.map { date -> date to async { planRepo.getPlanForDate(date) } }
```

`today` is always `end`, so it is always the last element of `dates`. This means
`planRepo.getPlanForDate(today)` is launched as two concurrent `async` coroutines — one for the
fallback and one as part of the `dates` map. For the common case (7-day and 28-day periods, where
`today` is always within the window) this results in one redundant Room query per `buildDailyPlans`
call. The `SummariesViewModelTest` comment at line 228 acknowledges this: "today is within the
window so it is queried at least twice — once in the dates list and once for the fallback async".

The redundant query has no correctness impact and negligible performance cost for a single date
lookup. The fix would be to check whether `today` is already in `dates` and reuse that deferred
rather than launching a second one. This is noted for completeness; it does not need to be
addressed unless the query overhead becomes measurable.

**Resolution**: Removed the standalone `fallbackPlan` async block. The fallback is now obtained by
finding today's entry in `planResults` and reusing its deferred, with a defensive `?: async { ... }`
branch for the edge case where `today` is not in `dates`. Total queries for a 7-day period drop
from 8 to 7. A new test `buildDailyPlans does not issue a redundant query for today` verifies
exactly 7 calls for a 7-day period. Confirmed in current code at lines 93–100 of `SummariesViewModel.kt`.

### O13 — `BarcodeScanScreen` and `CreateRecipeScreen`: `LaunchedEffect(Unit)` used for events channel where ViewModel is passed as parameter

**Status**: Resolved

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/barcodescan/BarcodeScanScreen.kt`, line 159;
`/app/src/main/java/com/delve/hungrywalrus/ui/screen/createrecipe/CreateRecipeScreen.kt`, line 84

Both screens receive their ViewModel as a composable parameter (not created internally via
`hiltViewModel()`). In `BarcodeScanScreen` the ViewModel is resolved from a conditional parent
scope in `HungryWalrusNavHost` (lines 217–252), which in theory can produce a different ViewModel
instance on re-entry. In `CreateRecipeScreen` the ViewModel is provided by the nav host caller
but is always created via `hiltViewModel()` scoped to the back-stack entry, so its identity is
stable for the lifetime of that entry.

For `BarcodeScanScreen`, using `LaunchedEffect(Unit)` means the events collector is never
restarted if the ViewModel instance changes. The fix applied in W03 (`LaunchedEffect(viewModel)`)
should also be applied here for consistency and safety, matching the defensive pattern. For
`CreateRecipeScreen` the risk is lower because the ViewModel identity is stable, but applying
the same pattern is good hygiene.

Note: `RecipeDetailScreen` (line 57) and `SettingsScreen` (line 79) also use `LaunchedEffect(Unit)`
for events, but in both cases the ViewModel is created directly via `hiltViewModel()` inside the
composable, so the identity cannot change — `LaunchedEffect(Unit)` is acceptable there.

**Resolution**: Changed `LaunchedEffect(Unit)` to `LaunchedEffect(viewModel)` in both files.
`BarcodeScanScreen.kt` line 160 and `CreateRecipeScreen.kt` line 85 both confirmed in current code.

### O14 — `WeightEntryScreen`: `toIntOrNull()` used to parse decimal weight for chip selection

**Status**: Resolved

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/addentry/WeightEntryScreen.kt`, line 58

```kotlin
val weightVal = uiState.weightG.toIntOrNull()
```

The weight input field uses `KeyboardType.Decimal` and accepts fractional values (e.g. "100.5g").
`toIntOrNull()` returns `null` for any non-integer string, so `QuickWeightSelector` receives a
`null` `selectedValue` whenever the user types a decimal weight. As a result no chip is highlighted
even when the typed value precisely matches a chip option like "100" typed as "100.0". This is a
minor usability inconsistency — the chip highlight is cosmetic only and does not affect the actual
weight or nutrition calculation, which uses `uiState.weightG` directly.

**Resolution**: Resolved as part of the W07 fix. Line 62 of `WeightEntryScreen.kt` now reads
`val weightVal = uiState.weightG.toDoubleOrNull()?.roundToInt()`. Confirmed in current code.

---

*Note: W05 was investigated and withdrawn during drafting — the code correctly matches the design spec's "Empty (no entries, plan exists)" state.*
