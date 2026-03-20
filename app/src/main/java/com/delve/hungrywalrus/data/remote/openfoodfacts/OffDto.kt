package com.delve.hungrywalrus.data.remote.openfoodfacts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OffSearchResponse(
    val products: List<OffProduct> = emptyList(),
)

@Serializable
data class OffBarcodeResponse(
    val status: Int = 0,
    val product: OffProduct? = null,
)

@Serializable
data class OffProduct(
    val code: String = "",
    @SerialName("product_name")
    val productName: String? = null,
    val nutriments: OffNutriments? = null,
)

@Serializable
data class OffNutriments(
    @SerialName("energy-kcal_100g")
    val energyKcal100g: Double? = null,
    @SerialName("proteins_100g")
    val proteins100g: Double? = null,
    @SerialName("carbohydrates_100g")
    val carbohydrates100g: Double? = null,
    @SerialName("fat_100g")
    val fat100g: Double? = null,
)
