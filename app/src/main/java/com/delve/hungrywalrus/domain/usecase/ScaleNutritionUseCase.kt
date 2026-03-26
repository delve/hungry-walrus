package com.delve.hungrywalrus.domain.usecase

import com.delve.hungrywalrus.domain.model.NutritionValues
import com.delve.hungrywalrus.domain.model.Recipe
import javax.inject.Inject

/**
 * Scales nutritional reference values to a consumed weight.
 *
 * Two overloads are provided:
 * - Per-100g food item scaled to a specific weight.
 * - Recipe total scaled to a portion weight.
 */
class ScaleNutritionUseCase @Inject constructor() {

    /**
     * Scale per-100g nutrition values to [weightG] grams consumed.
     * Formula: scaledValue = (valuePer100g / 100.0) * weightG
     *
     * All per-100g inputs must be >= 0.0; negative reference values are physically impossible.
     *
     * @throws IllegalArgumentException if any per-100g input or [weightG] is negative.
     */
    operator fun invoke(
        kcalPer100g: Double,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        weightG: Double,
    ): NutritionValues {
        require(weightG >= 0.0) { "weightG must not be negative" }
        require(kcalPer100g >= 0.0) { "kcalPer100g must not be negative" }
        require(proteinPer100g >= 0.0) { "proteinPer100g must not be negative" }
        require(carbsPer100g >= 0.0) { "carbsPer100g must not be negative" }
        require(fatPer100g >= 0.0) { "fatPer100g must not be negative" }
        return NutritionValues(
            kcal = (kcalPer100g / 100.0) * weightG,
            proteinG = (proteinPer100g / 100.0) * weightG,
            carbsG = (carbsPer100g / 100.0) * weightG,
            fatG = (fatPer100g / 100.0) * weightG,
        )
    }

    /**
     * Scale a [recipe] to a [portionWeightG] gram portion.
     * Formula: scaledValue = (recipeTotalValue / recipeTotalWeightG) * portionWeightG
     *
     * @throws IllegalArgumentException if [recipe].totalWeightG is not positive.
     * @throws IllegalArgumentException if [portionWeightG] is negative.
     */
    fun scaleRecipePortion(recipe: Recipe, portionWeightG: Double): NutritionValues {
        require(portionWeightG >= 0.0) { "portionWeightG must not be negative" }
        require(recipe.totalWeightG > 0.0) { "Recipe totalWeightG must be positive" }
        return NutritionValues(
            kcal = (recipe.totalKcal / recipe.totalWeightG) * portionWeightG,
            proteinG = (recipe.totalProteinG / recipe.totalWeightG) * portionWeightG,
            carbsG = (recipe.totalCarbsG / recipe.totalWeightG) * portionWeightG,
            fatG = (recipe.totalFatG / recipe.totalWeightG) * portionWeightG,
        )
    }
}
