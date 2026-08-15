# Code Review — Review pass (UI layer: QuickWeightSelector / Weight Entry)

## Summary

**Scope reviewed**: Developer session `handoffs/developer-notes-01-ui.md` — rewrite of
`QuickWeightSelector` from a horizontally scrolling `LazyRow` to a wrapping `FlowRow`, with the
"100%" chip moved to the leading position, plus the new unit test suite
`QuickWeightSelectorTest.kt`. Verified against `handoffs/architecture.md` and `handoffs/design.md`
Revision 5 (2026-08-15, sections 3.9, 4.1–4.5, 5.5, 6.10, 7, 8.4, 8.7).

**Note on inputs**: The session prompt for this pass pointed to
`./handoffs/developer-notes-Review.md`, which does not exist in the repository. The only developer
notes present, and the only files touched per `git status` (`QuickWeightSelector.kt` modified,
`QuickWeightSelectorTest.kt` added), are `handoffs/developer-notes-01-ui.md` and
`handoffs/develop-01-ui.json`. This review pass treats `developer-notes-01-ui.md` as the session
under review, since it is the only developer output matching the working tree changes.

**Assessment**: The implementation matches design spec Revision 5 precisely — container type,
chip ordering (100% first, then ascending gram presets), spacing tokens (`Spacing.sm` on both
axes), no-scroll/no-clip behaviour, and the accessibility reading order. The extraction of
`quickWeightChips` into a pure function is a reasonable response to the project's lack of Compose
UI testing infrastructure, and the resulting 15 unit tests are meaningful — they exercise ordering,
value resolution, and selection logic rather than merely asserting existence. No architectural,
DI, or cross-layer issues were found. One Warning was raised for stale finding-ID references in
code comments (pre-existing, discovered during this pass, not introduced by this session); three
Observations are recorded, none blocking.

### Pass history

| Pass | Date | Open | Closed | Regressions | Notes |
|------|------|------|--------|-------------|-------|
| 1 (this pass) | 2026-08-15 | 4 | 0 | — | Initial review of the QuickWeightSelector wrap/reorder change. No prior findings file existed. |

---

## Critical Issues

None identified in this pass.

---

## Warnings

---

ID: W01 State: Open

Summary:

Several code comments in the UI layer reference issue-tracker finding IDs (`O##`, `W##`) instead
of describing the code's behaviour or rationale in plain terms, violating the project rule that
comments must not depend on the issue tracking document. None of these were introduced by the
session under review (`QuickWeightSelector.kt` and its new test are clean), but they are present
in the immediate call site of the reviewed component and in adjacent UI-layer files discovered
while assessing integration, so they are recorded here per the standing instruction to flag any
such comment found during review. Locations:

- `app/src/main/java/com/delve/hungrywalrus/ui/screen/addentry/WeightEntryScreen.kt:59` —
  `// Parse weight as Double then round to Int for chip selection (O14) and +/- buttons (W07).`
  (this is the direct call site of `QuickWeightSelector`).
- `app/src/main/java/com/delve/hungrywalrus/ui/screen/barcodescan/BarcodeScanScreen.kt:159` —
  `// identity changes (O13: defensive pattern matching EntryConfirmScreen's W03 fix).`
- `app/src/test/java/com/delve/hungrywalrus/ui/screen/addentry/AddEntryViewModelTest.kt:64, 578,
  601, 630, 657, 691` — multiple comments reference `W03` and `W07` by ID (e.g. "Simulates the W07
  fix...", "Per W03 the cache write is deferred...").
- `app/src/test/java/com/delve/hungrywalrus/ui/component/EditIngredientSheetFormatTest.kt:12, 88`
  — file-level doc comment and an inline comment reference `O10` by ID.

None of the referenced IDs (`O10`, `O13`, `O14`, `W03`, `W07`) resolve to any finding in this
document or any other review file present in the repository, so a future reader cannot look them
up — the comments are already orphaned references. Each should be reworded to describe the
behaviour/rationale directly (e.g. "rounds the parsed weight before comparing against chip values
so that decimal input from the +/- buttons still highlights the matching chip").

Resolution:

Open.

---

## Observations

---

ID: O01 State: Open

Summary:

`quickWeightChips` (`QuickWeightSelector.kt`) appends the `options` gram chips in the exact order
supplied by the caller and performs no internal sort. Design spec section 5.5 states "The
`options` values then follow in ascending order (default set: 25, 50, 100, 150, 200, 250)", which
reads as a description of the resulting order rather than an explicit sort requirement, and the
sole call site (`WeightEntryScreen.kt`) always uses the default ascending list, so there is no
observable defect today. This is recorded as a fragility point: if a future caller ever supplies
an unsorted or reordered list, the chip group will silently render out of the ascending order the
spec describes, since nothing in the component enforces it. Worth a one-line spec clarification or
an internal `sortedBy` if the intent is that the component itself guarantees ascending order
regardless of caller input.

Resolution:

Open.

---

ID: O02 State: Open

Summary:

`app/src/main/java/com/delve/hungrywalrus/ui/component/package-info.kt` still lists
`QuickWeightSelector` (along with `NutritionProgressBar`, `NutritionSummaryRow`, `NutritionCard`,
`ConfirmationDialog`, `FoodSearchResultItem`, `LogEntryItem`) under a `// TODO: Implement shared UI
components` comment, even though all of these are implemented and in active use elsewhere in the
package (including by this session's reviewed component). This is stale documentation, not
introduced by the reviewed session, but noticed while assessing the component's integration with
the rest of `ui/component/`. It has no functional effect but is misleading to a reader of the
package.

Resolution:

Open.

---

ID: O03 State: Open

Summary:

Developer notes (decision 2) flag a concern: with the "100%" chip now first, a reference weight
that coincides with a gram preset (e.g. a 100g recipe) renders both the "100%" chip and the "100g"
chip in selected state simultaneously, and ask whether this is desired. Verified against design
spec section 5.5 ("`selectedValue` renders the matching chip in `FilterChip` selected state") —
the spec does not restrict selection to a single chip, so the current behaviour (both chips
selected when both resolve to the same weight) is consistent with the spec as written, and the
developer's assessment that this is a spec decision rather than an implementation defect is
correct. The behaviour is covered by an explicit test
(`a reference weight equal to a gram preset selects both chips`). No code change needed; recorded
here so the open design question is visible to the design/product owner rather than silently
dropped.

Resolution:

Open.

---
