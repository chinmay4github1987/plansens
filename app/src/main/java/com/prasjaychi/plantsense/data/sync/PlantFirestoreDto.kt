package com.prasjaychi.plantsense.data.sync

import com.prasjaychi.plantsense.data.database.PlantAnalysisEntity
import com.prasjaychi.plantsense.data.database.PlantReminderEntity
import java.util.UUID

/**
 * Firestore document schema for cross-device synchronized plant diagnosis reports.
 */
data class PlantFirestoreDocument(
    val id: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val timestampMs: Long = 0L,
    val formattedDate: String = "",
    val scientificName: String = "",
    val commonName: String = "",
    val family: String = "",
    val matchConfidence: Double = 0.0,
    val nativeRegion: String = "",
    val leafCharacteristics: String = "",
    val primaryCause: String = "",
    val rootCauseCategory: String = "",
    val severity: String = "MODERATE",
    val symptoms: List<String> = emptyList(),
    val pathogenStatus: String = "",
    val physiologicalImpact: String = "",
    val immediateIntervention: String = "",
    val wateringSchedule: String = "",
    val soilAndRepotting: String = "",
    val lightingRecommendation: String = "",
    val humidityAndAtmosphere: String = "",
    val nutritionCadence: String = "",
    val recoveryTimeline: String = "",
    val healthScore: Int = 85,
    val isFavorite: Boolean = false,
    val userNotes: String = "",
    val lastWateredMs: Long = 0L,
    val lastWateredFormatted: String = "",
    val lastUpdatedMs: Long = System.currentTimeMillis()
) {
    /**
     * Converts a Firestore document to a local Room entity.
     */
    fun toEntity(localId: Long = 0): PlantAnalysisEntity {
        return PlantAnalysisEntity(
            id = localId,
            timestampMs = if (timestampMs > 0) timestampMs else System.currentTimeMillis(),
            formattedDate = formattedDate.ifBlank { "Synced from Cloud" },
            scientificName = scientificName,
            commonName = commonName,
            family = family,
            matchConfidence = matchConfidence.toFloat(),
            nativeRegion = nativeRegion,
            leafCharacteristics = leafCharacteristics,
            primaryCause = primaryCause,
            rootCauseCategory = rootCauseCategory,
            severity = severity,
            symptomsJoined = symptoms.joinToString(";;;"),
            pathogenStatus = pathogenStatus,
            physiologicalImpact = physiologicalImpact,
            immediateIntervention = immediateIntervention,
            wateringSchedule = wateringSchedule,
            soilAndRepotting = soilAndRepotting,
            lightingRecommendation = lightingRecommendation,
            humidityAndAtmosphere = humidityAndAtmosphere,
            nutritionCadence = nutritionCadence,
            recoveryTimeline = recoveryTimeline,
            healthScore = healthScore,
            isFavorite = isFavorite,
            userNotes = userNotes,
            lastWateredMs = lastWateredMs,
            lastWateredFormatted = lastWateredFormatted,
            remoteId = id,
            lastSyncedMs = System.currentTimeMillis(),
            syncStatus = "SYNCED"
        )
    }

    /**
     * Converts to Firestore-compatible Map for serialization.
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "deviceId" to deviceId,
            "deviceName" to deviceName,
            "timestampMs" to timestampMs,
            "formattedDate" to formattedDate,
            "scientificName" to scientificName,
            "commonName" to commonName,
            "family" to family,
            "matchConfidence" to matchConfidence,
            "nativeRegion" to nativeRegion,
            "leafCharacteristics" to leafCharacteristics,
            "primaryCause" to primaryCause,
            "rootCauseCategory" to rootCauseCategory,
            "severity" to severity,
            "symptoms" to symptoms,
            "pathogenStatus" to pathogenStatus,
            "physiologicalImpact" to physiologicalImpact,
            "immediateIntervention" to immediateIntervention,
            "wateringSchedule" to wateringSchedule,
            "soilAndRepotting" to soilAndRepotting,
            "lightingRecommendation" to lightingRecommendation,
            "humidityAndAtmosphere" to humidityAndAtmosphere,
            "nutritionCadence" to nutritionCadence,
            "recoveryTimeline" to recoveryTimeline,
            "healthScore" to healthScore,
            "isFavorite" to isFavorite,
            "userNotes" to userNotes,
            "lastWateredMs" to lastWateredMs,
            "lastWateredFormatted" to lastWateredFormatted,
            "lastUpdatedMs" to lastUpdatedMs
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): PlantFirestoreDocument {
            return PlantFirestoreDocument(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                deviceId = map["deviceId"] as? String ?: "",
                deviceName = map["deviceName"] as? String ?: "",
                timestampMs = (map["timestampMs"] as? Number)?.toLong() ?: 0L,
                formattedDate = map["formattedDate"] as? String ?: "",
                scientificName = map["scientificName"] as? String ?: "",
                commonName = map["commonName"] as? String ?: "",
                family = map["family"] as? String ?: "",
                matchConfidence = (map["matchConfidence"] as? Number)?.toDouble() ?: 0.0,
                nativeRegion = map["nativeRegion"] as? String ?: "",
                leafCharacteristics = map["leafCharacteristics"] as? String ?: "",
                primaryCause = map["primaryCause"] as? String ?: "",
                rootCauseCategory = map["rootCauseCategory"] as? String ?: "",
                severity = map["severity"] as? String ?: "MODERATE",
                symptoms = @Suppress("UNCHECKED_CAST") (map["symptoms"] as? List<String>) ?: emptyList(),
                pathogenStatus = map["pathogenStatus"] as? String ?: "",
                physiologicalImpact = map["physiologicalImpact"] as? String ?: "",
                immediateIntervention = map["immediateIntervention"] as? String ?: "",
                wateringSchedule = map["wateringSchedule"] as? String ?: "",
                soilAndRepotting = map["soilAndRepotting"] as? String ?: "",
                lightingRecommendation = map["lightingRecommendation"] as? String ?: "",
                humidityAndAtmosphere = map["humidityAndAtmosphere"] as? String ?: "",
                nutritionCadence = map["nutritionCadence"] as? String ?: "",
                recoveryTimeline = map["recoveryTimeline"] as? String ?: "",
                healthScore = (map["healthScore"] as? Number)?.toInt() ?: 85,
                isFavorite = map["isFavorite"] as? Boolean ?: false,
                userNotes = map["userNotes"] as? String ?: "",
                lastWateredMs = (map["lastWateredMs"] as? Number)?.toLong() ?: 0L,
                lastWateredFormatted = map["lastWateredFormatted"] as? String ?: "",
                lastUpdatedMs = (map["lastUpdatedMs"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }

        fun fromEntity(
            entity: PlantAnalysisEntity,
            deviceId: String,
            deviceName: String
        ): PlantFirestoreDocument {
            val remoteDocId = if (entity.remoteId.isNotBlank()) entity.remoteId else "plant_${entity.commonName.lowercase().replace(" ", "_")}_${entity.id}"
            val symptoms = if (entity.symptomsJoined.isNotBlank()) {
                entity.symptomsJoined.split(";;;").filter { it.isNotBlank() }
            } else emptyList()

            return PlantFirestoreDocument(
                id = remoteDocId,
                deviceId = deviceId,
                deviceName = deviceName,
                timestampMs = entity.timestampMs,
                formattedDate = entity.formattedDate,
                scientificName = entity.scientificName,
                commonName = entity.commonName,
                family = entity.family,
                matchConfidence = entity.matchConfidence.toDouble(),
                nativeRegion = entity.nativeRegion,
                leafCharacteristics = entity.leafCharacteristics,
                primaryCause = entity.primaryCause,
                rootCauseCategory = entity.rootCauseCategory,
                severity = entity.severity,
                symptoms = symptoms,
                pathogenStatus = entity.pathogenStatus,
                physiologicalImpact = entity.physiologicalImpact,
                immediateIntervention = entity.immediateIntervention,
                wateringSchedule = entity.wateringSchedule,
                soilAndRepotting = entity.soilAndRepotting,
                lightingRecommendation = entity.lightingRecommendation,
                humidityAndAtmosphere = entity.humidityAndAtmosphere,
                nutritionCadence = entity.nutritionCadence,
                recoveryTimeline = entity.recoveryTimeline,
                healthScore = entity.healthScore,
                isFavorite = entity.isFavorite,
                userNotes = entity.userNotes,
                lastWateredMs = entity.lastWateredMs,
                lastWateredFormatted = entity.lastWateredFormatted,
                lastUpdatedMs = System.currentTimeMillis()
            )
        }
    }
}

/**
 * Firestore document schema for cross-device synchronized plant watering reminders.
 */
data class PlantReminderFirestoreDocument(
    val id: String = "",
    val plantRemoteId: String = "",
    val plantName: String = "",
    val scientificName: String = "",
    val wateringSchedule: String = "",
    val severity: String = "MODERATE",
    val hour: Int = 9,
    val minute: Int = 0,
    val repeatDays: Int = 1,
    val isEnabled: Boolean = true,
    val deviceId: String = "",
    val deviceName: String = "",
    val lastTriggeredMs: Long = 0L,
    val createdAtMs: Long = System.currentTimeMillis(),
    val lastUpdatedMs: Long = System.currentTimeMillis()
) {
    fun toEntity(localId: Long = 0, localPlantId: Long = 0): PlantReminderEntity {
        return PlantReminderEntity(
            id = localId,
            plantId = localPlantId,
            remotePlantId = plantRemoteId,
            plantName = plantName,
            scientificName = scientificName,
            wateringSchedule = wateringSchedule,
            severity = severity,
            hour = hour,
            minute = minute,
            repeatDays = repeatDays,
            isEnabled = isEnabled,
            lastTriggeredMs = lastTriggeredMs,
            createdAtMs = createdAtMs,
            remoteId = id,
            lastSyncedMs = System.currentTimeMillis(),
            syncStatus = "SYNCED",
            deviceId = deviceId
        )
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "plantRemoteId" to plantRemoteId,
            "plantName" to plantName,
            "scientificName" to scientificName,
            "wateringSchedule" to wateringSchedule,
            "severity" to severity,
            "hour" to hour,
            "minute" to minute,
            "repeatDays" to repeatDays,
            "isEnabled" to isEnabled,
            "deviceId" to deviceId,
            "deviceName" to deviceName,
            "lastTriggeredMs" to lastTriggeredMs,
            "createdAtMs" to createdAtMs,
            "lastUpdatedMs" to lastUpdatedMs
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): PlantReminderFirestoreDocument {
            return PlantReminderFirestoreDocument(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                plantRemoteId = map["plantRemoteId"] as? String ?: "",
                plantName = map["plantName"] as? String ?: "",
                scientificName = map["scientificName"] as? String ?: "",
                wateringSchedule = map["wateringSchedule"] as? String ?: "",
                severity = map["severity"] as? String ?: "MODERATE",
                hour = (map["hour"] as? Number)?.toInt() ?: 9,
                minute = (map["minute"] as? Number)?.toInt() ?: 0,
                repeatDays = (map["repeatDays"] as? Number)?.toInt() ?: 1,
                isEnabled = map["isEnabled"] as? Boolean ?: true,
                deviceId = map["deviceId"] as? String ?: "",
                deviceName = map["deviceName"] as? String ?: "",
                lastTriggeredMs = (map["lastTriggeredMs"] as? Number)?.toLong() ?: 0L,
                createdAtMs = (map["createdAtMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                lastUpdatedMs = (map["lastUpdatedMs"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }

        fun fromEntity(
            entity: PlantReminderEntity,
            deviceId: String,
            deviceName: String
        ): PlantReminderFirestoreDocument {
            val remoteDocId = if (entity.remoteId.isNotBlank()) entity.remoteId else "rem_${entity.plantName.lowercase().replace(" ", "_")}_${entity.id}"
            return PlantReminderFirestoreDocument(
                id = remoteDocId,
                plantRemoteId = entity.remotePlantId,
                plantName = entity.plantName,
                scientificName = entity.scientificName,
                wateringSchedule = entity.wateringSchedule,
                severity = entity.severity,
                hour = entity.hour,
                minute = entity.minute,
                repeatDays = entity.repeatDays,
                isEnabled = entity.isEnabled,
                deviceId = deviceId,
                deviceName = deviceName,
                lastTriggeredMs = entity.lastTriggeredMs,
                createdAtMs = entity.createdAtMs,
                lastUpdatedMs = System.currentTimeMillis()
            )
        }
    }
}
