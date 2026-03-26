# Code Review: Session 02 — Domain Layer

**Reviewer**: Code Review Agent
**Date**: 2026-03-22
**Developer notes reviewed**: `handoffs/developer-notes-02-domain layer.md`
**Architecture reference**: `handoffs/architecture.md` (Revision 1, 2026-03-22)

---

## Scope

This review covers the domain layer as verified in session 02. No code changes were made
in this session; the layer was previously implemented and this session performed a
verification-only pass.

Files reviewed:

- `app/src/main/java/com/delve/hungrywalrus/domain/` — all models, use cases, and `OfflineException`
- `app/src/test/java/com/delve/hungrywalrus/domain/usecase/` — all six test classes

---

## Summary

The domain layer is clean, well-structured, and architecturally compliant. All eleven domain
models exactly match the architecture spec (sections 5.2, 6.3). All three use cases implement
their specified formulae correctly and carry `@Inject constructor()` for Hilt. No I/O, no
Android framework dependencies, and no layer violations are present in the domain package. Test
coverage is broad and the tests are meaningful.

All six findings from the first pass have been resolved in the Session 03 fix pass. O03 and O04
from the second pass have been resolved in the Session 04 fix pass. O05 from the third pass has
been resolved: all four duplicate tests were removed from `ComputeRollingSummaryUseCaseEdgeCaseTest`
in the Session 05 fix pass. There are no open findings.

---

---
## Review Pass 2 — 2026-03-22
Reviewer: Code Review Agent

---

## Findings

---

### [CRITICAL] C01 — `ComputeRollingSummaryUseCase` silently ignores days absent from `dailyPlans`

**File:** `app/src/main/java/com/delve/hungrywalrus/domain/usecase/ComputeRollingSummaryUseCase.kt:52-62`

**Issue:**

The loop iterates over every date from `start` to `end` and checks `dailyPlans[date]`. If a date
key is not present in the map at all (i.e. the ViewModel omitted it entirely), `dailyPlans[date]`
returns `null` and `planCoveredDays` is not incremented — identical to a date that is explicitly
mapped to `null`. This means `totalTarget` becomes `null` for the period, which is the correct
result. However, the code comment and KDoc only describe the explicit `null` case ("no plan was
configured for that day") and do not mention the absent-key case. This creates a silent contract
requirement on callers: the ViewModel must either include all dates in the map (mapping uncovered
days to `null`) or omit them entirely, and both behaviours produce the same result only because of
this implicit handling. If a future caller assumes that an absent key means "use a default plan"
rather than "no plan," the behaviour will differ silently from the documented intent.

More concretely: `SummariesViewModel` is responsible for building the `dailyPlans` map by calling
`getPlanForDate()` for each day in the period. If any day's `getPlanForDate()` call fails silently
(returns `null` without an error), the day is absent from the map or mapped to `null`. Both paths
produce the same result — but the ViewModel's contract with the use case is not enforced by the
type system. The spec (section 17.7) describes the intent precisely, but there is no guard at the
use case boundary to reject a `dailyPlans` map that does not contain an entry for every date in
`[start, end]`.

This is not a bug in the use case itself: the formula is correct given the inputs the use case
receives. The issue is that the use case places an implicit structural contract on `dailyPlans`
that is not expressed anywhere in the API signature, KDoc, or via a precondition check. A
`require(dailyPlans.keys.containsAll(datesBetween(start, end)))` guard (or explicit documentation
that absent keys are treated as null-plan days) would make this contract explicit and protect
against future caller bugs.

**Recommendation:** Add a KDoc clarification to the `dailyPlans` parameter: "Keys need not cover
every date in the period; an absent key is treated identically to a `null` value — that day has no
plan." This removes the ambiguity without changing behaviour. If strict coverage is ever required,
a precondition guard can be added at that point.

**Resolution:** Fixed in Session 03. The `dailyPlans` KDoc (line 18 of `ComputeRollingSummaryUseCase.kt`)
now reads: "Keys need not cover every date in the period; an absent key is treated identically to
a null value — that day has no plan." The contract ambiguity is resolved.

---

### [HIGH] W01 — `NutritionPlan.kcalTarget` is typed as `Int`; `NutritionValues.kcal` is `Double`; mixing causes an implicit widening accumulation in `ComputeRollingSummaryUseCase`

**File:** `app/src/main/java/com/delve/hungrywalrus/domain/model/NutritionPlan.kt:6`
**Also relevant:** `app/src/main/java/com/delve/hungrywalrus/domain/usecase/ComputeRollingSummaryUseCase.kt:56`

**Issue:**

`NutritionPlan.kcalTarget` is declared as `Int` (matching the architecture spec, section 5.2:
"Daily kilocalorie target — Int"). In `ComputeRollingSummaryUseCase`, the accumulation is:

```kotlin
targetKcal += plan.kcalTarget   // Int added to Double
```

Kotlin silently widens `plan.kcalTarget` from `Int` to `Double` here, which is safe at the
language level. However, the type inconsistency is a source of latent confusion: `totalTarget.kcal`
is a `Double` that was accumulated from `Int` daily targets, while all other `NutritionValues`
fields are accumulated from `Double` plan fields. The result of the kcal accumulation is always an
integer-valued `Double` (e.g. `14000.0` not `14000.3`), which behaves correctly but differs from
the other fields in precision semantics.

The validation rules in the architecture spec (section 5.2) state that `kcalTarget` must be
greater than zero — an integer-only constraint that is enforced at the ViewModel level. Using `Int`
for `kcalTarget` while the rest of the nutrition values use `Double` is mildly inconsistent.

**Recommendation:** This is not a bug. However, if `kcalTarget` is ever changed to `Double` to
align with the other target fields, the type should be updated consistently across `NutritionPlan`,
`NutritionPlanEntity`, all mappers, and the DAO query return type. For now, add a comment in
`ComputeRollingSummaryUseCase` at the accumulation point noting the `Int`-to-`Double` widening
is intentional.

**Resolution:** Fixed in Session 03. Line 57 of `ComputeRollingSummaryUseCase.kt` now carries the
inline comment: `// Int widened to Double intentionally; kcalTarget is Int per spec (section 5.2)`.

---

### [MEDIUM] W02 — `ValidateFoodDataUseCase` does not validate that override values are non-negative

**File:** `app/src/main/java/com/delve/hungrywalrus/domain/usecase/ValidateFoodDataUseCase.kt:28-54`

**Issue:**

`applyOverrides` accepts any `Double?` for each nutritional field. There is no guard against
negative values (e.g. `kcalPer100g = -50.0`). A negative per-100g reference value would later
produce a negative scaled result from `ScaleNutritionUseCase`, which in turn would produce a
negative log entry (negative kcal, negative macros). This would propagate into daily progress
totals and rolling summaries, producing incorrect results with no visible error.

By contrast, `ScaleNutritionUseCase.invoke` guards against negative `weightG` with `require`. The
same principle applies to per-100g reference values: they cannot be negative in physical reality.

The requirements state that macronutrient targets must be "zero or greater" and kcal targets must
be "greater than zero" (enforced at ViewModel level for plans). The same constraint logically
applies to per-100g food data.

**Recommendation:** Add `require(value >= 0.0)` guards for each override parameter in
`applyOverrides`, or document explicitly that non-negative values are the caller's responsibility
(and that the ViewModel is expected to reject negative user inputs before calling this method). If
the decision is to validate at the ViewModel, add a comment stating this in the use case.

**Resolution:** Fixed in Session 03. `ValidateFoodDataUseCase.kt` lines 39–42 now guard each
non-null override with `require(it >= 0.0)`. KDoc updated with `@throws IllegalArgumentException`
contract. Four new tests added to `ValidateFoodDataUseCaseEdgeCaseTest` verify the guards.

---

### [MEDIUM] W03 — `ScaleNutritionUseCase.invoke` does not validate per-100g inputs

**File:** `app/src/main/java/com/delve/hungrywalrus/domain/usecase/ScaleNutritionUseCase.kt:20-34`

**Issue:**

`invoke` guards against negative `weightG` but places no constraint on the per-100g input values
(`kcalPer100g`, `proteinPer100g`, `carbsPer100g`, `fatPer100g`). A negative per-100g value
produces a negative scaled result without any exception or warning. This is consistent with the
gap noted in W02 — the full data pipeline from `applyOverrides` through `ScaleNutritionUseCase`
into the log entry has no guard on the sign of per-100g reference values.

Unlike the `weightG` guard (which has an explicit `require` and a test), there is no test for
negative per-100g inputs in either `ScaleNutritionUseCaseTest` or `ScaleNutritionUseCaseEdgeCaseTest`.

**Recommendation:** Either add `require` guards for non-negative per-100g values in `invoke`, or
document that the caller contract requires non-negative inputs and add a corresponding test that
verifies the contract is not violated upstream. The latter is acceptable if the ViewModel reliably
rejects negative user input before reaching this point.

**Resolution:** Fixed in Session 03. `ScaleNutritionUseCase.kt` lines 32–35 now guard all four
per-100g parameters with `require(value >= 0.0)`. KDoc updated with `@throws` contract. Four new
tests added to `ScaleNutritionUseCaseEdgeCaseTest` verify the guards.

---

### [LOW] O01 — Duplicate test coverage between base test and edge-case test for `ScaleNutritionUseCase`

**File:** `app/src/test/java/com/delve/hungrywalrus/domain/usecase/ScaleNutritionUseCaseEdgeCaseTest.kt:62-71`
**Also:** `app/src/test/java/com/delve/hungrywalrus/domain/usecase/ScaleNutritionUseCaseTest.kt:81-90`

**Issue:**

Both test classes contain a test for negative `weightG` throwing `IllegalArgumentException`:

- `ScaleNutritionUseCaseTest.kt:81`: `invoke throws for negative weight`
- `ScaleNutritionUseCaseEdgeCaseTest.kt:62`: `negative weightG throws IllegalArgumentException`

Both tests use `weightG = -1.0` with identical inputs and assert the same exception. Similarly,
both test classes verify `scaleRecipePortion` throws for a zero recipe weight (test file line 108
vs edge case line 121). There are four total duplicate test scenarios across the two files.

Duplicate tests are not harmful but add maintenance noise: if the guard is changed (e.g. to allow
negative values in a future scenario), both tests must be updated.

**Recommendation:** Remove the duplicated negative-weight and zero-recipe-weight tests from
`ScaleNutritionUseCaseEdgeCaseTest` since they are already covered in `ScaleNutritionUseCaseTest`.
Reserve the edge-case class for scenarios not in the base class.

**Resolution:** Fixed in Session 03. The two originally duplicated tests (`negative weightG throws
IllegalArgumentException` and `zero recipe totalWeightG throws IllegalArgumentException`) were
removed from `ScaleNutritionUseCaseEdgeCaseTest`. See O03 for a new minor duplicate introduced
during the same fix pass.

---

### [LOW] O02 — `ComputeRollingSummaryUseCaseEdgeCaseTest` has a weaker test comment that misrepresents the use case contract

**File:** `app/src/test/java/com/delve/hungrywalrus/domain/usecase/ComputeRollingSummaryUseCaseEdgeCaseTest.kt:38-46`

**Issue:**

The test `total intake sums all entries regardless of date field on entry` includes a comment:

```
// Entries timestamps are not inspected by the use case; all entries in the list are summed.
```

This comment accurately describes the implementation, but it implicitly documents a contract
that the ViewModel must enforce: only entries within the date window should be passed to the use
case. If a ViewModel bug passes entries outside the window, the use case will silently include
them in the total with no error. The comment makes this sound like a feature rather than a
delegation of responsibility. Readers could reasonably interpret this as meaning entries from any
date are acceptable inputs, which would be incorrect from a product correctness standpoint.

**Recommendation:** Reword the comment to make the caller contract explicit: "The use case does
not filter entries by date — the ViewModel is responsible for passing only entries within the
`[start, end]` window." This preserves the design intent while making the responsibility boundary
clear.

**Resolution:** Fixed in Session 03. `ComputeRollingSummaryUseCaseEdgeCaseTest.kt` line 39 now
reads: "The use case does not filter entries by date — the ViewModel is responsible for passing
only entries within the [start, end] window."

---

### [LOW] O03 — New duplicate test for `negative portionWeightG` introduced during O01 fix pass

**File:** `app/src/test/java/com/delve/hungrywalrus/domain/usecase/ScaleNutritionUseCaseEdgeCaseTest.kt:131-139`
**Also:** `app/src/test/java/com/delve/hungrywalrus/domain/usecase/ScaleNutritionUseCaseTest.kt:92-106`

**Issue:**

The Session 03 fix pass removed two duplicated tests from `ScaleNutritionUseCaseEdgeCaseTest` (the
negative `weightG` and zero recipe weight tests) per O01. However, it also added four new
per-100g guard tests to the edge-case class, and one of the edge-case class's pre-existing tests
(`negative portionWeightG throws IllegalArgumentException`, line 131) now duplicates a test that
was already present in `ScaleNutritionUseCaseTest` (`scaleRecipePortion throws for negative portionWeightG`,
line 92). Both tests call `scaleRecipePortion` with a negative `portionWeightG` and expect
`IllegalArgumentException`.

This is a new duplication not present before the fix pass.

**Recommendation:** Remove `negative portionWeightG throws IllegalArgumentException` from
`ScaleNutritionUseCaseEdgeCaseTest` (line 131–139), as it is already covered by
`scaleRecipePortion throws for negative portionWeightG` in `ScaleNutritionUseCaseTest`.

**Resolution:** Fixed in Session 04. `ScaleNutritionUseCaseEdgeCaseTest.kt` no longer contains
a test named `negative portionWeightG throws IllegalArgumentException`. The file now ends at
line 131 with 10 tests, matching the Session 04 count.

---

### [LOW] O04 — Misleading test name in `ValidateFoodDataUseCaseEdgeCaseTest`

**File:** `app/src/test/java/com/delve/hungrywalrus/domain/usecase/ValidateFoodDataUseCaseEdgeCaseTest.kt:87-95`

**Issue:**

The test at line 87 is named `applyOverrides does not overwrite an existing non-null kcal with a
non-null override`. The name implies the original value is preserved when an override is supplied,
but the test body and its own inline comment contradict this:

```kotlin
// Per the implementation: override is used when non-null
assertEquals(200.0, result.kcalPer100g!!, 0.001)
```

The test actually asserts that the override value (`200.0`) replaces the original (`100.0`), which
is the opposite of what the name states. A developer reading only the test name would conclude that
non-null overrides on non-null fields are ignored, which is incorrect behaviour and contradicts
`ValidateFoodDataUseCaseTest.kt:79` (`applyOverrides does not replace existing value with null`)
which correctly tests the null-override-is-ignored path.

The test itself exercises valid and useful behaviour (a non-null override replaces a pre-existing
value). The name just needs to be corrected.

**Recommendation:** Rename the test to `applyOverrides replaces existing non-null kcal when a
non-null override is provided` or similar, to accurately describe what is being tested.

**Resolution:** Fixed in Session 04. `ValidateFoodDataUseCaseEdgeCaseTest.kt` line 87 now reads
`` `applyOverrides replaces existing non-null kcal when a non-null override is provided` ``,
accurately describing what the test body asserts.

---

---
## Review Pass 3 — 2026-03-24
Reviewer: Code Review Agent

---

### [LOW] O05 — Duplicate tests between `ComputeRollingSummaryUseCaseTest` and `ComputeRollingSummaryUseCaseEdgeCaseTest`

**Files:**
- `app/src/test/java/com/delve/hungrywalrus/domain/usecase/ComputeRollingSummaryUseCaseTest.kt`
- `app/src/test/java/com/delve/hungrywalrus/domain/usecase/ComputeRollingSummaryUseCaseEdgeCaseTest.kt`

**Issue:**

Four test scenarios are covered by both classes, repeating the same assertions with only cosmetic
differences in test name or fixture values:

1. **`throws when start is after end`** — identical name and identical assertion in both files
   (`ComputeRollingSummaryUseCaseTest.kt:144` and `ComputeRollingSummaryUseCaseEdgeCaseTest.kt:138`).
   Both call `useCase(emptyList(), emptyMap(), end, start)` and expect `IllegalArgumentException`.

2. **`startDate and endDate are preserved in result`** (base, line 127) vs **`result startDate and endDate match inputs`** (edge case, line 107).
   Both assert `result.startDate == start` and `result.endDate == end` on a 7-day window with no
   entries and no plans.

3. **`periodDays is inclusive of start and end`** (base, line 122) vs **`periodDays for 7-day window is exactly 7`** (edge case, line 114).
   Both assert `result.periodDays == 7` for the same date range.

4. **`returns null target when dailyPlans has no non-null values`** (base, line 76) vs **`totalTarget is null when dailyPlans is empty map`** (edge case, line 130).
   Both assert `result.totalTarget == null` when the plans map produces no non-null plan for any
   day of the period.

These duplicates follow the same maintenance-noise pattern noted in O01 and O03 for the
`ScaleNutritionUseCase` test classes.

**Recommendation:** Remove the four redundant tests from `ComputeRollingSummaryUseCaseEdgeCaseTest`
(`throws when start is after end`, `result startDate and endDate match inputs`,
`periodDays for 7-day window is exactly 7`, `totalTarget is null when dailyPlans is empty map`)
since each is already covered by `ComputeRollingSummaryUseCaseTest`. The edge-case class retains
distinct value through its 28-day window tests, mid-period plan-change tests, and the large-values
overflow test.

**Resolution:** Fixed. All four duplicate tests have been removed from
`ComputeRollingSummaryUseCaseEdgeCaseTest.kt`. The edge-case file now contains 7 tests (127 lines),
none of which overlap with the base class. The remaining tests cover distinct scenarios: the
28-day window accumulation, `totalTarget` nullability for a 28-day partial plan, a mid-period
plan-change accumulation, `periodDays` for a 28-day window, and a large-values overflow check.
The base class is unchanged at 10 tests.

---

---
## Review Pass 4 — 2026-03-24
Reviewer: Code Review Agent

No new findings. Both `ComputeRollingSummaryUseCaseTest.kt` and
`ComputeRollingSummaryUseCaseEdgeCaseTest.kt` were re-read in full. The four duplicates
identified in O05 are confirmed absent from the edge-case class. All other tests in both
files are distinct and exercise meaningful behaviour. There are no open findings in this
review.

---

## Items confirmed correct

The following areas were reviewed and found to match the architecture specification:

| Area | Verdict |
|---|---|
| All 11 domain models match architecture sections 5.2 and 6.3 exactly | Correct |
| `NutritionPlan.effectiveFrom` is `Long` (epoch millis) | Correct |
| `LogEntry` stores only final calculated values; no weight field | Correct |
| `RecipeIngredient` per-100g fields are non-nullable; pre-condition documented in KDoc | Correct |
| `FoodSearchResult.missingFields` is re-derived after every `applyOverrides` call | Correct |
| `FoodSource` enum: `USDA`, `OPEN_FOOD_FACTS`, `MANUAL` — matches spec | Correct |
| `NutritionField` enum: `KCAL`, `PROTEIN`, `CARBS`, `FAT` — matches spec | Correct |
| `OfflineException` extends `Exception` (not `RuntimeException`); default message is clear | Correct |
| `ScaleNutritionUseCase.invoke` formula: `(valuePer100g / 100.0) * weightG` | Correct |
| `ScaleNutritionUseCase.scaleRecipePortion` formula: `(total / totalWeight) * portion` | Correct |
| `ScaleNutritionUseCase` guards: `require(weightG >= 0.0)`, `require(recipe.totalWeightG > 0.0)` | Correct |
| `ComputeRollingSummaryUseCase` `totalTarget` is `null` unless every day in the period has a plan | Correct |
| `ComputeRollingSummaryUseCase` `periodDays` is inclusive of both `start` and `end` | Correct |
| `ComputeRollingSummaryUseCase` `require(!start.isAfter(end))` guard | Correct |
| All three use cases carry `@Inject constructor()` — no `@HiltViewModel` (correct; use cases are plain classes) | Correct |
| No I/O, no Android framework imports, no Coroutines in domain models or use cases | Correct |
| `RecipeWithIngredients` comment clarifies separation from Room `@Relation` class | Correct |
| `RollingSummary.totalTarget` KDoc accurately describes the null-when-any-day-missing semantics | Correct |
| Test count matches developer notes (53 domain tests, 277 total) | Correct |
| All test cases exercise meaningful behaviour, not just constructor or getter presence | Correct |
