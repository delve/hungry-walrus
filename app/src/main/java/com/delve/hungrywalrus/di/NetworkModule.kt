package com.delve.hungrywalrus.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.delve.hungrywalrus.BuildConfig
import com.delve.hungrywalrus.data.remote.openfoodfacts.OffApiService
import com.delve.hungrywalrus.data.remote.usda.UsdaApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    const val ENCRYPTED_PREFS_FILE = "encrypted_prefs"
    const val USDA_API_KEY_PREF = "usda_api_key"

    private const val USDA_BASE_URL = "https://api.nal.usda.gov/fdc/v1/"
    private const val OFF_BASE_URL = "https://world.openfoodfacts.org/"
    private const val TIMEOUT_SECONDS = 15L

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }

    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(
        @ApplicationContext context: Context,
    ): SharedPreferences {
        return try {
            createEncryptedPrefs(context)
        } catch (e: Exception) {
            // On failure (e.g. Keystore corruption), clear and re-create.
            context.getSharedPreferences(ENCRYPTED_PREFS_FILE, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
            // Delete the file and try again.
            context.deleteSharedPreferences(ENCRYPTED_PREFS_FILE)
            createEncryptedPrefs(context)
        }
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    @Provides
    @Singleton
    @Named("usda")
    fun provideUsdaOkHttpClient(
        encryptedPrefs: SharedPreferences,
    ): OkHttpClient {
        val apiKeyInterceptor = Interceptor { chain ->
            val apiKey = encryptedPrefs.getString(USDA_API_KEY_PREF, "") ?: ""
            val originalRequest = chain.request()
            val url = originalRequest.url.newBuilder()
                .addQueryParameter("api_key", apiKey)
                .build()
            val request = originalRequest.newBuilder()
                .url(url)
                .build()
            chain.proceed(request)
        }

        return buildOkHttpClient(apiKeyInterceptor)
    }

    @Provides
    @Singleton
    @Named("off")
    fun provideOffOkHttpClient(): OkHttpClient {
        val userAgentInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "HungryWalrus/1.0 (Android; contact@delve.dev)")
                .build()
            chain.proceed(request)
        }

        return buildOkHttpClient(userAgentInterceptor)
    }

    private fun buildOkHttpClient(interceptor: Interceptor): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(interceptor)

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    @Named("usda")
    fun provideUsdaRetrofit(
        @Named("usda") okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(USDA_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    @Named("off")
    fun provideOffRetrofit(
        @Named("off") okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(OFF_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideUsdaApiService(
        @Named("usda") retrofit: Retrofit,
    ): UsdaApiService {
        return retrofit.create(UsdaApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideOffApiService(
        @Named("off") retrofit: Retrofit,
    ): OffApiService {
        return retrofit.create(OffApiService::class.java)
    }
}
