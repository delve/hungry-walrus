package com.delve.hungrywalrus.ui.screen.createrecipe;

import androidx.lifecycle.SavedStateHandle;
import com.delve.hungrywalrus.data.repository.RecipeRepository;
import com.delve.hungrywalrus.domain.usecase.ScaleNutritionUseCase;
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
public final class CreateRecipeViewModel_Factory implements Factory<CreateRecipeViewModel> {
  private final Provider<RecipeRepository> recipeRepoProvider;

  private final Provider<ScaleNutritionUseCase> scaleUseCaseProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public CreateRecipeViewModel_Factory(Provider<RecipeRepository> recipeRepoProvider,
      Provider<ScaleNutritionUseCase> scaleUseCaseProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.recipeRepoProvider = recipeRepoProvider;
    this.scaleUseCaseProvider = scaleUseCaseProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public CreateRecipeViewModel get() {
    return newInstance(recipeRepoProvider.get(), scaleUseCaseProvider.get(), savedStateHandleProvider.get());
  }

  public static CreateRecipeViewModel_Factory create(Provider<RecipeRepository> recipeRepoProvider,
      Provider<ScaleNutritionUseCase> scaleUseCaseProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new CreateRecipeViewModel_Factory(recipeRepoProvider, scaleUseCaseProvider, savedStateHandleProvider);
  }

  public static CreateRecipeViewModel newInstance(RecipeRepository recipeRepo,
      ScaleNutritionUseCase scaleUseCase, SavedStateHandle savedStateHandle) {
    return new CreateRecipeViewModel(recipeRepo, scaleUseCase, savedStateHandle);
  }
}
