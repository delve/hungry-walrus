package com.delve.hungrywalrus.data.remote.usda

import com.delve.hungrywalrus.domain.model.FoodSearchResult
import com.delve.hungrywalrus.domain.model.FoodSource
import com.delve.hungrywalrus.domain.model.NutritionField

private const val NUTRIENT_ID_KCAL = 1008
private const val NUTRIENT_ID_PROTEIN = 1003
private const val NUTRIENT_ID_CARBS = 1005
private const val NUTRIENT_ID_FAT = 1004

object UsdaResponseMapper {

    fun mapFoods(foods: List<UsdaFood>): List<FoodSearchResult> {
        return foods.map { mapFood(it) }
    }

    fun mapFood(food: UsdaFood): FoodSearchResult {
        val nutrientMap = food.foodNutrients.associateBy { it.nutrientId }

        val kcal = nutrientMap[NUTRIENT_ID_KCAL]?.value
        val protein = nutrientMap[NUTRIENT_ID_PROTEIN]?.value
        val carbs = nutrientMap[NUTRIENT_ID_CARBS]?.value
        val fat = nutrientMap[NUTRIENT_ID_FAT]?.value

        val missingFields = buildSet {
            if (kcal == null) add(NutritionField.KCAL)
            if (protein == null) add(NutritionField.PROTEIN)
            if (carbs == null) add(NutritionField.CARBS)
            if (fat == null) add(NutritionField.FAT)
        }

        return FoodSearchResult(
            id = "usda:${food.fdcId}",
            name = food.description,
            source = FoodSource.USDA,
            kcalPer100g = kcal,
            proteinPer100g = protein,
            carbsPer100g = carbs,
            fatPer100g = fat,
            missingFields = missingFields,
        )
    }
}
