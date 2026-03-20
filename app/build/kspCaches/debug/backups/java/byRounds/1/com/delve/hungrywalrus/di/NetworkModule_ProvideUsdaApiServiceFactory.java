package com.delve.hungrywalrus.di;

import com.delve.hungrywalrus.data.remote.usda.UsdaApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("javax.inject.Named")
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
public final class NetworkModule_ProvideUsdaApiServiceFactory implements Factory<UsdaApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideUsdaApiServiceFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public UsdaApiService get() {
    return provideUsdaApiService(retrofitProvider.get());
  }

  public static NetworkModule_ProvideUsdaApiServiceFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideUsdaApiServiceFactory(retrofitProvider);
  }

  public static UsdaApiService provideUsdaApiService(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideUsdaApiService(retrofit));
  }
}
