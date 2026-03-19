package com.delve.hungrywalrus.di;

import com.delve.hungrywalrus.data.local.HungryWalrusDatabase;
import com.delve.hungrywalrus.data.local.dao.NutritionPlanDao;
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
public final class DatabaseModule_ProvideNutritionPlanDaoFactory implements Factory<NutritionPlanDao> {
  private final Provider<HungryWalrusDatabase> databaseProvider;

  public DatabaseModule_ProvideNutritionPlanDaoFactory(
      Provider<HungryWalrusDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public NutritionPlanDao get() {
    return provideNutritionPlanDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideNutritionPlanDaoFactory create(
      Provider<HungryWalrusDatabase> databaseProvider) {
    return new DatabaseModule_ProvideNutritionPlanDaoFactory(databaseProvider);
  }

  public static NutritionPlanDao provideNutritionPlanDao(HungryWalrusDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideNutritionPlanDao(database));
  }
}
