package com.delve.hungrywalrus.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class DataRetentionWorker_AssistedFactory_Impl implements DataRetentionWorker_AssistedFactory {
  private final DataRetentionWorker_Factory delegateFactory;

  DataRetentionWorker_AssistedFactory_Impl(DataRetentionWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public DataRetentionWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<DataRetentionWorker_AssistedFactory> create(
      DataRetentionWorker_Factory delegateFactory) {
    return InstanceFactory.create(new DataRetentionWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<DataRetentionWorker_AssistedFactory> createFactoryProvider(
      DataRetentionWorker_Factory delegateFactory) {
    return InstanceFactory.create(new DataRetentionWorker_AssistedFactory_Impl(delegateFactory));
  }
}
