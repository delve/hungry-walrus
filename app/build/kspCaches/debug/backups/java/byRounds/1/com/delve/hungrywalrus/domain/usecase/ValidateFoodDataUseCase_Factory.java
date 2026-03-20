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
public final class ValidateFoodDataUseCase_Factory implements Factory<ValidateFoodDataUseCase> {
  @Override
  public ValidateFoodDataUseCase get() {
    return newInstance();
  }

  public static ValidateFoodDataUseCase_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ValidateFoodDataUseCase newInstance() {
    return new ValidateFoodDataUseCase();
  }

  private static final class InstanceHolder {
    private static final ValidateFoodDataUseCase_Factory INSTANCE = new ValidateFoodDataUseCase_Factory();
  }
}
