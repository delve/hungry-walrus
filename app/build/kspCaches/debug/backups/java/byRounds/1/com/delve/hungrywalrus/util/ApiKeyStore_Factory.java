package com.delve.hungrywalrus.util;

import android.content.SharedPreferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class ApiKeyStore_Factory implements Factory<ApiKeyStore> {
  private final Provider<SharedPreferences> encryptedPrefsProvider;

  public ApiKeyStore_Factory(Provider<SharedPreferences> encryptedPrefsProvider) {
    this.encryptedPrefsProvider = encryptedPrefsProvider;
  }

  @Override
  public ApiKeyStore get() {
    return newInstance(encryptedPrefsProvider.get());
  }

  public static ApiKeyStore_Factory create(Provider<SharedPreferences> encryptedPrefsProvider) {
    return new ApiKeyStore_Factory(encryptedPrefsProvider);
  }

  public static ApiKeyStore newInstance(SharedPreferences encryptedPrefs) {
    return new ApiKeyStore(encryptedPrefs);
  }
}
