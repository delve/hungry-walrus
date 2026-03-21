package com.delve.hungrywalrus.ui.screen.plan;

import com.delve.hungrywalrus.data.repository.NutritionPlanRepository;
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
public final class PlanViewModel_Factory implements Factory<PlanViewModel> {
  private final Provider<NutritionPlanRepository> planRepoProvider;

  public PlanViewModel_Factory(Provider<NutritionPlanRepository> planRepoProvider) {
    this.planRepoProvider = planRepoProvider;
  }

  @Override
  public PlanViewModel get() {
    return newInstance(planRepoProvider.get());
  }

  public static PlanViewModel_Factory create(Provider<NutritionPlanRepository> planRepoProvider) {
    return new PlanViewModel_Factory(planRepoProvider);
  }

  public static PlanViewModel newInstance(NutritionPlanRepository planRepo) {
    return new PlanViewModel(planRepo);
  }
}
