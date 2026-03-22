package com.delve.hungrywalrus.ui.screen.settings;

import com.delve.hungrywalrus.data.repository.NutritionPlanRepository;
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

  private final Provider<NutritionPlanRepository> planRepoProvider;

  public SettingsViewModel_Factory(Provider<ApiKeyStore> apiKeyStoreProvider,
      Provider<NutritionPlanRepository> planRepoProvider) {
    this.apiKeyStoreProvider = apiKeyStoreProvider;
    this.planRepoProvider = planRepoProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(apiKeyStoreProvider.get(), planRepoProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<ApiKeyStore> apiKeyStoreProvider,
      Provider<NutritionPlanRepository> planRepoProvider) {
    return new SettingsViewModel_Factory(apiKeyStoreProvider, planRepoProvider);
  }

  public static SettingsViewModel newInstance(ApiKeyStore apiKeyStore,
      NutritionPlanRepository planRepo) {
    return new SettingsViewModel(apiKeyStore, planRepo);
  }
}
