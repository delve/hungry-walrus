package com.delve.hungrywalrus.data.repository;

import com.delve.hungrywalrus.data.local.dao.FoodCacheDao;
import com.delve.hungrywalrus.data.remote.openfoodfacts.OffApiService;
import com.delve.hungrywalrus.data.remote.usda.UsdaApiService;
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
public final class FoodLookupRepositoryImpl_Factory implements Factory<FoodLookupRepositoryImpl> {
  private final Provider<UsdaApiService> usdaApiServiceProvider;

  private final Provider<OffApiService> offApiServiceProvider;

  private final Provider<FoodCacheDao> foodCacheDaoProvider;

  public FoodLookupRepositoryImpl_Factory(Provider<UsdaApiService> usdaApiServiceProvider,
      Provider<OffApiService> offApiServiceProvider, Provider<FoodCacheDao> foodCacheDaoProvider) {
    this.usdaApiServiceProvider = usdaApiServiceProvider;
    this.offApiServiceProvider = offApiServiceProvider;
    this.foodCacheDaoProvider = foodCacheDaoProvider;
  }

  @Override
  public FoodLookupRepositoryImpl get() {
    return newInstance(usdaApiServiceProvider.get(), offApiServiceProvider.get(), foodCacheDaoProvider.get());
  }

  public static FoodLookupRepositoryImpl_Factory create(
      Provider<UsdaApiService> usdaApiServiceProvider,
      Provider<OffApiService> offApiServiceProvider, Provider<FoodCacheDao> foodCacheDaoProvider) {
    return new FoodLookupRepositoryImpl_Factory(usdaApiServiceProvider, offApiServiceProvider, foodCacheDaoProvider);
  }

  public static FoodLookupRepositoryImpl newInstance(UsdaApiService usdaApiService,
      OffApiService offApiService, FoodCacheDao foodCacheDao) {
    return new FoodLookupRepositoryImpl(usdaApiService, offApiService, foodCacheDao);
  }
}
