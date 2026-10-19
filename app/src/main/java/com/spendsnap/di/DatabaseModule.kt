package com.spendsnap.di

import android.content.Context
import androidx.room.Room
import com.spendsnap.data.local.DatabaseSeeder
import com.spendsnap.data.local.SpendSnapDatabase
import com.spendsnap.data.local.dao.CategoryDao
import com.spendsnap.data.local.dao.ExpenseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): SpendSnapDatabase = Room.databaseBuilder(
        context,
        SpendSnapDatabase::class.java,
        SpendSnapDatabase.NAME,
    )
        .addCallback(DatabaseSeeder())
        .build()

    @Provides
    fun provideExpenseDao(database: SpendSnapDatabase): ExpenseDao = database.expenseDao()

    @Provides
    fun provideCategoryDao(database: SpendSnapDatabase): CategoryDao = database.categoryDao()
}
