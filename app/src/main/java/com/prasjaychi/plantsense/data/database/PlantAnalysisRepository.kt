package com.prasjaychi.plantsense.data.database

import com.prasjaychi.plantsense.viewmodel.DiagnosisSeverity
import com.prasjaychi.plantsense.viewmodel.PlantDiagnosisReport
import com.prasjaychi.plantsense.viewmodel.PlantIdentification
import com.prasjaychi.plantsense.viewmodel.PrescribedCarePlan
import com.prasjaychi.plantsense.viewmodel.RootCauseDiagnosis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository pattern implementation abstracting Room database operations for plant analysis history.
 */
class PlantAnalysisRepository(
    private val dao: PlantAnalysisDao,
    private val syncManager: com.prasjaychi.plantsense.data.sync.PlantFirestoreSyncManager? = null
) {

    val allAnalyses: Flow<List<PlantAnalysisEntity>> = dao.getAllAnalyses()
    val favoriteAnalyses: Flow<List<PlantAnalysisEntity>> = dao.getFavoriteAnalyses()
    val historyCount: Flow<Int> = dao.getHistoryCount()

    fun searchAnalyses(query: String): Flow<List<PlantAnalysisEntity>> {
        return dao.searchAnalyses(query)
    }

    suspend fun getAnalysisById(id: Long): PlantAnalysisEntity? {
        return dao.getAnalysisById(id)
    }

    suspend fun saveReport(
        report: PlantDiagnosisReport,
        notes: String = "",
        isFavorite: Boolean = false
    ): Long {
        val entity = PlantAnalysisEntity.fromReport(report, notes, isFavorite).copy(
            syncStatus = "PENDING_SYNC"
        )
        // 1. Immediate prioritized Room write
        val insertedId = dao.insertAnalysis(entity)
        // 2. Non-blocking lazy sync enqueue
        syncManager?.enqueueLazySync()
        return insertedId
    }

    suspend fun saveAnalysis(entity: PlantAnalysisEntity): Long {
        val insertedId = dao.insertAnalysis(entity.copy(syncStatus = "PENDING_SYNC"))
        syncManager?.enqueueLazySync()
        return insertedId
    }

    suspend fun updateFavorite(id: Long, isFavorite: Boolean) {
        // 1. Immediate prioritized Room update
        dao.updateFavorite(id, isFavorite)
        val current = dao.getAnalysisById(id)
        if (current != null) {
            dao.updateSyncInfo(id, current.remoteId, "PENDING_UPDATE", current.lastSyncedMs)
        }
        // 2. Non-blocking lazy sync enqueue
        syncManager?.enqueueLazySync()
    }

    suspend fun updateLastWatered(id: Long, timestampMs: Long, formattedDate: String) {
        // 1. Immediate prioritized Room update
        dao.updateLastWatered(id, timestampMs, formattedDate)
        val current = dao.getAnalysisById(id)
        if (current != null) {
            dao.updateSyncInfo(id, current.remoteId, "PENDING_UPDATE", current.lastSyncedMs)
        }
        // 2. Non-blocking lazy sync enqueue
        syncManager?.enqueueLazySync()
    }

    suspend fun updateUserNotes(id: Long, notes: String) {
        // 1. Immediate prioritized Room update
        dao.updateUserNotes(id, notes)
        val current = dao.getAnalysisById(id)
        if (current != null) {
            dao.updateSyncInfo(id, current.remoteId, "PENDING_UPDATE", current.lastSyncedMs)
        }
        // 2. Non-blocking lazy sync enqueue
        syncManager?.enqueueLazySync()
    }

    suspend fun updateReminderStatus(
        id: Long,
        isEnabled: Boolean,
        nextWateringMs: Long,
        nextWateringFormatted: String
    ) {
        // 1. Update Room database
        dao.updateReminderStatus(id, isEnabled, nextWateringMs, nextWateringFormatted)
        val current = dao.getAnalysisById(id)
        if (current != null) {
            dao.updateSyncInfo(id, current.remoteId, "PENDING_UPDATE", current.lastSyncedMs)
        }
        // 2. Non-blocking lazy sync enqueue
        syncManager?.enqueueLazySync()
    }

    suspend fun deleteById(id: Long) {
        val target = dao.getAnalysisById(id)
        // 1. Immediate prioritized Room deletion
        dao.deleteAnalysisById(id)
        // 2. Enqueue cloud deletion to sync queue
        if (target != null && target.remoteId.isNotBlank()) {
            syncManager?.enqueueCloudDeletion(
                targetType = "PLANT",
                remoteId = target.remoteId,
                localId = id
            )
        }
    }

    suspend fun clearHistory() {
        dao.clearAll()
    }

    /**
     * Exports all local Room plant history entries to an RFC-4180 CSV string.
     */
    suspend fun exportToCsvString(): String {
        val allEntities = dao.getAllAnalysesList()
        return com.prasjaychi.plantsense.data.backup.PlantCsvBackupManager.entitiesToCsvString(allEntities)
    }

    /**
     * Exports all local Room records directly into a shareable cache File.
     */
    suspend fun exportToCacheFile(context: android.content.Context): com.prasjaychi.plantsense.data.backup.CsvExportResult {
        return try {
            val allEntities = dao.getAllAnalysesList()
            val file = com.prasjaychi.plantsense.data.backup.PlantCsvBackupManager.exportToCacheFile(context, allEntities)
            com.prasjaychi.plantsense.data.backup.CsvExportResult(
                success = true,
                count = allEntities.size,
                file = file,
                fileName = file.name
            )
        } catch (e: Exception) {
            com.prasjaychi.plantsense.data.backup.CsvExportResult(
                success = false,
                errorMessage = e.localizedMessage ?: "Failed to generate CSV backup file."
            )
        }
    }

    /**
     * Exports all local Room records into a designated URI (e.g. from SAF CreateDocument).
     */
    suspend fun exportToUri(context: android.content.Context, uri: android.net.Uri): com.prasjaychi.plantsense.data.backup.CsvExportResult {
        return try {
            val allEntities = dao.getAllAnalysesList()
            com.prasjaychi.plantsense.data.backup.PlantCsvBackupManager.exportToUri(context, uri, allEntities)
        } catch (e: Exception) {
            com.prasjaychi.plantsense.data.backup.CsvExportResult(
                success = false,
                errorMessage = e.localizedMessage ?: "Failed to export data to destination."
            )
        }
    }

    /**
     * Imports botanical records from a CSV file URI into the Room database.
     * Supports either OVERWRITE (replace full database) or MERGE (smart non-destructive merge).
     */
    suspend fun importFromUri(
        context: android.content.Context,
        uri: android.net.Uri,
        overwrite: Boolean = false
    ): com.prasjaychi.plantsense.data.backup.CsvImportResult {
        return try {
            val parsedEntities = com.prasjaychi.plantsense.data.backup.PlantCsvBackupManager.readEntitiesFromUri(context, uri)
            if (parsedEntities.isEmpty()) {
                return com.prasjaychi.plantsense.data.backup.CsvImportResult(
                    success = false,
                    errorMessage = "The selected CSV file contains no valid plant history records."
                )
            }
            importEntities(parsedEntities, overwrite)
        } catch (e: Exception) {
            com.prasjaychi.plantsense.data.backup.CsvImportResult(
                success = false,
                errorMessage = e.localizedMessage ?: "Failed to read or parse the selected CSV file."
            )
        }
    }

    /**
     * Inserts or merges parsed entities into the local Room database with optional cloud sync enqueue.
     */
    suspend fun importEntities(
        entities: List<PlantAnalysisEntity>,
        overwrite: Boolean = false
    ): com.prasjaychi.plantsense.data.backup.CsvImportResult {
        var inserted = 0
        var updated = 0
        var skipped = 0

        if (overwrite) {
            dao.clearAll()
            // Reset IDs to 0 so Room generates new primary keys or maintains consistency
            val cleanEntities = entities.map { it.copy(id = 0, syncStatus = "PENDING_SYNC") }
            dao.insertAll(cleanEntities)
            inserted = cleanEntities.size
        } else {
            val existing = dao.getAllAnalysesList()
            val existingKeySet = existing.map { "${it.commonName}_${it.timestampMs}" }.toSet()

            val toInsert = mutableListOf<PlantAnalysisEntity>()
            for (entity in entities) {
                val key = "${entity.commonName}_${entity.timestampMs}"
                if (existingKeySet.contains(key)) {
                    // Check if existing item exists by matching commonName and scientificName
                    val match = existing.find { it.commonName == entity.commonName && it.scientificName == entity.scientificName }
                    if (match != null) {
                        dao.updateAnalysis(entity.copy(id = match.id, syncStatus = "PENDING_UPDATE"))
                        updated++
                    } else {
                        skipped++
                    }
                } else {
                    toInsert.add(entity.copy(id = 0, syncStatus = "PENDING_SYNC"))
                    inserted++
                }
            }
            if (toInsert.isNotEmpty()) {
                dao.insertAll(toInsert)
            }
        }

        // Trigger background sync if needed
        syncManager?.enqueueLazySync()

        return com.prasjaychi.plantsense.data.backup.CsvImportResult(
            success = true,
            totalParsed = entities.size,
            insertedCount = inserted,
            updatedCount = updated,
            skippedCount = skipped
        )
    }

    /**
     * Seeds initial realistic botanical records if database is empty so users can immediately
     * view previous analysis results and verify data persistence.
     */
    suspend fun seedInitialDataIfEmpty() {
        val current = dao.getAllAnalyses().firstOrNull()
        if (current.isNullOrEmpty()) {
            val sampleEntities = createDefaultSeedRecords()
            dao.insertAll(sampleEntities)
        }
    }

    private fun createDefaultSeedRecords(): List<PlantAnalysisEntity> {
        val now = System.currentTimeMillis()
        val oneDayAgo = now - 86_400_000L
        val threeDaysAgo = now - (3 * 86_400_000L)

        val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())

        return listOf(
            PlantAnalysisEntity(
                timestampMs = now,
                formattedDate = dateFormat.format(Date(now)),
                scientificName = "Monstera deliciosa",
                commonName = "Swiss Cheese Plant",
                family = "Araceae",
                matchConfidence = 98.4f,
                nativeRegion = "Tropical rainforests of Southern Mexico & Panama",
                leafCharacteristics = "Broad cordate leaves with characteristic natural perforations (fenestrations)",
                primaryCause = "Substrate Compaction & Sub-optimal Drainage",
                rootCauseCategory = "Physiological Stress (Overwatering / Moisture Stagnation)",
                severity = DiagnosisSeverity.MODERATE.name,
                symptomsJoined = listOf(
                    "Early interveinal chlorosis (yellowing) on basal lower foliage",
                    "Marginal leaf tip necrosis due to moisture retention",
                    "Substrate moisture saturation exceeding 82% at root ball base",
                    "Reduced transpiration rate in lower aerial roots"
                ).joinToString(";;;"),
                pathogenStatus = "Negative for fungal blight, bacterial wilt, and spider mite webbing (99.2% confidence)",
                physiologicalImpact = "Restricted root oxygen absorption leading to localized chlorophyll degradation.",
                immediateIntervention = "Halt watering immediately for 6-8 days until moisture in the upper 2.5 inches of substrate drops below 25%.",
                wateringSchedule = "Every 10-14 days; allow the top half of soil to dry out between thorough waterings.",
                soilAndRepotting = "Aerate root zone; repot into an aroid mix containing 40% coarse perlite, 30% pine bark chips, and 30% coco coir.",
                lightingRecommendation = "Position in bright, indirect sunlight (approx. 12,000–18,000 lux). Avoid harsh midday sun.",
                humidityAndAtmosphere = "Maintain 55% - 70% relative humidity. Ambient temperature 18°C–28°C (65°F–82°F).",
                nutritionCadence = "Suspend liquid fertilizers for 3 weeks; resume balanced 20-20-20 at 25% strength when fresh shoot emerges.",
                recoveryTimeline = "Visible recovery and stabilization expected within 14–21 days.",
                healthScore = 76,
                isFavorite = true,
                userNotes = "Living room specimen near east window."
            ),
            PlantAnalysisEntity(
                timestampMs = oneDayAgo,
                formattedDate = dateFormat.format(Date(oneDayAgo)),
                scientificName = "Ficus lyrata",
                commonName = "Fiddle-Leaf Fig",
                family = "Moraceae",
                matchConfidence = 96.8f,
                nativeRegion = "Lowland tropical rainforests of Western Africa",
                leafCharacteristics = "Large lyre-shaped coriaceous (leathery) leaves with prominent veining",
                primaryCause = "Cellular Oedema & Inconsistent Hydration Cadence",
                rootCauseCategory = "Hydraulic Shock / Capillary Rupture",
                severity = DiagnosisSeverity.MILD.name,
                symptomsJoined = listOf(
                    "Subtle reddish-brown cellular pinpricks on newly unfurled upper leaves",
                    "Uneven turgor pressure across leaf margins",
                    "Normal root color with healthy white apical root tips"
                ).joinToString(";;;"),
                pathogenStatus = "Clean specimen; no rust fungus or scale insect activity detected",
                physiologicalImpact = "Excessive hydrostatic water pressure rupturing foliar cells during rapid moisture uptake after drought intervals.",
                immediateIntervention = "Normalize watering cycles to maintain even, lightly moist substrate rather than feast-or-famine cycles.",
                wateringSchedule = "Water thoroughly when top 2 inches feel dry (approx every 7-9 days in warm seasons).",
                soilAndRepotting = "Ensure drain holes are unobstructed; flush soil every 60 days to avoid salt crystallization.",
                lightingRecommendation = "Provide very bright filtered sun (15,000–22,000 lux). Rotate pot 90° weekly for balanced symmetry.",
                humidityAndAtmosphere = "Maintain 50%+ humidity; protect from cold AC drafts and radiator heat vents.",
                nutritionCadence = "Apply 3-1-2 NPK formulation at half strength monthly during active growing season.",
                recoveryTimeline = "New foliage will emerge clear of oedema marks within 30 days.",
                healthScore = 88,
                isFavorite = false,
                userNotes = "Purchased last month, placed in office reception."
            ),
            PlantAnalysisEntity(
                timestampMs = threeDaysAgo,
                formattedDate = dateFormat.format(Date(threeDaysAgo)),
                scientificName = "Sansevieria trifasciata",
                commonName = "Snake Plant / Mother-in-Law's Tongue",
                family = "Asparagaceae",
                matchConfidence = 99.1f,
                nativeRegion = "Tropical West Africa from Nigeria to the Congo",
                leafCharacteristics = "Erect rigid sword-like leaves with green banded variegation and yellow margins",
                primaryCause = "Optimal Health & Thriving Cellular Vitality",
                rootCauseCategory = "Ideal Physiological State",
                severity = DiagnosisSeverity.OPTIMAL.name,
                symptomsJoined = listOf(
                    "Firm, upright leaf turgidity with zero foliar wrinkling",
                    "Vibrant cross-banded chlorophyll pigment density",
                    "Dry, well-aerated cactus/succulent substrate with zero odor",
                    "Active new rhizome offset (pup) emerging at soil line"
                ).joinToString(";;;"),
                pathogenStatus = "Complete absence of pathogens, powdery mildew, or pest colonies",
                physiologicalImpact = "Exceptional CAM (Crassulacean Acid Metabolism) respiration and strong root anchorage.",
                immediateIntervention = "No intervention needed. Current environmental regime is ideal.",
                wateringSchedule = "Sparingly every 21-30 days; permit total substrate dry-down between waterings.",
                soilAndRepotting = "Keep in well-draining cactus/succulent gritty mix with coarse pumice and perlite.",
                lightingRecommendation = "Tolerates low light up to direct morning sun (3,000–20,000 lux).",
                humidityAndAtmosphere = "Adaptable to dry indoor humidity (30%–50%). Optimal temperature 16°C–32°C.",
                nutritionCadence = "Low feeding requirement: fertilize once in spring and once in summer with cactus fertilizer.",
                recoveryTimeline = "Specimen is fully stabilized and in prime growth phase.",
                healthScore = 98,
                isFavorite = true,
                userNotes = "Bedroom shelf plant. Very hardy specimen."
            )
        )
    }
}
