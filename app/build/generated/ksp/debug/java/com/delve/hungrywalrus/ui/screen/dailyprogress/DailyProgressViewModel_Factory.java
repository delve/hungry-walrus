package com.delve.hungrywalrus.ui.screen.dailyprogress;

import com.delve.hungrywalrus.data.repository.LogEntryRepository;
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
public final class DailyProgressViewModel_Factory implements Factory<DailyProgressViewModel> {
  private final Provider<NutritionPlanRepository> planRepoProvider;

  private final Provider<LogEntryRepository> logRepoProvider;

  public DailyProgressViewModel_Factory(Provider<NutritionPlanRepository> planRepoProvider,
      Provider<LogEntryRepository> logRepoProvider) {
    this.planRepoProvider = planRepoProvider;
    this.logRepoProvider = logRepoProvider;
  }

  @Override
  public DailyProgressViewModel get() {
    return newInstance(planRepoProvider.get(), logRepoProvider.get());
  }

  public static DailyProgressViewModel_Factory create(
      Provider<NutritionPlanRepository> planRepoProvider,
      Provider<LogEntryRepository> logRepoProvider) {
    return new DailyProgressViewModel_Factory(planRepoProvider, logRepoProvider);
  }

  public static DailyProgressViewModel newInstance(NutritionPlanRepository planRepo,
      LogEntryRepository logRepo) {
    return new DailyProgressViewModel(planRepo, logRepo);
  }
}
