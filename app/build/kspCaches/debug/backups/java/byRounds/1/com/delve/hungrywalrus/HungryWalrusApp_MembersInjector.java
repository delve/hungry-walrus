package com.delve.hungrywalrus;

import androidx.hilt.work.HiltWorkerFactory;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class HungryWalrusApp_MembersInjector implements MembersInjector<HungryWalrusApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public HungryWalrusApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<HungryWalrusApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new HungryWalrusApp_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(HungryWalrusApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.delve.hungrywalrus.HungryWalrusApp.workerFactory")
  public static void injectWorkerFactory(HungryWalrusApp instance,
      HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
