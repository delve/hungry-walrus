package com.delve.hungrywalrus.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.delve.hungrywalrus.data.local.dao.FoodCacheDao;
import com.delve.hungrywalrus.data.local.dao.LogEntryDao;
import dagger.internal.DaggerGenerated;
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
public final class DataRetentionWorker_Factory {
  private final Provider<LogEntryDao> logEntryDaoProvider;

  private final Provider<FoodCacheDao> foodCacheDaoProvider;

  public DataRetentionWorker_Factory(Provider<LogEntryDao> logEntryDaoProvider,
      Provider<FoodCacheDao> foodCacheDaoProvider) {
    this.logEntryDaoProvider = logEntryDaoProvider;
    this.foodCacheDaoProvider = foodCacheDaoProvider;
  }

  public DataRetentionWorker get(Context context, WorkerParameters params) {
    return newInstance(context, params, logEntryDaoProvider.get(), foodCacheDaoProvider.get());
  }

  public static DataRetentionWorker_Factory create(Provider<LogEntryDao> logEntryDaoProvider,
      Provider<FoodCacheDao> foodCacheDaoProvider) {
    return new DataRetentionWorker_Factory(logEntryDaoProvider, foodCacheDaoProvider);
  }

  public static DataRetentionWorker newInstance(Context context, WorkerParameters params,
      LogEntryDao logEntryDao, FoodCacheDao foodCacheDao) {
    return new DataRetentionWorker(context, params, logEntryDao, foodCacheDao);
  }
}
