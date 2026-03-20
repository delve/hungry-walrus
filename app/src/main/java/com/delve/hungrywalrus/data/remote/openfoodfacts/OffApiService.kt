package com.delve.hungrywalrus.data.remote.openfoodfacts

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OffApiService {

    @GET("cgi/search.pl")
    suspend fun searchProducts(
        @Query("search_terms") searchTerms: String,
        @Query("search_simple") searchSimple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("fields") fields: String = "code,product_name,nutriments",
    ): OffSearchResponse

    @GET("api/v2/product/{barcode}")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String,
    ): OffBarcodeResponse
}
