package com.delve.hungrywalrus.data.repository;

import com.delve.hungrywalrus.data.local.dao.LogEntryDao;
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
public final class LogEntryRepositoryImpl_Factory implements Factory<LogEntryRepositoryImpl> {
  private final Provider<LogEntryDao> daoProvider;

  public LogEntryRepositoryImpl_Factory(Provider<LogEntryDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public LogEntryRepositoryImpl get() {
    return newInstance(daoProvider.get());
  }

  public static LogEntryRepositoryImpl_Factory create(Provider<LogEntryDao> daoProvider) {
    return new LogEntryRepositoryImpl_Factory(daoProvider);
  }

  public static LogEntryRepositoryImpl newInstance(LogEntryDao dao) {
    return new LogEntryRepositoryImpl(dao);
  }
}
