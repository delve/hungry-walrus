package com.delve.hungrywalrus;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.hilt.work.WorkerAssistedFactory;
import androidx.hilt.work.WorkerFactoryModule_ProvideFactoryFactory;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import com.delve.hungrywalrus.data.local.HungryWalrusDatabase;
import com.delve.hungrywalrus.data.local.dao.FoodCacheDao;
import com.delve.hungrywalrus.data.local.dao.LogEntryDao;
import com.delve.hungrywalrus.data.local.dao.NutritionPlanDao;
import com.delve.hungrywalrus.data.local.dao.RecipeDao;
import com.delve.hungrywalrus.data.local.dao.RecipeIngredientDao;
import com.delve.hungrywalrus.data.remote.openfoodfacts.OffApiService;
import com.delve.hungrywalrus.data.remote.usda.UsdaApiService;
import com.delve.hungrywalrus.data.repository.FoodLookupRepository;
import com.delve.hungrywalrus.data.repository.FoodLookupRepositoryImpl;
import com.delve.hungrywalrus.data.repository.LogEntryRepository;
import com.delve.hungrywalrus.data.repository.LogEntryRepositoryImpl;
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository;
import com.delve.hungrywalrus.data.repository.NutritionPlanRepositoryImpl;
import com.delve.hungrywalrus.data.repository.RecipeRepository;
import com.delve.hungrywalrus.data.repository.RecipeRepositoryImpl;
import com.delve.hungrywalrus.di.DatabaseModule_ProvideDatabaseFactory;
import com.delve.hungrywalrus.di.DatabaseModule_ProvideFoodCacheDaoFactory;
import com.delve.hungrywalrus.di.DatabaseModule_ProvideLogEntryDaoFactory;
import com.delve.hungrywalrus.di.DatabaseModule_ProvideNutritionPlanDaoFactory;
import com.delve.hungrywalrus.di.DatabaseModule_ProvideRecipeDaoFactory;
import com.delve.hungrywalrus.di.DatabaseModule_ProvideRecipeIngredientDaoFactory;
import com.delve.hungrywalrus.di.NetworkModule_ProvideEncryptedSharedPreferencesFactory;
import com.delve.hungrywalrus.di.NetworkModule_ProvideJsonFactory;
import com.delve.hungrywalrus.di.NetworkModule_ProvideOffApiServiceFactory;
import com.delve.hungrywalrus.di.NetworkModule_ProvideOffOkHttpClientFactory;
import com.delve.hungrywalrus.di.NetworkModule_ProvideOffRetrofitFactory;
import com.delve.hungrywalrus.di.NetworkModule_ProvideUsdaApiServiceFactory;
import com.delve.hungrywalrus.di.NetworkModule_ProvideUsdaOkHttpClientFactory;
import com.delve.hungrywalrus.di.NetworkModule_ProvideUsdaRetrofitFactory;
import com.delve.hungrywalrus.domain.usecase.ComputeRollingSummaryUseCase;
import com.delve.hungrywalrus.domain.usecase.ScaleNutritionUseCase;
import com.delve.hungrywalrus.domain.usecase.ValidateFoodDataUseCase;
import com.delve.hungrywalrus.ui.screen.addentry.AddEntryViewModel;
import com.delve.hungrywalrus.ui.screen.addentry.AddEntryViewModel_HiltModules;
import com.delve.hungrywalrus.ui.screen.addentry.AddEntryViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.delve.hungrywalrus.ui.screen.addentry.AddEntryViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.delve.hungrywalrus.ui.screen.createrecipe.CreateRecipeViewModel;
import com.delve.hungrywalrus.ui.screen.createrecipe.CreateRecipeViewModel_HiltModules;
import com.delve.hungrywalrus.ui.screen.createrecipe.CreateRecipeViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.delve.hungrywalrus.ui.screen.createrecipe.CreateRecipeViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.delve.hungrywalrus.ui.screen.dailyprogress.DailyProgressViewModel;
import com.delve.hungrywalrus.ui.screen.dailyprogress.DailyProgressViewModel_HiltModules;
import com.delve.hungrywalrus.ui.screen.dailyprogress.DailyProgressViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.delve.hungrywalrus.ui.screen.dailyprogress.DailyProgressViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.delve.hungrywalrus.ui.screen.plan.PlanViewModel;
import com.delve.hungrywalrus.ui.screen.plan.PlanViewModel_HiltModules;
import com.delve.hungrywalrus.ui.screen.plan.PlanViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.delve.hungrywalrus.ui.screen.plan.PlanViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.delve.hungrywalrus.ui.screen.recipes.RecipeDetailViewModel;
import com.delve.hungrywalrus.ui.screen.recipes.RecipeDetailViewModel_HiltModules;
import com.delve.hungrywalrus.ui.screen.recipes.RecipeDetailViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.delve.hungrywalrus.ui.screen.recipes.RecipeDetailViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.delve.hungrywalrus.ui.screen.recipes.RecipeListViewModel;
import com.delve.hungrywalrus.ui.screen.recipes.RecipeListViewModel_HiltModules;
import com.delve.hungrywalrus.ui.screen.recipes.RecipeListViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.delve.hungrywalrus.ui.screen.recipes.RecipeListViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.delve.hungrywalrus.ui.screen.settings.SettingsViewModel;
import com.delve.hungrywalrus.ui.screen.settings.SettingsViewModel_HiltModules;
import com.delve.hungrywalrus.ui.screen.settings.SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.delve.hungrywalrus.ui.screen.settings.SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.delve.hungrywalrus.ui.screen.summaries.SummariesViewModel;
import com.delve.hungrywalrus.ui.screen.summaries.SummariesViewModel_HiltModules;
import com.delve.hungrywalrus.ui.screen.summaries.SummariesViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.delve.hungrywalrus.ui.screen.summaries.SummariesViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.delve.hungrywalrus.util.ApiKeyStore;
import com.delve.hungrywalrus.worker.DataRetentionWorker;
import com.delve.hungrywalrus.worker.DataRetentionWorker_AssistedFactory;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.SingleCheck;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

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
public final class DaggerHungryWalrusApp_HiltComponents_SingletonC {
  private DaggerHungryWalrusApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public HungryWalrusApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements HungryWalrusApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public HungryWalrusApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements HungryWalrusApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public HungryWalrusApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements HungryWalrusApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public HungryWalrusApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements HungryWalrusApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public HungryWalrusApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements HungryWalrusApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public HungryWalrusApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements HungryWalrusApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public HungryWalrusApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements HungryWalrusApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public HungryWalrusApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends HungryWalrusApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends HungryWalrusApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends HungryWalrusApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends HungryWalrusApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(8).put(AddEntryViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, AddEntryViewModel_HiltModules.KeyModule.provide()).put(CreateRecipeViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, CreateRecipeViewModel_HiltModules.KeyModule.provide()).put(DailyProgressViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, DailyProgressViewModel_HiltModules.KeyModule.provide()).put(PlanViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, PlanViewModel_HiltModules.KeyModule.provide()).put(RecipeDetailViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, RecipeDetailViewModel_HiltModules.KeyModule.provide()).put(RecipeListViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, RecipeListViewModel_HiltModules.KeyModule.provide()).put(SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SettingsViewModel_HiltModules.KeyModule.provide()).put(SummariesViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SummariesViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }
  }

  private static final class ViewModelCImpl extends HungryWalrusApp_HiltComponents.ViewModelC {
    private final SavedStateHandle savedStateHandle;

    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AddEntryViewModel> addEntryViewModelProvider;

    private Provider<CreateRecipeViewModel> createRecipeViewModelProvider;

    private Provider<DailyProgressViewModel> dailyProgressViewModelProvider;

    private Provider<PlanViewModel> planViewModelProvider;

    private Provider<RecipeDetailViewModel> recipeDetailViewModelProvider;

    private Provider<RecipeListViewModel> recipeListViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private Provider<SummariesViewModel> summariesViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.savedStateHandle = savedStateHandleParam;
      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.addEntryViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.createRecipeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.dailyProgressViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.planViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.recipeDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.recipeListViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.summariesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(8).put(AddEntryViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) addEntryViewModelProvider)).put(CreateRecipeViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) createRecipeViewModelProvider)).put(DailyProgressViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) dailyProgressViewModelProvider)).put(PlanViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) planViewModelProvider)).put(RecipeDetailViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) recipeDetailViewModelProvider)).put(RecipeListViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) recipeListViewModelProvider)).put(SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) settingsViewModelProvider)).put(SummariesViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) summariesViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.delve.hungrywalrus.ui.screen.addentry.AddEntryViewModel 
          return (T) new AddEntryViewModel(singletonCImpl.bindLogEntryRepositoryProvider.get(), singletonCImpl.bindFoodLookupRepositoryProvider.get(), singletonCImpl.bindRecipeRepositoryProvider.get(), new ScaleNutritionUseCase(), new ValidateFoodDataUseCase(), singletonCImpl.apiKeyStoreProvider.get());

          case 1: // com.delve.hungrywalrus.ui.screen.createrecipe.CreateRecipeViewModel 
          return (T) new CreateRecipeViewModel(singletonCImpl.bindRecipeRepositoryProvider.get(), new ScaleNutritionUseCase(), viewModelCImpl.savedStateHandle);

          case 2: // com.delve.hungrywalrus.ui.screen.dailyprogress.DailyProgressViewModel 
          return (T) new DailyProgressViewModel(singletonCImpl.bindNutritionPlanRepositoryProvider.get(), singletonCImpl.bindLogEntryRepositoryProvider.get());

          case 3: // com.delve.hungrywalrus.ui.screen.plan.PlanViewModel 
          return (T) new PlanViewModel(singletonCImpl.bindNutritionPlanRepositoryProvider.get());

          case 4: // com.delve.hungrywalrus.ui.screen.recipes.RecipeDetailViewModel 
          return (T) new RecipeDetailViewModel(singletonCImpl.bindRecipeRepositoryProvider.get(), viewModelCImpl.savedStateHandle);

          case 5: // com.delve.hungrywalrus.ui.screen.recipes.RecipeListViewModel 
          return (T) new RecipeListViewModel(singletonCImpl.bindRecipeRepositoryProvider.get());

          case 6: // com.delve.hungrywalrus.ui.screen.settings.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.apiKeyStoreProvider.get());

          case 7: // com.delve.hungrywalrus.ui.screen.summaries.SummariesViewModel 
          return (T) new SummariesViewModel(singletonCImpl.bindLogEntryRepositoryProvider.get(), singletonCImpl.bindNutritionPlanRepositoryProvider.get(), new ComputeRollingSummaryUseCase());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends HungryWalrusApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends HungryWalrusApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends HungryWalrusApp_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<HungryWalrusDatabase> provideDatabaseProvider;

    private Provider<LogEntryDao> provideLogEntryDaoProvider;

    private Provider<FoodCacheDao> provideFoodCacheDaoProvider;

    private Provider<DataRetentionWorker_AssistedFactory> dataRetentionWorker_AssistedFactoryProvider;

    private Provider<LogEntryRepositoryImpl> logEntryRepositoryImplProvider;

    private Provider<LogEntryRepository> bindLogEntryRepositoryProvider;

    private Provider<SharedPreferences> provideEncryptedSharedPreferencesProvider;

    private Provider<OkHttpClient> provideUsdaOkHttpClientProvider;

    private Provider<Json> provideJsonProvider;

    private Provider<Retrofit> provideUsdaRetrofitProvider;

    private Provider<UsdaApiService> provideUsdaApiServiceProvider;

    private Provider<OkHttpClient> provideOffOkHttpClientProvider;

    private Provider<Retrofit> provideOffRetrofitProvider;

    private Provider<OffApiService> provideOffApiServiceProvider;

    private Provider<FoodLookupRepositoryImpl> foodLookupRepositoryImplProvider;

    private Provider<FoodLookupRepository> bindFoodLookupRepositoryProvider;

    private Provider<RecipeDao> provideRecipeDaoProvider;

    private Provider<RecipeIngredientDao> provideRecipeIngredientDaoProvider;

    private Provider<RecipeRepositoryImpl> recipeRepositoryImplProvider;

    private Provider<RecipeRepository> bindRecipeRepositoryProvider;

    private Provider<ApiKeyStore> apiKeyStoreProvider;

    private Provider<NutritionPlanDao> provideNutritionPlanDaoProvider;

    private Provider<NutritionPlanRepositoryImpl> nutritionPlanRepositoryImplProvider;

    private Provider<NutritionPlanRepository> bindNutritionPlanRepositoryProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private Map<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>> mapOfStringAndProviderOfWorkerAssistedFactoryOf(
        ) {
      return Collections.<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>>singletonMap("com.delve.hungrywalrus.worker.DataRetentionWorker", ((Provider) dataRetentionWorker_AssistedFactoryProvider));
    }

    private HiltWorkerFactory hiltWorkerFactory() {
      return WorkerFactoryModule_ProvideFactoryFactory.provideFactory(mapOfStringAndProviderOfWorkerAssistedFactoryOf());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<HungryWalrusDatabase>(singletonCImpl, 2));
      this.provideLogEntryDaoProvider = DoubleCheck.provider(new SwitchingProvider<LogEntryDao>(singletonCImpl, 1));
      this.provideFoodCacheDaoProvider = DoubleCheck.provider(new SwitchingProvider<FoodCacheDao>(singletonCImpl, 3));
      this.dataRetentionWorker_AssistedFactoryProvider = SingleCheck.provider(new SwitchingProvider<DataRetentionWorker_AssistedFactory>(singletonCImpl, 0));
      this.logEntryRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 4);
      this.bindLogEntryRepositoryProvider = DoubleCheck.provider((Provider) logEntryRepositoryImplProvider);
      this.provideEncryptedSharedPreferencesProvider = DoubleCheck.provider(new SwitchingProvider<SharedPreferences>(singletonCImpl, 9));
      this.provideUsdaOkHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 8));
      this.provideJsonProvider = DoubleCheck.provider(new SwitchingProvider<Json>(singletonCImpl, 10));
      this.provideUsdaRetrofitProvider = DoubleCheck.provider(new SwitchingProvider<Retrofit>(singletonCImpl, 7));
      this.provideUsdaApiServiceProvider = DoubleCheck.provider(new SwitchingProvider<UsdaApiService>(singletonCImpl, 6));
      this.provideOffOkHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 13));
      this.provideOffRetrofitProvider = DoubleCheck.provider(new SwitchingProvider<Retrofit>(singletonCImpl, 12));
      this.provideOffApiServiceProvider = DoubleCheck.provider(new SwitchingProvider<OffApiService>(singletonCImpl, 11));
      this.foodLookupRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 5);
      this.bindFoodLookupRepositoryProvider = DoubleCheck.provider((Provider) foodLookupRepositoryImplProvider);
      this.provideRecipeDaoProvider = DoubleCheck.provider(new SwitchingProvider<RecipeDao>(singletonCImpl, 15));
      this.provideRecipeIngredientDaoProvider = DoubleCheck.provider(new SwitchingProvider<RecipeIngredientDao>(singletonCImpl, 16));
      this.recipeRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 14);
      this.bindRecipeRepositoryProvider = DoubleCheck.provider((Provider) recipeRepositoryImplProvider);
      this.apiKeyStoreProvider = DoubleCheck.provider(new SwitchingProvider<ApiKeyStore>(singletonCImpl, 17));
      this.provideNutritionPlanDaoProvider = DoubleCheck.provider(new SwitchingProvider<NutritionPlanDao>(singletonCImpl, 19));
      this.nutritionPlanRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 18);
      this.bindNutritionPlanRepositoryProvider = DoubleCheck.provider((Provider) nutritionPlanRepositoryImplProvider);
    }

    @Override
    public void injectHungryWalrusApp(HungryWalrusApp hungryWalrusApp) {
      injectHungryWalrusApp2(hungryWalrusApp);
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private HungryWalrusApp injectHungryWalrusApp2(HungryWalrusApp instance) {
      HungryWalrusApp_MembersInjector.injectWorkerFactory(instance, hiltWorkerFactory());
      return instance;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.delve.hungrywalrus.worker.DataRetentionWorker_AssistedFactory 
          return (T) new DataRetentionWorker_AssistedFactory() {
            @Override
            public DataRetentionWorker create(Context context, WorkerParameters params) {
              return new DataRetentionWorker(context, params, singletonCImpl.provideLogEntryDaoProvider.get(), singletonCImpl.provideFoodCacheDaoProvider.get());
            }
          };

          case 1: // com.delve.hungrywalrus.data.local.dao.LogEntryDao 
          return (T) DatabaseModule_ProvideLogEntryDaoFactory.provideLogEntryDao(singletonCImpl.provideDatabaseProvider.get());

          case 2: // com.delve.hungrywalrus.data.local.HungryWalrusDatabase 
          return (T) DatabaseModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 3: // com.delve.hungrywalrus.data.local.dao.FoodCacheDao 
          return (T) DatabaseModule_ProvideFoodCacheDaoFactory.provideFoodCacheDao(singletonCImpl.provideDatabaseProvider.get());

          case 4: // com.delve.hungrywalrus.data.repository.LogEntryRepositoryImpl 
          return (T) new LogEntryRepositoryImpl(singletonCImpl.provideLogEntryDaoProvider.get());

          case 5: // com.delve.hungrywalrus.data.repository.FoodLookupRepositoryImpl 
          return (T) new FoodLookupRepositoryImpl(singletonCImpl.provideUsdaApiServiceProvider.get(), singletonCImpl.provideOffApiServiceProvider.get(), singletonCImpl.provideFoodCacheDaoProvider.get());

          case 6: // com.delve.hungrywalrus.data.remote.usda.UsdaApiService 
          return (T) NetworkModule_ProvideUsdaApiServiceFactory.provideUsdaApiService(singletonCImpl.provideUsdaRetrofitProvider.get());

          case 7: // @javax.inject.Named("usda") retrofit2.Retrofit 
          return (T) NetworkModule_ProvideUsdaRetrofitFactory.provideUsdaRetrofit(singletonCImpl.provideUsdaOkHttpClientProvider.get(), singletonCImpl.provideJsonProvider.get());

          case 8: // @javax.inject.Named("usda") okhttp3.OkHttpClient 
          return (T) NetworkModule_ProvideUsdaOkHttpClientFactory.provideUsdaOkHttpClient(singletonCImpl.provideEncryptedSharedPreferencesProvider.get());

          case 9: // android.content.SharedPreferences 
          return (T) NetworkModule_ProvideEncryptedSharedPreferencesFactory.provideEncryptedSharedPreferences(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 10: // kotlinx.serialization.json.Json 
          return (T) NetworkModule_ProvideJsonFactory.provideJson();

          case 11: // com.delve.hungrywalrus.data.remote.openfoodfacts.OffApiService 
          return (T) NetworkModule_ProvideOffApiServiceFactory.provideOffApiService(singletonCImpl.provideOffRetrofitProvider.get());

          case 12: // @javax.inject.Named("off") retrofit2.Retrofit 
          return (T) NetworkModule_ProvideOffRetrofitFactory.provideOffRetrofit(singletonCImpl.provideOffOkHttpClientProvider.get(), singletonCImpl.provideJsonProvider.get());

          case 13: // @javax.inject.Named("off") okhttp3.OkHttpClient 
          return (T) NetworkModule_ProvideOffOkHttpClientFactory.provideOffOkHttpClient();

          case 14: // com.delve.hungrywalrus.data.repository.RecipeRepositoryImpl 
          return (T) new RecipeRepositoryImpl(singletonCImpl.provideDatabaseProvider.get(), singletonCImpl.provideRecipeDaoProvider.get(), singletonCImpl.provideRecipeIngredientDaoProvider.get());

          case 15: // com.delve.hungrywalrus.data.local.dao.RecipeDao 
          return (T) DatabaseModule_ProvideRecipeDaoFactory.provideRecipeDao(singletonCImpl.provideDatabaseProvider.get());

          case 16: // com.delve.hungrywalrus.data.local.dao.RecipeIngredientDao 
          return (T) DatabaseModule_ProvideRecipeIngredientDaoFactory.provideRecipeIngredientDao(singletonCImpl.provideDatabaseProvider.get());

          case 17: // com.delve.hungrywalrus.util.ApiKeyStore 
          return (T) new ApiKeyStore(singletonCImpl.provideEncryptedSharedPreferencesProvider.get());

          case 18: // com.delve.hungrywalrus.data.repository.NutritionPlanRepositoryImpl 
          return (T) new NutritionPlanRepositoryImpl(singletonCImpl.provideNutritionPlanDaoProvider.get());

          case 19: // com.delve.hungrywalrus.data.local.dao.NutritionPlanDao 
          return (T) DatabaseModule_ProvideNutritionPlanDaoFactory.provideNutritionPlanDao(singletonCImpl.provideDatabaseProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
