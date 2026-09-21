package com.prasjaychi.plantsense.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Room Entity representing periodic plant health monitoring logs and diagnostic progression.
 */
@Entity(
    tableName = "plant_health_records",
    indices = [
        Index(value = ["plant_id"]),
        Index(value = ["plant_name"]),
        Index(value = ["timestamp_ms"])
    ]
)
data class PlantHealthRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "plant_id")
    val plantId: Long = 0L,

    @ColumnInfo(name = "plant_name")
    val plantName: String,

    @ColumnInfo(name = "scientific_name")
    val scientificName: String = "",

    @ColumnInfo(name = "timestamp_ms")
    val timestampMs: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "formatted_date")
    val formattedDate: String = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(timestampMs)),

    @ColumnInfo(name = "health_score")
    val healthScore: Int = 100, // 0 to 100

    @ColumnInfo(name = "vitality_status")
    val vitalityStatus: String = "HEALTHY", // THRIVING, HEALTHY, NEEDS_ATTENTION, CRITICAL

    @ColumnInfo(name = "soil_moisture_level")
    val soilMoistureLevel: Float = 0.5f, // 0.0 (Dry) to 1.0 (Saturated)

    @ColumnInfo(name = "soil_moisture_status")
    val soilMoistureStatus: String = "OPTIMAL", // DRY, OPTIMAL, WET

    @ColumnInfo(name = "leaf_condition")
    val leafCondition: String = "Vibrant", // Vibrant, Yellowing, Browning, Wilting, Spots

    @ColumnInfo(name = "pest_presence")
    val pestPresence: Boolean = false,

    @ColumnInfo(name = "pest_details")
    val pestDetails: String = "",

    @ColumnInfo(name = "sunlight_exposure_hours")
    val sunlightExposureHours: Float = 6.0f,

    @ColumnInfo(name = "ambient_temperature_c")
    val ambientTemperatureC: Float = 22.0f,

    @ColumnInfo(name = "ambient_humidity_percent")
    val ambientHumidityPercent: Float = 50.0f,

    @ColumnInfo(name = "fertilizer_applied")
    val fertilizerApplied: Boolean = false,

    @ColumnInfo(name = "pruned_or_cleaned")
    val prunedOrCleaned: Boolean = false,

    @ColumnInfo(name = "care_notes")
    val careNotes: String = "",

    @ColumnInfo(name = "photo_uri")
    val photoUri: String = ""
)
