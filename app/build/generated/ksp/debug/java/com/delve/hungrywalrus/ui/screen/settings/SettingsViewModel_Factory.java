package com.delve.hungrywalrus.ui.screen.settings;

import com.delve.hungrywalrus.util.ApiKeyStore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<ApiKeyStore> apiKeyStoreProvider;

  public SettingsViewModel_Factory(Provider<ApiKeyStore> apiKeyStoreProvider) {
    this.apiKeyStoreProvider = apiKeyStoreProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(apiKeyStoreProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<ApiKeyStore> apiKeyStoreProvider) {
    return new SettingsViewModel_Factory(apiKeyStoreProvider);
  }

  public static SettingsViewModel newInstance(ApiKeyStore apiKeyStore) {
    return new SettingsViewModel(apiKeyStore);
  }
}
