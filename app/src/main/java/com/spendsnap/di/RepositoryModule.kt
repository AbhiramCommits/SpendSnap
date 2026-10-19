package com.spendsnap.di

import com.spendsnap.data.repository.ExpenseRepository
import com.spendsnap.data.repository.OfflineFirstExpenseRepository
import com.spendsnap.sync.NoOpSyncScheduler
import com.spendsnap.sync.SyncScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindExpenseRepository(
        repository: OfflineFirstExpenseRepository,
    ): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindSyncScheduler(
        scheduler: NoOpSyncScheduler,
    ): SyncScheduler
}
