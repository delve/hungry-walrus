package com.delve.hungrywalrus.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides system-level dependencies that are referenced by ViewModels but otherwise
 * would tie them to non-injectable static APIs (`LocalTime.now()`, `LocalDate.now()`).
 *
 * The [Clock] binding here is used by `SummariesViewModel` to evaluate the 20:00
 * rolling-window cutoff (architecture §7.6). Production code uses
 * [Clock.systemDefaultZone] so the cutoff is computed in the device's local time
 * zone, matching the user's intuition of "today" and "yesterday". Unit tests can
 * provide a fixed `Clock` to deterministically verify behaviour at 19:59 vs 20:00
 * vs 20:01.
 */
@Module
@InstallIn(SingletonComponent::class)
object SystemModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
