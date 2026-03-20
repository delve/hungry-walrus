package com.delve.hungrywalrus.data.remote.usda

import retrofit2.http.GET
import retrofit2.http.Query

interface UsdaApiService {

    @GET("foods/search")
    suspend fun searchFoods(
        @Query("query") query: String,
        @Query("dataType") dataType: String = "Foundation,SR Legacy",
        @Query("pageSize") pageSize: Int = 25,
        @Query("api_key") apiKey: String,
    ): UsdaSearchResponse
}
