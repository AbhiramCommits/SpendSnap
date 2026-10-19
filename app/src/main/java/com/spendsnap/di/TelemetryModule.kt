package com.spendsnap.di

import com.spendsnap.telemetry.CrashlyticsTelemetry
import com.spendsnap.telemetry.Telemetry
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TelemetryModule {
    @Binds
    @Singleton
    abstract fun bindTelemetry(telemetry: CrashlyticsTelemetry): Telemetry
}
