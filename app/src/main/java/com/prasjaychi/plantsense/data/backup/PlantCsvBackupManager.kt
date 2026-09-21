package com.prasjaychi.plantsense.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.prasjaychi.plantsense.data.database.PlantAnalysisEntity
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Result data holder for CSV export operations.
 */
data class CsvExportResult(
    val success: Boolean,
    val count: Int = 0,
    val file: File? = null,
    val uri: Uri? = null,
    val fileName: String = "",
    val errorMessage: String? = null
)

/**
 * Result data holder for CSV import operations.
 */
data class CsvImportResult(
    val success: Boolean,
    val totalParsed: Int = 0,
    val insertedCount: Int = 0,
    val updatedCount: Int = 0,
    val skippedCount: Int = 0,
    val errorMessage: String? = null
)

/**
 * High-reliability CSV Backup & Restore manager for PlantSense Room Database records.
 * Complies with RFC-4180 CSV standards, supporting multi-line quotes, escaping,
 * column-order-resilient header mapping, and flexible Merge/Restore strategies.
 */
object PlantCsvBackupManager {

    val CSV_HEADERS = listOf(
        "id",
        "timestamp_ms",
        "formatted_date",
        "scientific_name",
        "common_name",
        "family",
        "match_confidence",
        "native_region",
        "leaf_characteristics",
        "primary_cause",
        "root_cause_category",
        "severity",
        "symptoms_joined",
        "pathogen_status",
        "physiological_impact",
        "immediate_intervention",
        "watering_schedule",
        "soil_and_repotting",
        "lighting_recommendation",
        "humidity_and_atmosphere",
        "nutrition_cadence",
        "recovery_timeline",
        "health_score",
        "is_favorite",
        "user_notes",
        "last_watered_ms",
        "last_watered_formatted",
        "remote_id",
        "sync_status"
    )

    /**
     * Serializes a list of PlantAnalysisEntity objects into an RFC-4180 compliant CSV string.
     */
    fun entitiesToCsvString(entities: List<PlantAnalysisEntity>): String {
        val sb = StringBuilder()
        // Write header
        sb.append(CSV_HEADERS.joinToString(",")).append("\r\n")

        for (item in entities) {
            val row = listOf(
                item.id.toString(),
                item.timestampMs.toString(),
                escapeCsvCell(item.formattedDate),
                escapeCsvCell(item.scientificName),
                escapeCsvCell(item.commonName),
                escapeCsvCell(item.family),
                item.matchConfidence.toString(),
                escapeCsvCell(item.nativeRegion),
                escapeCsvCell(item.leafCharacteristics),
                escapeCsvCell(item.primaryCause),
                escapeCsvCell(item.rootCauseCategory),
                escapeCsvCell(item.severity),
                escapeCsvCell(item.symptomsJoined),
                escapeCsvCell(item.pathogenStatus),
                escapeCsvCell(item.physiologicalImpact),
                escapeCsvCell(item.immediateIntervention),
                escapeCsvCell(item.wateringSchedule),
                escapeCsvCell(item.soilAndRepotting),
                escapeCsvCell(item.lightingRecommendation),
                escapeCsvCell(item.humidityAndAtmosphere),
                escapeCsvCell(item.nutritionCadence),
                escapeCsvCell(item.recoveryTimeline),
                item.healthScore.toString(),
                item.isFavorite.toString(),
                escapeCsvCell(item.userNotes),
                item.lastWateredMs.toString(),
                escapeCsvCell(item.lastWateredFormatted),
                escapeCsvCell(item.remoteId),
                escapeCsvCell(item.syncStatus)
            )
            sb.append(row.joinToString(",")).append("\r\n")
        }
        return sb.toString()
    }

    /**
     * Parses an RFC-4180 compliant CSV string into a list of PlantAnalysisEntity objects.
     */
    fun parseCsvStringToEntities(csvContent: String): List<PlantAnalysisEntity> {
        val records = parseCsvRows(csvContent)
        if (records.isEmpty()) return emptyList()

        val headerRow = records[0].map { it.trim().lowercase(Locale.ROOT) }
        val headerMap = headerRow.mapIndexed { index, name -> name to index }.toMap()

        val entities = mutableListOf<PlantAnalysisEntity>()

        for (i in 1 until records.size) {
            val row = records[i]
            if (row.isEmpty() || (row.size == 1 && row[0].isBlank())) continue

            fun getVal(colName: String, defaultValue: String = ""): String {
                val index = headerMap[colName.lowercase(Locale.ROOT)] ?: return defaultValue
                return if (index < row.size) row[index] else defaultValue
            }

            try {
                val idVal = getVal("id", "0").toLongOrNull() ?: 0L
                val timestampVal = getVal("timestamp_ms", System.currentTimeMillis().toString()).toLongOrNull()
                    ?: System.currentTimeMillis()
                val formattedDateVal = getVal(
                    "formatted_date",
                    SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(timestampVal))
                )
                val scientificName = getVal("scientific_name", "Unknown Species")
                val commonName = getVal("common_name", "Plant Specimen")
                val family = getVal("family", "General")
                val matchConfidence = getVal("match_confidence", "90.0").toFloatOrNull() ?: 90.0f
                val nativeRegion = getVal("native_region", "Worldwide")
                val leafCharacteristics = getVal("leaf_characteristics", "Standard foliage")
                val primaryCause = getVal("primary_cause", "General Assessment")
                val rootCauseCategory = getVal("root_cause_category", "Optimal")
                val severity = getVal("severity", "OPTIMAL")
                val symptomsJoined = getVal("symptoms_joined", "")
                val pathogenStatus = getVal("pathogen_status", "Clear")
                val physiologicalImpact = getVal("physiological_impact", "Healthy")
                val immediateIntervention = getVal("immediate_intervention", "Maintain current regime")
                val wateringSchedule = getVal("watering_schedule", "Weekly")
                val soilAndRepotting = getVal("soil_and_repotting", "Standard potting mix")
                val lightingRecommendation = getVal("lighting_recommendation", "Bright indirect light")
                val humidityAndAtmosphere = getVal("humidity_and_atmosphere", "Moderate humidity (40-60%)")
                val nutritionCadence = getVal("nutrition_cadence", "Monthly feeding")
                val recoveryTimeline = getVal("recovery_timeline", "Stable")
                val healthScore = getVal("health_score", "85").toIntOrNull() ?: 85
                val isFavorite = getVal("is_favorite", "false").toBooleanStrictOrNull() ?: false
                val userNotes = getVal("user_notes", "")
                val lastWateredMs = getVal("last_watered_ms", "0").toLongOrNull() ?: 0L
                val lastWateredFormatted = getVal("last_watered_formatted", "")
                val remoteId = getVal("remote_id", "")
                val syncStatus = getVal("sync_status", "LOCAL_ONLY")

                entities.add(
                    PlantAnalysisEntity(
                        id = idVal,
                        timestampMs = timestampVal,
                        formattedDate = formattedDateVal,
                        scientificName = scientificName,
                        commonName = commonName,
                        family = family,
                        matchConfidence = matchConfidence,
                        nativeRegion = nativeRegion,
                        leafCharacteristics = leafCharacteristics,
                        primaryCause = primaryCause,
                        rootCauseCategory = rootCauseCategory,
                        severity = severity,
                        symptomsJoined = symptomsJoined,
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
                        remoteId = remoteId,
                        syncStatus = syncStatus
                    )
                )
            } catch (_: Exception) {
                // Ignore malformed individual rows
            }
        }

        return entities
    }

    /**
     * Exports the entities into a temporary shareable File in cacheDir.
     */
    fun exportToCacheFile(context: Context, entities: List<PlantAnalysisEntity>): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "PlantSense_Backup_$timeStamp.csv"
        val cacheFile = File(context.cacheDir, fileName)
        val csvContent = entitiesToCsvString(entities)
        cacheFile.writeText(csvContent, Charsets.UTF_8)
        return cacheFile
    }

    /**
     * Exports directly to a destination URI (e.g. from Storage Access Framework CreateDocument).
     */
    fun exportToUri(context: Context, uri: Uri, entities: List<PlantAnalysisEntity>): CsvExportResult {
        return try {
            val csvContent = entitiesToCsvString(entities)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }
            CsvExportResult(
                success = true,
                count = entities.size,
                uri = uri,
                fileName = "PlantSense_Backup.csv"
            )
        } catch (e: Exception) {
            CsvExportResult(
                success = false,
                errorMessage = e.localizedMessage ?: "Failed to write CSV to selected location."
            )
        }
    }

    /**
     * Imports entities from a content URI (e.g. from SAF OpenDocument / GetContent).
     */
    fun readEntitiesFromUri(context: Context, uri: Uri): List<PlantAnalysisEntity> {
        val stringBuilder = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line).append("\n")
                    line = reader.readLine()
                }
            }
        }
        return parseCsvStringToEntities(stringBuilder.toString())
    }

    /**
     * Creates an Android Share Intent for the generated CSV backup file.
     */
    fun createShareIntent(context: Context, file: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "PlantSense Health History Backup (${file.name})")
            putExtra(Intent.EXTRA_TEXT, "Exported PlantSense botanical records backup file attached.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Escapes standard CSV field following RFC 4180 rules.
     */
    private fun escapeCsvCell(value: String): String {
        val needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        return if (needsQuotes) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    /**
     * Robust RFC 4180 parser supporting quoted newlines and escaped quotation marks.
     */
    private fun parseCsvRows(csv: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < csv.length) {
            val c = csv[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < csv.length && csv[i + 1] == '"') {
                        // Escaped quote
                        currentField.append('"')
                        i++
                    } else {
                        // Closing quote
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> {
                        inQuotes = true
                    }
                    ',' -> {
                        currentRow.add(currentField.toString())
                        currentField.setLength(0)
                    }
                    '\r' -> {
                        if (i + 1 < csv.length && csv[i + 1] == '\n') {
                            i++
                        }
                        currentRow.add(currentField.toString())
                        currentField.setLength(0)
                        rows.add(ArrayList(currentRow))
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString())
                        currentField.setLength(0)
                        rows.add(ArrayList(currentRow))
                        currentRow.clear()
                    }
                    else -> {
                        currentField.append(c)
                    }
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            rows.add(currentRow)
        }

        return rows
    }
}
