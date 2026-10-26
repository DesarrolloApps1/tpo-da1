package com.takeabreak.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BreakSessionEntity::class, BusinessEntity::class, RewardEntity::class, RedemptionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun breakDao(): BreakDao
    abstract fun catalogDao(): CatalogDao
    abstract fun redemptionDao(): RedemptionDao
}
