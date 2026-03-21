package com.delve.hungrywalrus.ui.screen.recipes;

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
public final class RecipeListViewModel_Factory implements Factory<RecipeListViewModel> {
  private final Provider<RecipeRepository> recipeRepoProvider;

  public RecipeListViewModel_Factory(Provider<RecipeRepository> recipeRepoProvider) {
    this.recipeRepoProvider = recipeRepoProvider;
  }

  @Override
  public RecipeListViewModel get() {
    return newInstance(recipeRepoProvider.get());
  }

  public static RecipeListViewModel_Factory create(Provider<RecipeRepository> recipeRepoProvider) {
    return new RecipeListViewModel_Factory(recipeRepoProvider);
  }

  public static RecipeListViewModel newInstance(RecipeRepository recipeRepo) {
    return new RecipeListViewModel(recipeRepo);
  }
}
