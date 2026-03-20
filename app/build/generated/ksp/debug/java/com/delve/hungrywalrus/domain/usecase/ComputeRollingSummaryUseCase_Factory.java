package com.delve.hungrywalrus.domain.usecase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class ComputeRollingSummaryUseCase_Factory implements Factory<ComputeRollingSummaryUseCase> {
  @Override
  public ComputeRollingSummaryUseCase get() {
    return newInstance();
  }

  public static ComputeRollingSummaryUseCase_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ComputeRollingSummaryUseCase newInstance() {
    return new ComputeRollingSummaryUseCase();
  }

  private static final class InstanceHolder {
    private static final ComputeRollingSummaryUseCase_Factory INSTANCE = new ComputeRollingSummaryUseCase_Factory();
  }
}
