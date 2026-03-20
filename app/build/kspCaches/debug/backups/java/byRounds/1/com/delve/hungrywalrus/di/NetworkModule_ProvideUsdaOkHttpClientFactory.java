package com.delve.hungrywalrus.di;

import android.content.SharedPreferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("javax.inject.Named")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class NetworkModule_ProvideUsdaOkHttpClientFactory implements Factory<OkHttpClient> {
  private final Provider<SharedPreferences> encryptedPrefsProvider;

  public NetworkModule_ProvideUsdaOkHttpClientFactory(
      Provider<SharedPreferences> encryptedPrefsProvider) {
    this.encryptedPrefsProvider = encryptedPrefsProvider;
  }

  @Override
  public OkHttpClient get() {
    return provideUsdaOkHttpClient(encryptedPrefsProvider.get());
  }

  public static NetworkModule_ProvideUsdaOkHttpClientFactory create(
      Provider<SharedPreferences> encryptedPrefsProvider) {
    return new NetworkModule_ProvideUsdaOkHttpClientFactory(encryptedPrefsProvider);
  }

  public static OkHttpClient provideUsdaOkHttpClient(SharedPreferences encryptedPrefs) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideUsdaOkHttpClient(encryptedPrefs));
  }
}
