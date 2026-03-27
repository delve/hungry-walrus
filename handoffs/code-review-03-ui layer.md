# Code Review — Session 03: UI Layer (Today Screen Update)

Reviewer: Code Review Agent
Date: 2026-03-27
Branch: firstpass

---

## Summary

This session made two targeted changes: (1) `NutritionProgressBar.kt` gained an `isKcalBar` boolean parameter to split the label row into two private sub-composables with distinct typography, and (2) `DailyProgressScreen.kt` replaced the old three-column macro layout with three vertically stacked full-width rows and wired `isKcalBar = true` on the kcal bar.

Both changes are small and self-contained. The implementation is broadly correct and consistent with the architecture. No new tests were introduced, which is acceptable because the ViewModel and its state model are unchanged and the UI changes are purely layout/typographic. Two issues are raised below: one warning about a minor discrepancy between the kcal bar label value format and the design spec, and one observation about a dead code path in `Formatter.formatMacro`.

---

## Critical issues

None.

---

## Warnings

### W01 — `KcalLabelRow` omits the label prefix "Kcal:" that the design wireframe implies; actual impact is the format string, not a hard spec requirement

**Status**: Open

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/component/NutritionProgressBar.kt`, line 104

**Detail**: The design spec (section 3.1, element 2) shows the kcal row label as `"X / Y kcal"` (numbers and unit only) in `titleMedium`, which the implementation reproduces correctly. However the layout wireframe in the same section shows `"Kcal: 1,250 / 2,000  Remaining: 750"` with a `"Kcal: "` prefix on the left value. These two descriptions are in tension: the prose specification says numbers-only, the ASCII wireframe includes a prefix. The developer session notes correctly identify the prose spec as authoritative and the implementation follows it. However the inconsistency is worth flagging so the product owner can confirm which representation is intended, since it affects glanceability at a glance.

This is categorised as a warning rather than an observation because it is a spec ambiguity that could result in a product owner rejecting the implementation after it ships.

---

## Observations

### O01 — `Formatter.formatMacro` has a redundant branch in its if/else

**Status**: Open

**File**: `/app/src/main/java/com/delve/hungrywalrus/util/Formatter.kt`, lines 58–62

**Detail**: Both branches of the `if/else` in `formatMacro` produce identical output — `String.format(Locale.UK, "%.1f", rounded)`. The original intent was presumably to use `"%.0f"` for whole numbers and `"%.1f"` for half-values, but both branches ended up as `"%.1f"`. This is pre-existing code, not introduced in this session. It is flagged here because `NutritionProgressBar.kt` relies on `formatMacro` for the macro label rows introduced in this session. The practical effect is that whole-number macro values display as `"12.0g"` rather than a potentially more compact form — this aligns with the spec format `"12.5g"` (which implies one decimal place always) so there is no functional defect, but the dead branch is misleading and should be cleaned up.

### O02 — `isKcalBar` boolean parameter uses a type-unsafe pattern that would not scale beyond two modes

**Status**: Open

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/component/NutritionProgressBar.kt`, line 42

**Detail**: The developer session notes document the decision to use a boolean rather than a separate composable, citing avoidance of duplication in the progress indicator and semantics logic. This is reasonable for exactly two modes. If a third display mode were ever needed the boolean would need to be replaced with a sealed class or enum. The decision is sound for the current scope; this observation is recorded only to capture the trade-off for future reference.

### O03 — Progress summary section is not pinned when the entry list scrolls

**Status**: Open

**File**: `/app/src/main/java/com/delve/hungrywalrus/ui/screen/dailyprogress/DailyProgressScreen.kt`, lines 146–261

**Detail**: The design spec (section 3.1, element 2) states the progress summary section is "non-scrollable, pinned above the list". The current implementation places the progress summary `Column` and the `LazyColumn` (or empty-state `Box`) inside a plain outer `Column`. The `LazyColumn` has `fillMaxSize`, which means it consumes remaining space and does not scroll the progress bars off screen — so in practice the bars are visually pinned. However, the empty-state `Box` also uses `fillMaxSize`, which is correct. The structure is functionally consistent with the spec intent, but it is worth noting that if the progress section were ever made taller it could push the empty-state box off screen. Not a current defect but worth awareness.
