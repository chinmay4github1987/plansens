package com.prasjaychi.plantsense.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database instance managing local persistence for plant analysis history and diagnoses.
 */
@Database(
    entities = [
        PlantAnalysisEntity::class,
        PlantReminderEntity::class,
        SyncOperationEntity::class,
        PlantHealthRecordEntity::class,
        UserSettingsEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class PlantDatabase : RoomDatabase() {

    abstract fun plantAnalysisDao(): PlantAnalysisDao
    abstract fun plantReminderDao(): PlantReminderDao
    abstract fun syncOperationDao(): SyncOperationDao
    abstract fun plantHealthRecordDao(): PlantHealthRecordDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: PlantDatabase? = null

        fun getDatabase(context: Context): PlantDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlantDatabase::class.java,
                    "plant_diagnostics_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
