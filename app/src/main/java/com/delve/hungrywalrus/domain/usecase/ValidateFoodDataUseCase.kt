package com.delve.hungrywalrus.domain.usecase

import com.delve.hungrywalrus.domain.model.FoodSearchResult
import com.delve.hungrywalrus.domain.model.NutritionField
import javax.inject.Inject

/**
 * Validates and repairs a [FoodSearchResult] by applying user-supplied overrides for missing fields.
 *
 * The [missingFields] set is always re-derived after overrides are applied, so the returned
 * result is guaranteed to have [FoodSearchResult.missingFields] consistent with its nullable fields.
 */
class ValidateFoodDataUseCase @Inject constructor() {

    /**
     * Returns true if [result] has all required nutritional data (no missing fields).
     */
    fun isComplete(result: FoodSearchResult): Boolean = result.missingFields.isEmpty()

    /**
     * Applies user-supplied override values for any missing fields in [result].
     * Override parameters are only applied when non-null; existing non-null field values are
     * never replaced by a null override.
     *
     * All non-null override values must be >= 0.0; negative per-100g nutritional values are
     * physically impossible.
     *
     * @throws IllegalArgumentException if any non-null override value is negative.
     * @return A new [FoodSearchResult] with overrides applied and [FoodSearchResult.missingFields]
     *         re-derived from the resulting nullable fields.
     */
    fun applyOverrides(
        result: FoodSearchResult,
        kcalPer100g: Double? = null,
        proteinPer100g: Double? = null,
        carbsPer100g: Double? = null,
        fatPer100g: Double? = null,
    ): FoodSearchResult {
        kcalPer100g?.let { require(it >= 0.0) { "kcalPer100g must not be negative" } }
        proteinPer100g?.let { require(it >= 0.0) { "proteinPer100g must not be negative" } }
        carbsPer100g?.let { require(it >= 0.0) { "carbsPer100g must not be negative" } }
        fatPer100g?.let { require(it >= 0.0) { "fatPer100g must not be negative" } }
        val newKcal = kcalPer100g ?: result.kcalPer100g
        val newProtein = proteinPer100g ?: result.proteinPer100g
        val newCarbs = carbsPer100g ?: result.carbsPer100g
        val newFat = fatPer100g ?: result.fatPer100g

        val missing = buildSet {
            if (newKcal == null) add(NutritionField.KCAL)
            if (newProtein == null) add(NutritionField.PROTEIN)
            if (newCarbs == null) add(NutritionField.CARBS)
            if (newFat == null) add(NutritionField.FAT)
        }

        return result.copy(
            kcalPer100g = newKcal,
            proteinPer100g = newProtein,
            carbsPer100g = newCarbs,
            fatPer100g = newFat,
            missingFields = missing,
        )
    }
}
