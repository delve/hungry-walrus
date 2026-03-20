# Code Review: Session 02 — Domain Layer

*Reviewed: 2026-03-20 (post-second-fix-session)*

---

## Summary

This is the final review of the domain layer delivered in Developer Session 02, covering the state
of the code after two fix sessions. The files reviewed are:

**Source**
- `app/src/main/java/com/delve/hungrywalrus/domain/model/RollingSummary.kt`
- `app/src/main/java/com/delve/hungrywalrus/domain/usecase/ScaleNutritionUseCase.kt`
- `app/src/main/java/com/delve/hungrywalrus/domain/usecase/ValidateFoodDataUseCase.kt`
- `app/src/main/java/com/delve/hungrywalrus/domain/usecase/ComputeRollingSummaryUseCase.kt`

**Tests**
- `app/src/test/java/com/delve/hungrywalrus/domain/usecase/ScaleNutritionUseCaseTest.kt`
- `app/src/test/java/com/delve/hungrywalrus/domain/usecase/ValidateFoodDataUseCaseTest.kt`
- `app/src/test/java/com/delve/hungrywalrus/domain/usecase/ComputeRollingSummaryUseCaseTest.kt`

All four findings from the previous review (m1, s1, s2, s3) have been correctly resolved. The
domain layer is clean, architecturally aligned, and correctly implements the business rules from
the architecture document. No new findings of Critical or Major severity were identified. Two minor
observations are noted below for completeness; neither blocks the next session.

---

## Previous findings: resolution status

### m1 — Negative `portionWeightG` guard in `scaleRecipePortion`

**Resolved correctly.**
`require(portionWeightG >= 0.0) { "portionWeightG must not be negative" }` was added at line 44 of
`ScaleNutritionUseCase.kt`, before the existing `recipe.totalWeightG` guard. The ordering (portion
weight first, recipe weight second) is consistent with parameter order in the function signature.
The `@throws` KDoc block was updated to document the new guard. The guard is symmetric with the
one in `invoke`.

### s1 — Stale KDoc on `RollingSummary.totalTarget`

**Resolved correctly.**
`RollingSummary.kt` lines 8–9 now read: _"[totalTarget] is null when one or more days in the
period have no active nutrition plan. It is non-null only when every day in the period has a plan
entry."_ This accurately describes the `planCoveredDays == periodDays` contract implemented in
`ComputeRollingSummaryUseCase`.

### s2 — Single-day period test

**Resolved correctly.**
`ComputeRollingSummaryUseCaseTest.kt` lines 149–159 contain the test
`single-day period has periodDays 1 and dailyAverage equals totalIntake`. It passes `start ==
end == 2026-03-20` with a real entry, asserts `periodDays == 1`, and asserts each `dailyAverage`
field equals the corresponding `totalIntake` field. This correctly exercises the minimum boundary
of the `ChronoUnit.DAYS.between + 1` formula.

### s3 — Negative `portionWeightG` test

**Resolved correctly.**
`ScaleNutritionUseCaseTest.kt` lines 92–106 contain
`scaleRecipePortion throws for negative portionWeightG`, annotated
`@Test(expected = IllegalArgumentException::class)`, passing `portionWeightG = -1.0` with a valid
recipe. It is placed adjacent to the existing `scaleRecipePortion throws for zero recipe weight`
test, maintaining structural symmetry.

---

## New findings

### Critical

None.

### Major

None.

### Minor

#### n1 — `invoke` KDoc does not document the negative-weight guard

**Severity:** Minor
**File:** `app/src/main/java/com/delve/hungrywalrus/domain/usecase/ScaleNutritionUseCase.kt`
(lines 16–19)

The KDoc for the `invoke` operator describes the formula but has no `@throws` block, unlike
`scaleRecipePortion` which gained one as part of the s1/m1 fix. A caller reading the KDoc for
`invoke` cannot tell from documentation alone that passing a negative `weightG` throws
`IllegalArgumentException`; the only way to discover this is to read the function body or the
test. Given that `scaleRecipePortion` now documents its `@throws` contract, the two entry points
are inconsistent at the documentation level.

**Recommendation:** Add `@throws IllegalArgumentException if [weightG] is negative` to the KDoc
for `invoke`.

#### n2 — Partial-plan test uses `buildMap` but does not exhaust all seven dates

**Severity:** Minor
**File:** `app/src/test/java/com/delve/hungrywalrus/domain/usecase/ComputeRollingSummaryUseCaseTest.kt`
(lines 135–142)

The test `returns null target when only some days have a plan` builds a map covering only three
dates (indices 0..2) and passes it to `invoke` with the standard 7-day window. Because absent map
keys are treated as null by the use case, the remaining four dates are uncovered and `totalTarget`
is correctly null. This is a valid test.

However, the earlier test `returns null target when dailyPlans has no non-null values` (lines
76–83) constructs a map with two explicit `null` values for the first two days and an empty
implicit coverage for the remaining five. Both tests assert `assertNull(result.totalTarget)` and
both pass. The two tests overlap in what they cover and could be consolidated, or the first test
could be made more specific by asserting that an entirely empty map also yields null (which it does
but is not the stated intent of the test name). This is a readability concern only; the logic
under test is correct.

**Recommendation:** Consider renaming or consolidating the two null-target tests to make their
distinct intents clearer: one for the "completely empty map" case, one for the "partial coverage"
case.

---

## Verdict

**Approve.**

All previously identified findings have been resolved correctly. Business logic is correct, guard
conditions are symmetric, KDoc reflects the current contract, and test coverage is adequate across
boundary conditions including single-day periods, mid-period plan changes, partial plan coverage,
and negative input values. The two new minor observations are documentation and readability
suggestions that do not need to be addressed before proceeding to the ViewModel layer.
