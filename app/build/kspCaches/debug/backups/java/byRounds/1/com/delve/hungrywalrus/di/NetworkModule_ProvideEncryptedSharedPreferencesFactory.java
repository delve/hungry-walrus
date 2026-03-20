package com.delve.hungrywalrus.di;

import android.content.Context;
import android.content.SharedPreferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class NetworkModule_ProvideEncryptedSharedPreferencesFactory implements Factory<SharedPreferences> {
  private final Provider<Context> contextProvider;

  public NetworkModule_ProvideEncryptedSharedPreferencesFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SharedPreferences get() {
    return provideEncryptedSharedPreferences(contextProvider.get());
  }

  public static NetworkModule_ProvideEncryptedSharedPreferencesFactory create(
      Provider<Context> contextProvider) {
    return new NetworkModule_ProvideEncryptedSharedPreferencesFactory(contextProvider);
  }

  public static SharedPreferences provideEncryptedSharedPreferences(Context context) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideEncryptedSharedPreferences(context));
  }
}
