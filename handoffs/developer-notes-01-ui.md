# Developer Notes — Layer 01-ui (UI layer)

**Date:** 2026-08-15
**Scope:** Quick-select weight control on the Weight Entry screen (`log/weight_entry`) and its
shared component `com.delve.hungrywalrus.ui.component.QuickWeightSelector`.

## Request being implemented

"In the Log Food flow move the quick select button for 100% to the beginning of the list, and let
the list wrap instead of scrolling."

Per the plan for this layer, the architecture document required no change; the design spec was
updated first (design.md Revision 5, sections 3.9 elements 5-6, 4.1-4.5, 5.5, 6.10, 7, 8.4, 8.7)
and this session implements that revision.

## What was built

### 1. `QuickWeightSelector` rewritten as a wrapping chip group

- Container changed from a horizontally scrolling `LazyRow` to `androidx.compose.foundation.layout.FlowRow`.
  All chips are composed at once; the group wraps onto additional lines as the available width
  requires. No scroll state, no scroll indicators, no edge fades.
- `horizontalArrangement` and `verticalArrangement` are both `Arrangement.spacedBy(Spacing.sm)`
  (8dp), per design spec 5.5. The value is exposed as an internal constant
  `QuickWeightChipSpacing` so the token choice is assertable in a unit test.
- The "100%" chip is now emitted **first**, ahead of every entry in `options`. It remains
  conditional on `show100Percent && hundredPercentWeight != null`; when it is not shown the group
  simply begins with the first gram preset and nothing occupies the leading position.
- Component height is now variable. Nothing in the component constrains it, and the caller
  (`WeightEntryScreen`) already places it in a `verticalScroll` `Column` with no fixed height, so
  design spec 6.10 is satisfied with no change to the screen.

### 2. Chip ordering/selection extracted into a testable pure function

Composables cannot be exercised by the project's plain JVM unit tests (there is no
`compose-ui-test` dependency and the existing test suite is JVM/Robolectric only). To keep this
behaviour under test, the ordering and selection rules now live in an internal pure function:

```kotlin
internal fun quickWeightChips(
    options: List<Int>,
    selectedValue: Int?,
    show100Percent: Boolean,
    hundredPercentWeight: Double?,
): List<QuickWeightChip>
```

`QuickWeightChip` is an internal sealed interface with `Grams(weightG, selected)` and
`HundredPercent(weightG, selected)` variants carrying the chip label and selected state. The
composable maps each chip to a `FilterChip`, dispatching taps to `onSelect(weightG)` or
`onSelect100Percent()` exactly as before.

### 3. Behaviour deliberately preserved

- Public props and their semantics are unchanged: `options`, `selectedValue`, `onSelect`,
  `show100Percent`, `hundredPercentWeight`, `onSelect100Percent`, `modifier`.
- Selection rules are unchanged: a gram chip is selected when `selectedValue == weightG`; the
  100% chip is selected when `selectedValue?.toDouble() == hundredPercentWeight`.
- No ViewModel change. `AddEntryViewModel` still owns the weight state and supplies
  `hundredPercentWeight`; the scaled-nutrition preview and Confirm-button enablement still react
  to the weight field as before.
- `WeightEntryScreen` is untouched — the call site's arguments and layout already satisfy the
  revised spec.

Because `FlowRow` places children in composition order, the accessibility traversal order follows
the new visual order (100% first) with no explicit semantics work required (design spec 7).

## Key decisions and deviations

1. **`FlowRow` required an explicit `@OptIn(ExperimentalLayoutApi::class)`.** The layer scope
   stated `FlowRow` is stable in the Compose Foundation version pulled in by the pinned BOM
   (`compose-bom = 2025.02.00`). It is not: the compiler rejects the call without the opt-in
   ("The API of this layout is experimental and is likely to change in the future"). The opt-in is
   applied narrowly to the single composable and documented in a code comment. Everything else
   about the approach is per spec; no alternative layout was substituted.

2. **Simultaneous selection of "100%" and a gram chip is retained.** The architect flagged that
   with 100% first, a reference weight equal to a chip value (e.g. a 100g recipe) can render two
   chips selected at once. Design spec 5.5 states only that "`selectedValue` renders the matching
   chip in `FilterChip` selected state" and the layer scope says selected-state highlighting rules
   are unchanged, so the pre-existing behaviour is preserved: both chips highlight because both
   resolve to the same weight, which is accurate rather than misleading. This is covered by an
   explicit test. If a reviewer wants only one chip highlighted, that is a spec decision, not an
   implementation one.

3. **Fractional reference weights never highlight the 100% chip.** Pre-existing behaviour, carried
   over unchanged: `WeightEntryScreen` writes `totalWeightG.toInt()` into the weight field, so a
   recipe totalling 437.5g sets the field to 437 and the chip does not highlight. This is a
   pre-existing rounding wrinkle in the call site, outside this layer's scope; recorded here rather
   than fixed. It is documented by a test so the behaviour is not changed accidentally.

4. **The component is no longer lazy.** With 6-7 chips this is a non-issue and is what the spec
   requires.

## Tests

New file `app/src/test/java/com/delve/hungrywalrus/ui/component/QuickWeightSelectorTest.kt`
(15 tests, all passing). Coverage:

- **Ordering:** 100% chip rendered first when a reference weight is available; omitted (group
  starts at 25g) when `show100Percent` is false; omitted when the flag is set but
  `hundredPercentWeight` is null; dropping the chip leaves no gap or placeholder; gram chips follow
  the caller-supplied order; empty `options` yields only the 100% chip.
- **Value resolution:** gram chip resolves to its own weight; 100% chip resolves to the reference
  total weight (including a fractional one).
- **Selection:** gram chip matching the current weight is selected; 100% chip matching the current
  weight is selected; nothing is selected for a non-matching weight; nothing is selected when the
  weight field is empty/invalid (`selectedValue == null`); a fractional reference weight never
  selects the 100% chip; a reference weight equal to a gram preset selects both chips.
- **Layout tokens:** chip spacing uses the `sm` (8dp) token, applied to both axes.

Whole-suite result: `./gradlew build test` passes — 562 debug unit tests, 0 failures, 0 errors,
release build and lint clean.

## Integration concerns

- None for other layers. No domain, data, DI or navigation surface was touched.
- The only compile-time coupling introduced is on `androidx.compose.foundation.layout.FlowRow`,
  reached transitively via Material3/Foundation (consistent with the existing use of `LazyRow` and
  `rememberScrollState` in this module without an explicit foundation dependency). If a future
  BOM bump changes the experimental `FlowRow` signature, the breakage is confined to
  `QuickWeightSelector.kt`.

## Open questions / remaining work

- Whether the double-selected-chip case (decision 2) should instead suppress the gram chip's
  selected state. Needs a design decision, not a code change.
- `FoodCache` still has no `servingSizeG` field (design spec 8.4), so the 100% chip is in practice
  only reachable from the recipe-portion flow. Unchanged by this session, noted as pre-existing.
- Visual verification of the wrapped layout on a narrow device and at large font scales
  (design spec 6.10) requires a device/emulator run; it is not covered by JVM unit tests.

## Files changed

- **Modified:** `app/src/main/java/com/delve/hungrywalrus/ui/component/QuickWeightSelector.kt`
- **Added:** `app/src/test/java/com/delve/hungrywalrus/ui/component/QuickWeightSelectorTest.kt`
- **Added:** `handoffs/developer-notes-01-ui.md` (this file)
- **Added:** `handoffs/develop-01-ui.json`

No other files were added, modified, or removed.
