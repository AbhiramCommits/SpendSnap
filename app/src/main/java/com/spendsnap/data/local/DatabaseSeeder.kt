package com.spendsnap.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.spendsnap.data.local.dao.CategoryDao
import com.spendsnap.data.local.entity.CategoryEntity

/**
 * Creates a useful category set the first time the database is opened.
 * Stable IDs make CSV imports and seed updates idempotent.
 */
class DatabaseSeeder : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        DEFAULT_CATEGORIES.forEach { category ->
            db.execSQL(
                "INSERT OR IGNORE INTO categories (id, name, colorHex, isDefault) VALUES (?, ?, ?, ?)",
                arrayOf(category.id, category.name, category.colorHex, if (category.isDefault) 1 else 0),
            )
        }
    }

    suspend fun seed(categoryDao: CategoryDao) {
        if (categoryDao.count() == 0) {
            categoryDao.upsertAll(DEFAULT_CATEGORIES)
        }
    }

    companion object {
        const val OTHER_CATEGORY_ID = "category-other"

        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity("category-meals", "Meals", "#C8553D", true),
            CategoryEntity("category-travel", "Travel", "#2F6F8F", true),
            CategoryEntity("category-software", "Software", "#6B5CA5", true),
            CategoryEntity("category-office", "Office", "#A06A2C", true),
            CategoryEntity("category-marketing", "Marketing", "#B4477A", true),
            CategoryEntity("category-hardware", "Hardware", "#3C7A52", true),
            CategoryEntity("category-fees", "Fees", "#7B6250", true),
            CategoryEntity(OTHER_CATEGORY_ID, "Other", "#6C757D", true),
        )
    }
}
