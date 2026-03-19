package com.delve.hungrywalrus.di;

import com.delve.hungrywalrus.data.local.HungryWalrusDatabase;
import com.delve.hungrywalrus.data.local.dao.LogEntryDao;
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
public final class DatabaseModule_ProvideLogEntryDaoFactory implements Factory<LogEntryDao> {
  private final Provider<HungryWalrusDatabase> databaseProvider;

  public DatabaseModule_ProvideLogEntryDaoFactory(Provider<HungryWalrusDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public LogEntryDao get() {
    return provideLogEntryDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideLogEntryDaoFactory create(
      Provider<HungryWalrusDatabase> databaseProvider) {
    return new DatabaseModule_ProvideLogEntryDaoFactory(databaseProvider);
  }

  public static LogEntryDao provideLogEntryDao(HungryWalrusDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideLogEntryDao(database));
  }
}
