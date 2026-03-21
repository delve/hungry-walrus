package com.delve.hungrywalrus.ui.screen.addentry;

import com.delve.hungrywalrus.data.repository.FoodLookupRepository;
import com.delve.hungrywalrus.data.repository.LogEntryRepository;
import com.delve.hungrywalrus.data.repository.RecipeRepository;
import com.delve.hungrywalrus.domain.usecase.ScaleNutritionUseCase;
import com.delve.hungrywalrus.domain.usecase.ValidateFoodDataUseCase;
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
public final class AddEntryViewModel_Factory implements Factory<AddEntryViewModel> {
  private final Provider<LogEntryRepository> logRepoProvider;

  private final Provider<FoodLookupRepository> foodLookupRepoProvider;

  private final Provider<RecipeRepository> recipeRepoProvider;

  private final Provider<ScaleNutritionUseCase> scaleUseCaseProvider;

  private final Provider<ValidateFoodDataUseCase> validateUseCaseProvider;

  private final Provider<ApiKeyStore> apiKeyStoreProvider;

  public AddEntryViewModel_Factory(Provider<LogEntryRepository> logRepoProvider,
      Provider<FoodLookupRepository> foodLookupRepoProvider,
      Provider<RecipeRepository> recipeRepoProvider,
      Provider<ScaleNutritionUseCase> scaleUseCaseProvider,
      Provider<ValidateFoodDataUseCase> validateUseCaseProvider,
      Provider<ApiKeyStore> apiKeyStoreProvider) {
    this.logRepoProvider = logRepoProvider;
    this.foodLookupRepoProvider = foodLookupRepoProvider;
    this.recipeRepoProvider = recipeRepoProvider;
    this.scaleUseCaseProvider = scaleUseCaseProvider;
    this.validateUseCaseProvider = validateUseCaseProvider;
    this.apiKeyStoreProvider = apiKeyStoreProvider;
  }

  @Override
  public AddEntryViewModel get() {
    return newInstance(logRepoProvider.get(), foodLookupRepoProvider.get(), recipeRepoProvider.get(), scaleUseCaseProvider.get(), validateUseCaseProvider.get(), apiKeyStoreProvider.get());
  }

  public static AddEntryViewModel_Factory create(Provider<LogEntryRepository> logRepoProvider,
      Provider<FoodLookupRepository> foodLookupRepoProvider,
      Provider<RecipeRepository> recipeRepoProvider,
      Provider<ScaleNutritionUseCase> scaleUseCaseProvider,
      Provider<ValidateFoodDataUseCase> validateUseCaseProvider,
      Provider<ApiKeyStore> apiKeyStoreProvider) {
    return new AddEntryViewModel_Factory(logRepoProvider, foodLookupRepoProvider, recipeRepoProvider, scaleUseCaseProvider, validateUseCaseProvider, apiKeyStoreProvider);
  }

  public static AddEntryViewModel newInstance(LogEntryRepository logRepo,
      FoodLookupRepository foodLookupRepo, RecipeRepository recipeRepo,
      ScaleNutritionUseCase scaleUseCase, ValidateFoodDataUseCase validateUseCase,
      ApiKeyStore apiKeyStore) {
    return new AddEntryViewModel(logRepo, foodLookupRepo, recipeRepo, scaleUseCase, validateUseCase, apiKeyStore);
  }
}
