package com.spendsnap.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.spendsnap.data.local.dao.CategoryDao
import com.spendsnap.data.local.dao.ExpenseDao
import com.spendsnap.data.local.entity.CategoryEntity
import com.spendsnap.data.local.entity.ExpenseEntity

@Database(
    entities = [ExpenseEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SpendSnapDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    abstract fun categoryDao(): CategoryDao

    companion object {
        const val NAME = "spendsnap.db"
    }
}
