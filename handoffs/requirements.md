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

### Meal logging
- Each log entry is an independent record. There is no meal container
  or grouping mechanism.
- A log entry can be one of:
  - A portion of a saved recipe: user selects a recipe and enters the
    weight consumed. Nutritional values are calculated proportionally
    from the recipe's total weight.
  - A single food item: user finds or enters a food item and enters
    the weight consumed. Nutritional values are scaled from the per-100g
    reference values to the entered weight.
- Food items can be added via:
  - Generic/natural food search (USDA FoodData Central).
  - Branded product search (Open Food Facts).
  - Barcode scan using device camera (Open Food Facts only).
  - Manual entry of food name and nutritional values.
- All food entries require a weight input from the user, whether the
  item comes from a recipe, an API lookup, or manual entry. This weight
  input is used only to scale the nutritional values of the food reference
  data based on the amount consumed.
- Each log entry records: food name (or recipe name), kilocalories,
  protein (g), carbohydrates (g), fat (g), and a timestamp.
- After completing an entry, the user sees a validation summary with
  options to confirm or go back and edit before saving.
- Log entries can be deleted from the daily progress view with a
  confirmation prompt to prevent accidental deletion.
- Full log entry editing is out of scope for this version.

### Daily progress
- Displays the current day's total intake versus the plan.
- Shows remaining allowance or overage for each metric.
- Displayed as both progress bars and numeric values.

### Rolling summaries
- 7-day summary: cumulative total intake and cumulative plan targets
  over the last 7 days.
- 28-day summary: same metrics over the last 28 days.

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