package com.spendsnap

import android.content.Context
import androidx.room.Room
import com.spendsnap.data.local.DatabaseSeeder
import com.spendsnap.data.local.SpendSnapDatabase
import com.spendsnap.data.local.dao.CategoryDao
import com.spendsnap.data.local.dao.ExpenseDao
import com.spendsnap.data.remote.FakeSpendSnapApi
import com.spendsnap.data.remote.SpendSnapApi
import com.spendsnap.data.repository.ExpenseRepository
import com.spendsnap.data.repository.OfflineFirstExpenseRepository
import com.spendsnap.sync.SyncScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TestAppModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): SpendSnapDatabase =
        Room.inMemoryDatabaseBuilder(context, SpendSnapDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(DatabaseSeeder())
            .build()

    @Provides
    fun provideExpenseDao(database: SpendSnapDatabase): ExpenseDao = database.expenseDao()

    @Provides
    fun provideCategoryDao(database: SpendSnapDatabase): CategoryDao = database.categoryDao()

    @Provides
    @Singleton
    fun provideFakeApi(): FakeSpendSnapApi = FakeSpendSnapApi(delayMillis = 0)

    @Provides
    @Singleton
    fun provideApi(fake: FakeSpendSnapApi): SpendSnapApi = fake

    @Provides
    @Singleton
    fun provideSyncScheduler(): SyncScheduler =
        object : SyncScheduler {
            override fun requestImmediateSync() = Unit

            override fun ensurePeriodicSync() = Unit
        }

    @Provides
    @Singleton
    fun provideRepository(
        expenseDao: ExpenseDao,
        categoryDao: CategoryDao,
        api: SpendSnapApi,
        scheduler: SyncScheduler,
    ): ExpenseRepository =
        OfflineFirstExpenseRepository(
            expenseDao = expenseDao,
            categoryDao = categoryDao,
            api = api,
            syncScheduler = scheduler,
        )
}
