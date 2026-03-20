package com.delve.hungrywalrus.data.repository;

import com.delve.hungrywalrus.data.local.dao.RecipeDao;
import com.delve.hungrywalrus.data.local.dao.RecipeIngredientDao;
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
public final class RecipeRepositoryImpl_Factory implements Factory<RecipeRepositoryImpl> {
  private final Provider<RecipeDao> recipeDaoProvider;

  private final Provider<RecipeIngredientDao> ingredientDaoProvider;

  public RecipeRepositoryImpl_Factory(Provider<RecipeDao> recipeDaoProvider,
      Provider<RecipeIngredientDao> ingredientDaoProvider) {
    this.recipeDaoProvider = recipeDaoProvider;
    this.ingredientDaoProvider = ingredientDaoProvider;
  }

  @Override
  public RecipeRepositoryImpl get() {
    return newInstance(recipeDaoProvider.get(), ingredientDaoProvider.get());
  }

  public static RecipeRepositoryImpl_Factory create(Provider<RecipeDao> recipeDaoProvider,
      Provider<RecipeIngredientDao> ingredientDaoProvider) {
    return new RecipeRepositoryImpl_Factory(recipeDaoProvider, ingredientDaoProvider);
  }

  public static RecipeRepositoryImpl newInstance(RecipeDao recipeDao,
      RecipeIngredientDao ingredientDao) {
    return new RecipeRepositoryImpl(recipeDao, ingredientDao);
  }
}
