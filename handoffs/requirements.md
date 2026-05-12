# Product Requirements: Hungry Walrus

## Overview
Hungry Walrus is an Android mobile application for tracking daily nutritional
intake against a user-defined plan. The app runs entirely on the local device
with no backend services. Network activity is limited to retrieving nutrition
data from external APIs.

## User profile
Health-conscious individuals who already have a nutrition plan from a
professional or other source and want a simple tool to track adherence.
The app does not generate or recommend plans.

## Features

### Nutrition plan
- User enters their daily targets: total kilocalories, protein (g),
  carbohydrates (g), and fat (g).
- The app stores these values and uses them as the baseline for progress
  tracking.
- User can update their plan at any time. Changes apply from that point
  forward and do not alter historical data.
- The nutrition plan is managed from the Settings screen. There is no
  separate dedicated plan screen.
- Kilocalorie targets must be greater than zero. Macronutrient targets
  (protein, carbohydrates, fat) must be zero or greater. A zero value
  for a macronutrient is valid (e.g. a zero-fat target).

### Recipes
- User can create a recipe composed of multiple ingredients.
- Each ingredient is a food item with a quantity, added via any of the
  food lookup methods (USDA search, Open Food Facts search, barcode scan,
  or manual entry).
- The recipe stores its ingredients, total weight, and total nutritional
  values derived from the ingredients.
- Recipes persist locally and can be edited. Edits to a recipe do not
  alter previously logged entries that used that recipe.
- Recipes are reusable across multiple log entries.
- Recipe creation displays a live running total of nutritional values as
  ingredients are added.
- In recipe edit mode, the user can modify an existing ingredient
  in-place by tapping the ingredient row. Tapping opens an edit dialog
  pre-populated with the ingredient's current values. The edit dialog
  must allow the user to change the ingredient's weight at minimum. For
  ingredients added via manual entry, the dialog must also allow the
  user to change the ingredient name and the per-100g nutritional
  values (kcal, protein, carbohydrates, fat). For ingredients sourced
  from USDA or Open Food Facts, the dialog should also allow the user
  to correct the ingredient name and per-100g values, since API data
  may be incorrect or incomplete and the user should not have to
  remove and re-add the ingredient to fix a small error. Confirming
  the edit dialog updates the in-memory ingredient list and recomputes
  the running totals immediately. The changes are persisted to the
  database only when the user saves the recipe. Cancelling the edit
  dialog discards the changes for that ingredient.

### Meal logging
- Each log entry is an independent record. There is no meal container
  or grouping mechanism.
- A log entry can be one of:
  - A portion of a saved recipe: user selects a recipe and enters the
    weight consumed. Nutritional values are calculated proportionally
    from the recipe's total weight.
  - A single food item found via API: user finds a food item via search
    or barcode scan and enters the weight consumed. Nutritional values
    are scaled from the per-100g reference values to the entered weight.
  - A manually entered food item: user enters the food name and the
    exact nutritional values consumed directly. Manual entry does not
    require a weight input -- the user provides the final kilocalorie
    and macronutrient values as consumed.
- Food items can be added via:
  - Generic/natural food search (USDA FoodData Central).
  - Branded product search (Open Food Facts).
  - Barcode scan using device camera (Open Food Facts only).
  - Manual entry of food name and nutritional values.
- For API-sourced and recipe-based entries, the user must provide a
  weight input. This weight is used to scale the nutritional values of
  the food reference data based on the amount consumed. Weight for a log
  entry is not stored.
- For manual entries, the user enters nutritional values directly as
  consumed. No weight input is required and no scaling is performed.
- Each log entry records: food name (or recipe name), kilocalories,
  protein (g), carbohydrates (g), fat (g), and a timestamp.
- After completing an entry, the user sees a validation summary with
  options to confirm or go back and edit before saving.
- Log entries can be deleted from the daily progress view with a
  confirmation prompt to prevent accidental deletion.
- Full log entry editing is out of scope for this version.

### Daily progress
- Displays the current day's total intake versus the plan.
- Shows a running total of kilocalories, protein, carbohydrates, and fat
  consumed so far today.
- Shows remaining allowance or overage for each metric.
- Displayed as both progress bars and numeric values.
- When no nutrition plan has been configured, the daily progress screen
  displays a clear notice directing the user to set up a plan in Settings.
  This notice must update immediately when a plan is subsequently saved
  without requiring the user to navigate away and back.

### Rolling summaries
- 7-day summary: cumulative total intake and cumulative plan targets
  over the last 7 days.
- 28-day summary: same metrics over the last 28 days.
- Both views display cumulative plan target values alongside cumulative
  intake values so the user can compare consumption against their plan
  for the period.
- Summary data must refresh when the user navigates to the summaries
  screen (e.g. by switching tabs). A stale snapshot from a previous
  visit is not acceptable -- the user should see up-to-date totals
  reflecting any entries logged since the last visit.
- Rolling window definition: the current day is excluded from both the
  7-day and 28-day windows unless the local device time is at or after
  20:00. Rationale: until late evening the current day is incomplete and
  including a partially-consumed day would distort cumulative totals
  (both intake and plan targets) and make day-over-day comparisons
  misleading. Concretely:
  - Before 20:00 local time: the period ends at the end of yesterday
    (i.e. just before midnight at the boundary between yesterday and
    today). The 7-day period covers the 7 days ending yesterday; the
    28-day period covers the 28 days ending yesterday.
  - From 20:00 local time onward: the period ends at the end of today.
    The 7-day period covers the 7 days ending today (inclusive); the
    28-day period covers the 28 days ending today (inclusive).
  The 20:00 threshold is a constant value selected based on user
  feedback and is not user-configurable in this version.

## Data sources
- USDA FoodData Central: generic and natural food queries. Free API,
  requires an API key (free registration).
- Open Food Facts: branded product search and barcode lookups. Free API,
  no authentication required.
- Barcode scans query Open Food Facts only. Do not query USDA for barcode
  lookups.
- Search results for packaged foods display nutrition values as shown on
  the package label. Search results for unpackaged/generic foods display
  nutrition per 100g.
- API responses are cached locally for a reasonable duration to improve
  performance under flaky connectivity and reduce API load. Cache
  duration and eviction strategy to be determined by the Architect.
- If any of the four core nutritional values (kilocalories, protein,
  carbohydrates, fat) are missing from an API response, prompt the user
  to provide an estimate for the missing values before the entry can be
  saved.

## Data retention
- Log entries older than 2 years are automatically deleted.
- Recipes are retained indefinitely with an option for the user to
  manually delete them.

## Constraints
- All data stored locally on device. No cloud storage, no user accounts,
  no authentication.
- English language only. UK formatting conventions.
- Metric units. Energy in kilocalories (kcal).
- Must support Android 10 (API 29) and above.
- Camera permission required for barcode scanning.
- USDA API key must be stored securely. Architect to determine approach.
- Nutritional values displayed to the user are rounded to the nearest
  0.5g for macronutrients and the nearest whole number for kilocalories.

## Out of scope
- Nutrition plan generation or recommendations.
- Social features or data sharing.
- iOS support.
- Annual recap (potential future feature, do not design for it).
- Meal grouping or meal containers. Each log entry is independent.
- Full log entry editing (potential future enhancement).
- Light mode theme.

## Revision history

### Revision 1 -- 2026-05-12

- **Rolling summary window cutoff added.** Clarified the definition of
  the rolling 7-day and 28-day periods. The current day is now excluded
  from both windows unless the local device time is at or after 20:00,
  at which point today is treated as complete and included in the
  period. Before 20:00 the period ends at end-of-yesterday; from 20:00
  onward it ends at end-of-today. Rationale: an in-progress day would
  otherwise depress cumulative intake totals and distort the
  intake-versus-plan comparison that the summaries are intended to
  surface. The 20:00 threshold was selected based on user feedback and
  is a fixed constant in this version.

### Revision 2 -- 2026-05-12

- **In-place editing of recipe ingredients.** The Recipes section now
  specifies that in recipe edit mode, tapping an existing ingredient
  opens an edit dialog pre-populated with the ingredient's current
  values, rather than requiring the user to remove and re-add the
  ingredient. Weight is always editable. Ingredient name and per-100g
  macronutrient values are also editable, both for manual ingredients
  (where this is essential, since the user is the sole source of those
  values) and for API-sourced ingredients (where this allows the user
  to correct errors or fill in missing values without re-fetching).
  Confirming the dialog updates the in-memory ingredient list and
  recomputes the running totals immediately; changes are persisted
  only on recipe save. Cancelling discards the changes for that
  ingredient. This is a UI/ViewModel-level change; no database schema
  or DAO changes are required.
