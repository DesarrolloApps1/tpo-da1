package com.takeabreak.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BreakSessionEntity::class, BusinessEntity::class, RewardEntity::class, RedemptionEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun breakDao(): BreakDao
    abstract fun catalogDao(): CatalogDao
    abstract fun redemptionDao(): RedemptionDao

    companion object {
        // Las pausas ya guardadas quedan sin bonus
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE break_sessions ADD COLUMN bonusPoints INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
