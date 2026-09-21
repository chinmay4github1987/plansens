package com.prasjaychi.plantsense.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.prasjaychi.plantsense.viewmodel.DiagnosisSeverity
import com.prasjaychi.plantsense.viewmodel.PlantDiagnosisReport
import com.prasjaychi.plantsense.viewmodel.PlantIdentification
import com.prasjaychi.plantsense.viewmodel.PrescribedCarePlan
import com.prasjaychi.plantsense.viewmodel.RootCauseDiagnosis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Room Entity representing a stored plant identification and diagnosis analysis entry.
 */
@Entity(tableName = "plant_analysis_history")
data class PlantAnalysisEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timestamp_ms")
    val timestampMs: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "formatted_date")
    val formattedDate: String = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(timestampMs)),

    // Identification details
    @ColumnInfo(name = "scientific_name")
    val scientificName: String,

    @ColumnInfo(name = "common_name")
    val commonName: String,

    @ColumnInfo(name = "family")
    val family: String,

    @ColumnInfo(name = "match_confidence")
    val matchConfidence: Float,

    @ColumnInfo(name = "native_region")
    val nativeRegion: String,

    @ColumnInfo(name = "leaf_characteristics")
    val leafCharacteristics: String,

    // Root cause and diagnosis
    @ColumnInfo(name = "primary_cause")
    val primaryCause: String,

    @ColumnInfo(name = "root_cause_category")
    val rootCauseCategory: String,

    @ColumnInfo(name = "severity")
    val severity: String, // OPTIMAL, MILD, MODERATE, CRITICAL

    @ColumnInfo(name = "symptoms_joined")
    val symptomsJoined: String, // Delimited symptoms list

    @ColumnInfo(name = "pathogen_status")
    val pathogenStatus: String,

    @ColumnInfo(name = "physiological_impact")
    val physiologicalImpact: String,

    // Prescribed care plan
    @ColumnInfo(name = "immediate_intervention")
    val immediateIntervention: String,

    @ColumnInfo(name = "watering_schedule")
    val wateringSchedule: String,

    @ColumnInfo(name = "soil_and_repotting")
    val soilAndRepotting: String,

    @ColumnInfo(name = "lighting_recommendation")
    val lightingRecommendation: String,

    @ColumnInfo(name = "humidity_and_atmosphere")
    val humidityAndAtmosphere: String,

    @ColumnInfo(name = "nutrition_cadence")
    val nutritionCadence: String,

    @ColumnInfo(name = "recovery_timeline")
    val recoveryTimeline: String,

    // Health and user metadata
    @ColumnInfo(name = "health_score")
    val healthScore: Int,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "user_notes")
    val userNotes: String = "",

    // Manual plant care watering logs
    @ColumnInfo(name = "last_watered_ms")
    val lastWateredMs: Long = 0L,

    @ColumnInfo(name = "last_watered_formatted")
    val lastWateredFormatted: String = "",

    // Firestore cross-device sync metadata
    @ColumnInfo(name = "remote_id")
    val remoteId: String = "",

    @ColumnInfo(name = "last_synced_ms")
    val lastSyncedMs: Long = 0L,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "LOCAL_ONLY",

    // WorkManager watering reminder notification state
    @ColumnInfo(name = "is_reminder_enabled", defaultValue = "0")
    val isReminderEnabled: Boolean = false,

    @ColumnInfo(name = "next_watering_ms", defaultValue = "0")
    val nextWateringMs: Long = 0L,

    @ColumnInfo(name = "next_watering_formatted", defaultValue = "''")
    val nextWateringFormatted: String = ""
) {
    /**
     * Converts this database entity back into the active UI state PlantDiagnosisReport model.
     */
    fun toReport(): PlantDiagnosisReport {
        val severityEnum = try {
            DiagnosisSeverity.valueOf(severity)
        } catch (_: Exception) {
            DiagnosisSeverity.MODERATE
        }

        val symptomsList = if (symptomsJoined.isNotBlank()) {
            symptomsJoined.split(";;;").filter { it.isNotBlank() }
        } else {
            emptyList()
        }

        return PlantDiagnosisReport(
            identification = PlantIdentification(
                scientificName = scientificName,
                commonName = commonName,
                family = family,
                matchConfidence = matchConfidence,
                nativeRegion = nativeRegion,
                leafCharacteristics = leafCharacteristics
            ),
            rootCause = RootCauseDiagnosis(
                primaryCause = primaryCause,
                rootCauseCategory = rootCauseCategory,
                severity = severityEnum,
                symptomsDetected = symptomsList,
                pathogenStatus = pathogenStatus,
                physiologicalImpact = physiologicalImpact
            ),
            carePlan = PrescribedCarePlan(
                immediateIntervention = immediateIntervention,
                wateringSchedule = wateringSchedule,
                soilAndRepotting = soilAndRepotting,
                lightingRecommendation = lightingRecommendation,
                humidityAndAtmosphere = humidityAndAtmosphere,
                nutritionCadence = nutritionCadence,
                recoveryTimeline = recoveryTimeline,
                lastWateredMs = lastWateredMs,
                lastWateredFormatted = lastWateredFormatted
            ),
            healthScore = healthScore,
            timestamp = formattedDate,
            isSavedToWorkspace = true
        )
    }

    companion object {
        /**
         * Creates an entity from a PlantDiagnosisReport.
         */
        fun fromReport(
            report: PlantDiagnosisReport,
            notes: String = "",
            isFavorite: Boolean = false
        ): PlantAnalysisEntity {
            return PlantAnalysisEntity(
                scientificName = report.identification.scientificName,
                commonName = report.identification.commonName,
                family = report.identification.family,
                matchConfidence = report.identification.matchConfidence,
                nativeRegion = report.identification.nativeRegion,
                leafCharacteristics = report.identification.leafCharacteristics,
                primaryCause = report.rootCause.primaryCause,
                rootCauseCategory = report.rootCause.rootCauseCategory,
                severity = report.rootCause.severity.name,
                symptomsJoined = report.rootCause.symptomsDetected.joinToString(";;;"),
                pathogenStatus = report.rootCause.pathogenStatus,
                physiologicalImpact = report.rootCause.physiologicalImpact,
                immediateIntervention = report.carePlan.immediateIntervention,
                wateringSchedule = report.carePlan.wateringSchedule,
                soilAndRepotting = report.carePlan.soilAndRepotting,
                lightingRecommendation = report.carePlan.lightingRecommendation,
                humidityAndAtmosphere = report.carePlan.humidityAndAtmosphere,
                nutritionCadence = report.carePlan.nutritionCadence,
                recoveryTimeline = report.carePlan.recoveryTimeline,
                healthScore = report.healthScore,
                isFavorite = isFavorite,
                userNotes = notes,
                lastWateredMs = report.carePlan.lastWateredMs,
                lastWateredFormatted = report.carePlan.lastWateredFormatted
            )
        }
    }
}
