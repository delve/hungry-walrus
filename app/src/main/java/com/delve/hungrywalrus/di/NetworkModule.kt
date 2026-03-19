package com.delve.hungrywalrus.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for network dependencies.
 *
 * TODO: Provide the following:
 * - USDA OkHttpClient with API key interceptor (@Named("usda"))
 * - USDA Retrofit instance (@Named("usda"))
 * - USDA API service interface
 * - Open Food Facts OkHttpClient with User-Agent interceptor (@Named("off"))
 * - Open Food Facts Retrofit instance (@Named("off"))
 * - Open Food Facts API service interface
 * - HttpLoggingInterceptor (debug builds only)
 *
 * Timeouts: 15s connect, 15s read, 15s write.
 * JSON: Kotlinx Serialization via retrofit2-kotlinx-serialization-converter.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    // TODO: Implement in network/API session
}
