package com.delve.hungrywalrus.ui.screen.summaries;

import com.delve.hungrywalrus.data.repository.LogEntryRepository;
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository;
import com.delve.hungrywalrus.domain.usecase.ComputeRollingSummaryUseCase;
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
public final class SummariesViewModel_Factory implements Factory<SummariesViewModel> {
  private final Provider<LogEntryRepository> logRepoProvider;

  private final Provider<NutritionPlanRepository> planRepoProvider;

  private final Provider<ComputeRollingSummaryUseCase> computeSummaryUseCaseProvider;

  public SummariesViewModel_Factory(Provider<LogEntryRepository> logRepoProvider,
      Provider<NutritionPlanRepository> planRepoProvider,
      Provider<ComputeRollingSummaryUseCase> computeSummaryUseCaseProvider) {
    this.logRepoProvider = logRepoProvider;
    this.planRepoProvider = planRepoProvider;
    this.computeSummaryUseCaseProvider = computeSummaryUseCaseProvider;
  }

  @Override
  public SummariesViewModel get() {
    return newInstance(logRepoProvider.get(), planRepoProvider.get(), computeSummaryUseCaseProvider.get());
  }

  public static SummariesViewModel_Factory create(Provider<LogEntryRepository> logRepoProvider,
      Provider<NutritionPlanRepository> planRepoProvider,
      Provider<ComputeRollingSummaryUseCase> computeSummaryUseCaseProvider) {
    return new SummariesViewModel_Factory(logRepoProvider, planRepoProvider, computeSummaryUseCaseProvider);
  }

  public static SummariesViewModel newInstance(LogEntryRepository logRepo,
      NutritionPlanRepository planRepo, ComputeRollingSummaryUseCase computeSummaryUseCase) {
    return new SummariesViewModel(logRepo, planRepo, computeSummaryUseCase);
  }
}
