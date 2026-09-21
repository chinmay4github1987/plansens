package com.prasjaychi.plantsense.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a local plant watering reminder schedule,
 * synchronized across devices with Firebase Firestore.
 */
@Entity(tableName = "plant_reminders")
data class PlantReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "plant_id")
    val plantId: Long = 0L,

    @ColumnInfo(name = "remote_plant_id")
    val remotePlantId: String = "",

    @ColumnInfo(name = "plant_name")
    val plantName: String,

    @ColumnInfo(name = "scientific_name")
    val scientificName: String,

    @ColumnInfo(name = "watering_schedule")
    val wateringSchedule: String,

    @ColumnInfo(name = "severity")
    val severity: String = "MODERATE",

    @ColumnInfo(name = "hour")
    val hour: Int = 9,

    @ColumnInfo(name = "minute")
    val minute: Int = 0,

    @ColumnInfo(name = "repeat_days")
    val repeatDays: Int = 1,

    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,

    @ColumnInfo(name = "last_triggered_ms")
    val lastTriggeredMs: Long = 0L,

    @ColumnInfo(name = "created_at_ms")
    val createdAtMs: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "remote_id")
    val remoteId: String = "",

    @ColumnInfo(name = "last_synced_ms")
    val lastSyncedMs: Long = 0L,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "LOCAL_ONLY",

    @ColumnInfo(name = "device_id")
    val deviceId: String = ""
)
