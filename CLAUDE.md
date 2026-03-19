# Hungry Walrus

Android nutrition tracking app. Local-first, no backend. Kotlin + Jetpack Compose.

## Stack
- Language: Kotlin
- UI: Jetpack Compose
- Local storage: Room database
- External API: Open Food Facts (free, no auth required)
- External API: USDA FoodData Central (free, API key required)
- Min SDK: 29 (Android 10)
- Target SDK: TBD by Architect

## Environment
- ANDROID_HOME: ~/Android/Sdk
- JDK: 17

## Core features
- User-defined nutrition plan (daily calorie and macronutrient targets, manually entered)
- Meal logging with manual entry or search via food database APIs
- Daily progress view showing intake against plan
- Rolling 7-day and 28-day summary views

## Project structure
- `/docs` — long-lived project documentation
- `/handoffs` — agent output documents passed between pipeline stages
- `/app` — Android application source
- `.claude/agents/` — agent role definitions

## Commands
- TBD — will be populated after DevOps agent configures the build pipeline

## Key decisions
- All user data stored locally on device. No cloud sync, no user accounts.
- Internet access is only for nutrition data lookups via food database APIs.
- English only, UK formatting conventions (dd/MM/yyyy, comma for thousands).
- Metric units. Energy in kilocalories (kcal).
- Agent outputs (architecture doc, design spec, review reports) go in /handoffs.