# Code Review: Session 01 — Data Layer (Round 3)
*Reviewed: 2026-03-20*

## Summary

This is a post-fix review of the data layer following Fix Session 03. All findings from Round 2 have been resolved or have a recorded Product Owner override. The implementation is structurally sound: the architecture document's patterns are followed consistently, the Room schema is correctly defined, DI is wired correctly, and the three previously-critical behavioural bugs (day-boundary gap, non-atomic recipe mutations, double API key) remain fixed. The `isNetworkAvailable()` TOCTOU preflight has been removed from `FoodLookupRepositoryImpl`, search-result caching has been removed to align with the architecture spec, and the `getCurrentPlan` snapshot limitation is documented on both the interface and the implementation.

One new minor issue is identified: the `FoodLookupRepositoryImpl.lookupBarcode` method returns a stale expired cache entry on `HttpException` (e.g. HTTP 4xx/5xx), which is inconsistent with its behaviour on `IOException` where a fresh API result would have been preferred. This is a small correctness deviation worth fixing. A second minor issue is that `DatabaseModule` does not scope its DAO `@Provides` methods with `@Singleton`, which is technically harmless given Room returns the same DAO instance internally, but is inconsistent with the rest of the DI configuration. The test count discrepancy between the Fix Session 03 notes (45) and the Verification section (38) is a documentation issue in the developer notes rather than a code problem; the actual source contains 16 + 8 + 5 + 5 + 5 + 6 = 45 data-layer tests.

Severity counts: Critical: 0 / Major: 0 / Minor: 3

---

## Findings

### Critical

None.

---

### Major

None.

---

### Minor

#### m1 — `lookupBarcode` returns stale cache on `HttpException` even when the cache entry was already identified as expired

**File:** `app/src/main/java/com/delve/hungrywalrus/data/repository/FoodLookupRepositoryImpl.kt` (lines 78–83)

**Description:** At lines 59–61, `lookupBarcode` checks the cache first: if a valid non-expired entry exists it is returned immediately. If the entry is expired (or missing), execution falls through to the network call. If the network call throws `HttpException`, the catch block at lines 78–83 returns `cached.toDomain()` if `cached != null`. The `cached` variable here may hold an expired entry — the same entry that was deemed stale at line 59 and intentionally bypassed. This means an HTTP 4xx or 5xx response causes a silent fallback to expired data, with no indication to the caller that the data is potentially stale.

By contrast, the `IOException` path (lines 72–77) uses identical logic and also returns the stale cache. For `IOException` this is the correct behaviour per architecture section 6.2 ("On network failure: Return cached data if available"). However, an `HttpException` represents a server response — the server was reachable, so the offline justification does not apply. Returning stale data on a 404 (product not found) or 500 (server error) is semantically different from returning it when offline.

In practice the most likely `HttpException` from the OFF barcode endpoint is a 404, which should arguably be treated the same as `status == 0` (product not found, return `Result.success(null)`). The current code instead returns the stale cached product, giving the user incorrect data.

**Recommendation:** Distinguish the error type before deciding whether to fall back to the stale cache. For `HttpException`, consider returning `Result.success(null)` for 404 (product not found), or `Result.failure(Exception(mapHttpError(e.code())))` for server errors, rather than silently serving expired cached data. Alternatively, only fall back to a stale cache for `IOException` (offline) and let all `HttpException` paths propagate failures to the caller, reserving the stale cache fallback exclusively for the offline case.

---

#### m2 — DAO provider methods in `DatabaseModule` are not `@Singleton` scoped

**File:** `app/src/main/java/com/delve/hungrywalrus/di/DatabaseModule.kt` (lines 33–55)

**Description:** `provideDatabase` is correctly annotated `@Singleton`. However, the five DAO provider methods (`provideNutritionPlanDao`, `provideLogEntryDao`, `provideRecipeDao`, `provideRecipeIngredientDao`, `provideFoodCacheDao`) carry no scope annotation. Without `@Singleton`, Hilt will call each `@Provides` method on every injection site, creating a new DAO wrapper object each time. In practice Room's own internals return the same underlying DAO implementation for a given database instance, so the objects are functionally equivalent. However, this is inconsistent with the rest of the DI configuration (all other `@Provides` in `NetworkModule` and `DatabaseModule`'s database itself are `@Singleton`), creates unnecessary object churn, and will confuse anyone debugging DI graphs who expects singletons throughout.

**Recommendation:** Add `@Singleton` to each of the five DAO provider methods in `DatabaseModule` to match the scoping pattern used everywhere else in the project.

---

#### m3 — `FoodLookupRepositoryImplTest` does not test the `HttpException` fallback path in `lookupBarcode` when the cache is expired

**File:** `app/src/test/java/com/delve/hungrywalrus/data/repository/FoodLookupRepositoryImplTest.kt`

**Description:** The existing test `lookupBarcode returns failure on HttpException when no cache` (line 265) covers the `HttpException` path when `cached == null`. It correctly asserts a failure result. What is not tested is the scenario where `cached != null` but the entry is expired and the API throws `HttpException`. In that case the current code at lines 78–83 returns the stale cache entry as a success. This is the exact behaviour described in m1 above, and the absence of a test for this path means the stale-cache-on-HttpException regression could be introduced silently.

**Recommendation:** Add a test named `lookupBarcode returns failure on HttpException when cache is expired, not stale cache`. Set up an expired cache entity, make the API throw `HttpException` with code 500, and assert that the result is a failure rather than the stale cached product. This test will also document the intended contract once m1 is resolved.

---

## Test coverage assessment

The test suite is in a materially good state. All previously identified gaps (OFF HTTP error path, `lookupBarcode` without cache, `cacheResult` entity content, `getAllRecipes`, `deleteRecipe`) have been filled in Fix Sessions 01 and 03. The overall count is 45 data-layer unit tests across 6 test classes.

**Remaining gaps:**

- As noted in m3, the expired-cache-plus-`HttpException` path in `lookupBarcode` is untested.

- `searchUsda` has no test for the generic `Exception` catch block (the `catch (e: Exception)` path that returns "Could not read food data"). The equivalent path in `searchOpenFoodFacts` is tested (`searchOpenFoodFacts returns failure with generic message on unexpected Exception`). Adding a matching test for `searchUsda` would make coverage symmetric and ensure the message string is not accidentally changed.

- `NutritionPlanRepositoryTest.getPlanForDate converts date to epoch millis` verifies `plan.kcalTarget` but does not assert the remaining mapped fields (`proteinTargetG`, `carbsTargetG`, `fatTargetG`, `effectiveFrom`). The `getCurrentPlan` test does verify all fields. Adding the remaining assertions to `getPlanForDate` would make the mapping test complete without significant effort.

- `LogEntryRepositoryTest.getEntriesForDate` asserts `foodName` and `kcal` from the mapped result but not `id`, `proteinG`, `carbsG`, `fatG`, or `timestamp`. These are trivially testable with the entity already in the test fixture at lines 37–45.

**Test quality notes:**

- The `savePlan inserts entity with correct values` timestamp bracketing pattern (`beforeCall..afterCall`) is the correct approach and should be used consistently in any future tests that assert on `System.currentTimeMillis()` captures.

- The `mockkStatic("androidx.room.RoomDatabaseKt")` / `unmockkStatic` teardown pattern in `RecipeRepositoryImplTest` is correctly implemented. The `coVerifyOrder` in `updateRecipe deletes old ingredients before inserting new ones` verifies the delete-before-insert contract, which is the most important correctness property of that method.

- All 16 tests in `FoodLookupRepositoryImplTest` use `runTest` and `coEvery`/`coVerify` correctly. No tests use `Thread.sleep` or `runBlocking` in a context where `runTest` should be used instead.

---

## Conclusion

The data layer is ready to support ViewModel development. No critical or major issues remain. The two structural minor issues (m1 and m2) should be fixed before they accumulate with any UI-layer code that builds on them: m1 because it silently surfaces stale data on a server error, and m2 to keep the DI configuration consistent and avoid future confusion. m3 (the missing test case) should accompany the m1 fix.

Priority order:

1. **m1** — Fix `lookupBarcode` to not fall back to stale cache on `HttpException`. The correct behaviour for a 404 is `Result.success(null)`; for 5xx it should be `Result.failure(...)`. Write the companion test as part of the same change (m3).
2. **m2** — Add `@Singleton` to the five DAO providers in `DatabaseModule`. One-line change per provider, zero risk.
3. Fill the minor test gaps (symmetric `searchUsda` generic exception test; full field assertions in `getPlanForDate` and `getEntriesForDate`). These are low-effort additions that reduce future regression risk.
