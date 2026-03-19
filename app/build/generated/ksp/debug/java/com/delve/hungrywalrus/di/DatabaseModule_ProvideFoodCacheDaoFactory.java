package com.delve.hungrywalrus.di;

import com.delve.hungrywalrus.data.local.HungryWalrusDatabase;
import com.delve.hungrywalrus.data.local.dao.FoodCacheDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideFoodCacheDaoFactory implements Factory<FoodCacheDao> {
  private final Provider<HungryWalrusDatabase> databaseProvider;

  public DatabaseModule_ProvideFoodCacheDaoFactory(
      Provider<HungryWalrusDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public FoodCacheDao get() {
    return provideFoodCacheDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideFoodCacheDaoFactory create(
      Provider<HungryWalrusDatabase> databaseProvider) {
    return new DatabaseModule_ProvideFoodCacheDaoFactory(databaseProvider);
  }

  public static FoodCacheDao provideFoodCacheDao(HungryWalrusDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideFoodCacheDao(database));
  }
}
