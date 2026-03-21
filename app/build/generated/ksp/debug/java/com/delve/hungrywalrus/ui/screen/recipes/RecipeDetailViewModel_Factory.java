package com.delve.hungrywalrus.ui.screen.recipes;

import androidx.lifecycle.SavedStateHandle;
import com.delve.hungrywalrus.data.repository.RecipeRepository;
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
public final class RecipeDetailViewModel_Factory implements Factory<RecipeDetailViewModel> {
  private final Provider<RecipeRepository> recipeRepoProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public RecipeDetailViewModel_Factory(Provider<RecipeRepository> recipeRepoProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.recipeRepoProvider = recipeRepoProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public RecipeDetailViewModel get() {
    return newInstance(recipeRepoProvider.get(), savedStateHandleProvider.get());
  }

  public static RecipeDetailViewModel_Factory create(Provider<RecipeRepository> recipeRepoProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new RecipeDetailViewModel_Factory(recipeRepoProvider, savedStateHandleProvider);
  }

  public static RecipeDetailViewModel newInstance(RecipeRepository recipeRepo,
      SavedStateHandle savedStateHandle) {
    return new RecipeDetailViewModel(recipeRepo, savedStateHandle);
  }
}
