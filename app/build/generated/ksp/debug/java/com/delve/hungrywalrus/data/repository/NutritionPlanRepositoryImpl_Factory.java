package com.delve.hungrywalrus.data.repository;

import com.delve.hungrywalrus.data.local.dao.NutritionPlanDao;
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
public final class NutritionPlanRepositoryImpl_Factory implements Factory<NutritionPlanRepositoryImpl> {
  private final Provider<NutritionPlanDao> daoProvider;

  public NutritionPlanRepositoryImpl_Factory(Provider<NutritionPlanDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public NutritionPlanRepositoryImpl get() {
    return newInstance(daoProvider.get());
  }

  public static NutritionPlanRepositoryImpl_Factory create(Provider<NutritionPlanDao> daoProvider) {
    return new NutritionPlanRepositoryImpl_Factory(daoProvider);
  }

  public static NutritionPlanRepositoryImpl newInstance(NutritionPlanDao dao) {
    return new NutritionPlanRepositoryImpl(dao);
  }
}
