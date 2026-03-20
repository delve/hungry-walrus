package com.delve.hungrywalrus.di;

import com.delve.hungrywalrus.data.local.HungryWalrusDatabase;
import com.delve.hungrywalrus.data.local.dao.RecipeIngredientDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideRecipeIngredientDaoFactory implements Factory<RecipeIngredientDao> {
  private final Provider<HungryWalrusDatabase> databaseProvider;

  public DatabaseModule_ProvideRecipeIngredientDaoFactory(
      Provider<HungryWalrusDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public RecipeIngredientDao get() {
    return provideRecipeIngredientDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideRecipeIngredientDaoFactory create(
      Provider<HungryWalrusDatabase> databaseProvider) {
    return new DatabaseModule_ProvideRecipeIngredientDaoFactory(databaseProvider);
  }

  public static RecipeIngredientDao provideRecipeIngredientDao(HungryWalrusDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideRecipeIngredientDao(database));
  }
}
