package com.delve.hungrywalrus.data.remote.openfoodfacts

import com.delve.hungrywalrus.domain.model.FoodSearchResult
import com.delve.hungrywalrus.domain.model.FoodSource
import com.delve.hungrywalrus.domain.model.NutritionField

object OffResponseMapper {

    fun mapProducts(products: List<OffProduct>): List<FoodSearchResult> {
        return products.map { mapProduct(it) }
    }

    fun mapProduct(product: OffProduct): FoodSearchResult {
        val kcal = product.nutriments?.energyKcal100g
        val protein = product.nutriments?.proteins100g
        val carbs = product.nutriments?.carbohydrates100g
        val fat = product.nutriments?.fat100g

        val missingFields = buildSet {
            if (kcal == null) add(NutritionField.KCAL)
            if (protein == null) add(NutritionField.PROTEIN)
            if (carbs == null) add(NutritionField.CARBS)
            if (fat == null) add(NutritionField.FAT)
        }

        val name = if (product.productName.isNullOrBlank()) {
            product.code
        } else {
            product.productName
        }

        return FoodSearchResult(
            id = "off:${product.code}",
            name = name,
            source = FoodSource.OPEN_FOOD_FACTS,
            kcalPer100g = kcal,
            proteinPer100g = protein,
            carbsPer100g = carbs,
            fatPer100g = fat,
            missingFields = missingFields,
        )
    }
}
