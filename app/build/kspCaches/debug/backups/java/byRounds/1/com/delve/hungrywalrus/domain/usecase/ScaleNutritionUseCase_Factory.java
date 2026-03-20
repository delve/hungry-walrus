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
public final class ScaleNutritionUseCase_Factory implements Factory<ScaleNutritionUseCase> {
  @Override
  public ScaleNutritionUseCase get() {
    return newInstance();
  }

  public static ScaleNutritionUseCase_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ScaleNutritionUseCase newInstance() {
    return new ScaleNutritionUseCase();
  }

  private static final class InstanceHolder {
    private static final ScaleNutritionUseCase_Factory INSTANCE = new ScaleNutritionUseCase_Factory();
  }
}
