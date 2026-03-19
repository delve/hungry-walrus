package com.delve.hungrywalrus.di;

import com.delve.hungrywalrus.data.local.HungryWalrusDatabase;
import com.delve.hungrywalrus.data.local.dao.RecipeDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideRecipeDaoFactory implements Factory<RecipeDao> {
  private final Provider<HungryWalrusDatabase> databaseProvider;

  public DatabaseModule_ProvideRecipeDaoFactory(Provider<HungryWalrusDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public RecipeDao get() {
    return provideRecipeDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideRecipeDaoFactory create(
      Provider<HungryWalrusDatabase> databaseProvider) {
    return new DatabaseModule_ProvideRecipeDaoFactory(databaseProvider);
  }

  public static RecipeDao provideRecipeDao(HungryWalrusDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideRecipeDao(database));
  }
}
