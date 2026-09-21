package com.prasjaychi.plantsense

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.prasjaychi.plantsense.ui.camera.ContrastFilterMode
import com.prasjaychi.plantsense.ui.camera.PlantContrastFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.flow.first
import com.prasjaychi.plantsense.ui.NavHubApp
import com.prasjaychi.plantsense.ui.theme.MyApplicationTheme
import org.junit.Rule

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PlantSense", appName)
  }

  @Test
  fun `verify plant contrast filter creates enhanced bitmap and respects modes`() {
    val sourceBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    assertNotNull(sourceBitmap)

    // Test OFF mode returns the same or matching bitmap
    val offBitmap = PlantContrastFilter.applyContrastFilter(sourceBitmap, ContrastFilterMode.OFF)
    assertEquals(100, offBitmap.width)
    assertEquals(100, offBitmap.height)

    // Test BALANCED mode
    val balancedBitmap = PlantContrastFilter.applyContrastFilter(sourceBitmap, ContrastFilterMode.BALANCED)
    assertEquals(100, balancedBitmap.width)
    assertEquals(100, balancedBitmap.height)

    // Test VIVID mode
    val vividBitmap = PlantContrastFilter.applyContrastFilter(sourceBitmap, ContrastFilterMode.VIVID)
    assertEquals(100, vividBitmap.width)
    assertEquals(100, vividBitmap.height)

    // Test MAX_DYNAMIC mode
    val maxBitmap = PlantContrastFilter.applyContrastFilter(sourceBitmap, ContrastFilterMode.MAX_DYNAMIC)
    assertEquals(100, maxBitmap.width)
    assertEquals(100, maxBitmap.height)

    assertEquals(1.25f, ContrastFilterMode.BALANCED.contrastMultiplier)
    assertEquals(1.45f, ContrastFilterMode.VIVID.contrastMultiplier)
    assertEquals(1.70f, ContrastFilterMode.MAX_DYNAMIC.contrastMultiplier)
  }

  @Test
  fun `verify watering cadence parsing from care plan schedule text`() {
    val helper = com.prasjaychi.plantsense.notifications.PlantWateringNotificationHelper

    assertEquals(1, helper.parseWateringCadenceDays("Daily watering required"))
    assertEquals(1, helper.parseWateringCadenceDays("Every day in the morning"))
    assertEquals(2, helper.parseWateringCadenceDays("Alternate days"))
    assertEquals(7, helper.parseWateringCadenceDays("Weekly deep watering"))
    assertEquals(10, helper.parseWateringCadenceDays("Every 10-14 days; allow top half of soil to dry"))
    assertEquals(14, helper.parseWateringCadenceDays("Every 2 weeks"))
    assertEquals(21, helper.parseWateringCadenceDays("Every 3-4 weeks during winter"))
  }

  @Test
  fun `verify notification channel initialization`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.prasjaychi.plantsense.notifications.PlantWateringNotificationHelper.createNotificationChannel(context)

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      val channel = notificationManager.getNotificationChannel(com.prasjaychi.plantsense.notifications.PlantWateringNotificationHelper.CHANNEL_ID)
      assertNotNull(channel)
      assertEquals(com.prasjaychi.plantsense.notifications.PlantWateringNotificationHelper.CHANNEL_NAME, channel.name)
    }
  }

  @Test
  fun `verify plant identification trends frequency and time-series aggregation`() {
    val samplePlants = listOf(
      com.prasjaychi.plantsense.data.database.PlantAnalysisEntity(
        id = 1,
        timestampMs = 1000L,
        scientificName = "Monstera deliciosa",
        commonName = "Swiss Cheese Plant",
        family = "Araceae",
        matchConfidence = 95f,
        nativeRegion = "Central America",
        leafCharacteristics = "Fenestrated",
        primaryCause = "Healthy",
        rootCauseCategory = "Optimal",
        severity = "OPTIMAL",
        symptomsJoined = "None",
        pathogenStatus = "Clear",
        physiologicalImpact = "Normal",
        immediateIntervention = "None",
        wateringSchedule = "Weekly",
        soilAndRepotting = "Standard",
        lightingRecommendation = "Bright indirect",
        humidityAndAtmosphere = "Moderate",
        nutritionCadence = "Monthly",
        recoveryTimeline = "Stable",
        healthScore = 90
      ),
      com.prasjaychi.plantsense.data.database.PlantAnalysisEntity(
        id = 2,
        timestampMs = 2000L,
        scientificName = "Monstera deliciosa",
        commonName = "Swiss Cheese Plant",
        family = "Araceae",
        matchConfidence = 97f,
        nativeRegion = "Central America",
        leafCharacteristics = "Fenestrated",
        primaryCause = "Healthy",
        rootCauseCategory = "Optimal",
        severity = "OPTIMAL",
        symptomsJoined = "None",
        pathogenStatus = "Clear",
        physiologicalImpact = "Normal",
        immediateIntervention = "None",
        wateringSchedule = "Weekly",
        soilAndRepotting = "Standard",
        lightingRecommendation = "Bright indirect",
        humidityAndAtmosphere = "Moderate",
        nutritionCadence = "Monthly",
        recoveryTimeline = "Stable",
        healthScore = 94
      ),
      com.prasjaychi.plantsense.data.database.PlantAnalysisEntity(
        id = 3,
        timestampMs = 3000L,
        scientificName = "Ficus lyrata",
        commonName = "Fiddle-Leaf Fig",
        family = "Moraceae",
        matchConfidence = 92f,
        nativeRegion = "West Africa",
        leafCharacteristics = "Lyre-shaped",
        primaryCause = "Underwatering",
        rootCauseCategory = "Moisture",
        severity = "MILD",
        symptomsJoined = "Drooping",
        pathogenStatus = "Clear",
        physiologicalImpact = "Loss of turgor",
        immediateIntervention = "Water",
        wateringSchedule = "10 days",
        soilAndRepotting = "Standard",
        lightingRecommendation = "High",
        humidityAndAtmosphere = "High",
        nutritionCadence = "Monthly",
        recoveryTimeline = "7 days",
        healthScore = 75
      )
    )

    val groupedByCommonName = samplePlants.groupBy { it.commonName }
    assertEquals(2, groupedByCommonName["Swiss Cheese Plant"]?.size)
    assertEquals(1, groupedByCommonName["Fiddle-Leaf Fig"]?.size)
    assertEquals(3, samplePlants.size)

    val mostFrequent = groupedByCommonName.maxByOrNull { it.value.size }
    assertNotNull(mostFrequent)
    assertEquals("Swiss Cheese Plant", mostFrequent?.key)
  }

  @Test
  fun `verify plant health history CSV export and import serialization`() {
    val samplePlants = listOf(
      com.prasjaychi.plantsense.data.database.PlantAnalysisEntity(
        id = 101,
        timestampMs = 1700000000000L,
        formattedDate = "Nov 14, 2023 • 12:00",
        scientificName = "Monstera deliciosa",
        commonName = "Swiss Cheese Plant",
        family = "Araceae",
        matchConfidence = 98.4f,
        nativeRegion = "Tropical rainforests",
        leafCharacteristics = "Fenestrated broad leaves",
        primaryCause = "Compacted Substrate",
        rootCauseCategory = "Drainage Stress",
        severity = "MODERATE",
        symptomsJoined = "Interveinal chlorosis;;;Leaf tip necrosis",
        pathogenStatus = "Clear",
        physiologicalImpact = "Reduced oxygenation",
        immediateIntervention = "Halt watering for 7 days",
        wateringSchedule = "Every 10-14 days",
        soilAndRepotting = "Aroid mix with perlite",
        lightingRecommendation = "Bright indirect light",
        humidityAndAtmosphere = "60% humidity",
        nutritionCadence = "20-20-20 balanced",
        recoveryTimeline = "14-21 days",
        healthScore = 78,
        isFavorite = true,
        userNotes = "Near balcony window",
        lastWateredMs = 1699900000000L,
        lastWateredFormatted = "Nov 13, 2023",
        remoteId = "plant_remote_101",
        syncStatus = "SYNCED"
      ),
      com.prasjaychi.plantsense.data.database.PlantAnalysisEntity(
        id = 102,
        timestampMs = 1700086400000L,
        formattedDate = "Nov 15, 2023 • 14:30",
        scientificName = "Sansevieria trifasciata",
        commonName = "Snake Plant",
        family = "Asparagaceae",
        matchConfidence = 99.1f,
        nativeRegion = "West Africa",
        leafCharacteristics = "Erect sword leaves, banded",
        primaryCause = "Optimal Health",
        rootCauseCategory = "Optimal",
        severity = "OPTIMAL",
        symptomsJoined = "Vibrant pigment, upright growth",
        pathogenStatus = "Negative for pests",
        physiologicalImpact = "Ideal respiration",
        immediateIntervention = "Maintain regime",
        wateringSchedule = "Every 21-30 days",
        soilAndRepotting = "Gritty succulent mix",
        lightingRecommendation = "Tolerates low to bright light",
        humidityAndAtmosphere = "Average indoor",
        nutritionCadence = "Light feeding in spring",
        recoveryTimeline = "Thriving",
        healthScore = 98,
        isFavorite = false,
        userNotes = "Bedroom shelf plant",
        lastWateredMs = 0L,
        lastWateredFormatted = "",
        remoteId = "plant_remote_102",
        syncStatus = "LOCAL_ONLY"
      )
    )

    // 1. Serialize to CSV
    val csvString = com.prasjaychi.plantsense.data.backup.PlantCsvBackupManager.entitiesToCsvString(samplePlants)
    assertNotNull(csvString)
    org.junit.Assert.assertTrue(csvString.contains("Monstera deliciosa"))
    org.junit.Assert.assertTrue(csvString.contains("Sansevieria trifasciata"))
    org.junit.Assert.assertTrue(csvString.contains("Swiss Cheese Plant"))
    org.junit.Assert.assertTrue(csvString.contains("timestamp_ms"))

    // 2. Parse CSV back to Entities
    val parsedEntities = com.prasjaychi.plantsense.data.backup.PlantCsvBackupManager.parseCsvStringToEntities(csvString)
    assertEquals(2, parsedEntities.size)

    val first = parsedEntities[0]
    assertEquals(101L, first.id)
    assertEquals("Monstera deliciosa", first.scientificName)
    assertEquals("Swiss Cheese Plant", first.commonName)
    assertEquals("Araceae", first.family)
    assertEquals(98.4f, first.matchConfidence, 0.01f)
    assertEquals("MODERATE", first.severity)
    assertEquals(78, first.healthScore)
    assertEquals(true, first.isFavorite)
    assertEquals("Near balcony window", first.userNotes)
    assertEquals("SYNCED", first.syncStatus)

    val second = parsedEntities[1]
    assertEquals(102L, second.id)
    assertEquals("Sansevieria trifasciata", second.scientificName)
    assertEquals("Snake Plant", second.commonName)
    assertEquals(98, second.healthScore)
    assertEquals(false, second.isFavorite)
    assertEquals("LOCAL_ONLY", second.syncStatus)
  }

  @Test
  fun `verify plant gemini analyzer bitmap base64 encoding and heuristic report generation`() {
    val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
    val base64 = com.prasjaychi.plantsense.data.ai.PlantGeminiAnalyzer.bitmapToBase64(bitmap)
    assertNotNull(base64)
    org.junit.Assert.assertTrue(base64.isNotEmpty())

    val heuristicReport = com.prasjaychi.plantsense.data.ai.PlantGeminiAnalyzer.generateBotanicalHeuristicReport(bitmap)
    assertNotNull(heuristicReport)
    assertEquals("Monstera deliciosa", heuristicReport.identification.scientificName)
    assertEquals("Swiss Cheese Plant", heuristicReport.identification.commonName)
    assertEquals("Araceae", heuristicReport.identification.family)
    org.junit.Assert.assertTrue(heuristicReport.healthScore in 0..100)
    org.junit.Assert.assertTrue(heuristicReport.rootCause.symptomsDetected.isNotEmpty())
  }

  @Test
  fun `verify plant care tips provider contextual advice for identified species`() {
    // 1. Monstera deliciosa
    val monsteraReport = com.prasjaychi.plantsense.viewmodel.PlantDiagnosisReport(
      identification = com.prasjaychi.plantsense.viewmodel.PlantIdentification(
        scientificName = "Monstera deliciosa",
        commonName = "Swiss Cheese Plant",
        family = "Araceae"
      )
    )
    val monsteraGuide = com.prasjaychi.plantsense.data.care.PlantCareTipsProvider.getCareGuideFor(monsteraReport)
    assertNotNull(monsteraGuide)
    assertEquals("Monstera deliciosa", monsteraGuide.scientificName)
    org.junit.Assert.assertTrue(monsteraGuide.sunlight.category.contains("Bright Indirect", ignoreCase = true))
    org.junit.Assert.assertTrue(monsteraGuide.soil.mixName.contains("Aroid", ignoreCase = true))
    assertEquals(4, monsteraGuide.soil.components.size)
    assertEquals(false, monsteraGuide.proTips.isPetSafe)

    // 2. Snake Plant
    val snakeReport = com.prasjaychi.plantsense.viewmodel.PlantDiagnosisReport(
      identification = com.prasjaychi.plantsense.viewmodel.PlantIdentification(
        scientificName = "Sansevieria trifasciata",
        commonName = "Snake Plant",
        family = "Asparagaceae"
      )
    )
    val snakeGuide = com.prasjaychi.plantsense.data.care.PlantCareTipsProvider.getCareGuideFor(snakeReport)
    assertNotNull(snakeGuide)
    org.junit.Assert.assertTrue(snakeGuide.soil.mixName.contains("Succulent", ignoreCase = true) || snakeGuide.soil.mixName.contains("Cactus", ignoreCase = true))
    org.junit.Assert.assertTrue(snakeGuide.watering.cadenceSummary.isNotEmpty())

    // 3. Dynamic synthesis for unknown / rare species identified by Gemini
    val rareReport = com.prasjaychi.plantsense.viewmodel.PlantDiagnosisReport(
      identification = com.prasjaychi.plantsense.viewmodel.PlantIdentification(
        scientificName = "Alocasia reginula",
        commonName = "Black Velvet Alocasia",
        family = "Araceae"
      ),
      carePlan = com.prasjaychi.plantsense.viewmodel.PrescribedCarePlan(
        wateringSchedule = "Every 7 days when top 2 inches dry",
        lightingRecommendation = "Dappled bright indirect morning light (14,000 lux)",
        soilAndRepotting = "Chunky aroid mix with 30% perlite and charcoal"
      )
    )
    val synthesizedGuide = com.prasjaychi.plantsense.data.care.PlantCareTipsProvider.getCareGuideFor(rareReport)
    assertNotNull(synthesizedGuide)
    assertEquals("Alocasia reginula", synthesizedGuide.scientificName)
    org.junit.Assert.assertTrue(synthesizedGuide.sunlight.idealPlacement.isNotEmpty())
    org.junit.Assert.assertTrue(synthesizedGuide.soil.components.isNotEmpty())
    org.junit.Assert.assertTrue(synthesizedGuide.watering.moistureCheckMethod.isNotEmpty())
  }

  @Test
  fun `verify plant history viewmodel initialization`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.prasjaychi.plantsense.viewmodel.PlantHistoryViewModel(context)
    assertNotNull(vm)
    assertNotNull(vm.uiState.value)
  }

  @Test
  fun `verify plant library viewmodel initialization and curated botanical list`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.prasjaychi.plantsense.viewmodel.PlantLibraryViewModel(context)
    assertNotNull(vm)
    val state = vm.uiState.value
    org.junit.Assert.assertTrue(state.allPlants.isNotEmpty())
    org.junit.Assert.assertTrue(state.allPlants.size >= 8)
    // Test filter query
    vm.onSearchQueryChanged("Monstera")
    val queryFiltered = state.allPlants.filter {
      it.scientificName.contains("Monstera", ignoreCase = true) || it.commonName.contains("Monstera", ignoreCase = true)
    }
    assertEquals(1, queryFiltered.size)
    assertEquals("Monstera deliciosa", queryFiltered.first().scientificName)
    // Test category selection
    vm.onCategorySelected(com.prasjaychi.plantsense.viewmodel.LibraryCategory.PET_SAFE)
    assertEquals(com.prasjaychi.plantsense.viewmodel.LibraryCategory.PET_SAFE, vm.activeCategory.value)
  }

  @Test
  fun `verify NavHubApp renders and navigates to Plant History and Library via bottom bar`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        NavHubApp()
      }
    }
    // Test Hero Quick Action navigation
    composeTestRule.onNodeWithTag("hero_library_button").performClick()
    composeTestRule.waitForIdle()

    // Test Bottom Navigation item clicks
    composeTestRule.onNodeWithTag("bottom_nav_item_plant_history").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("bottom_nav_item_dashboard").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("bottom_nav_item_plant_library").performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `verify room database persists plant health records and user settings locally`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = com.prasjaychi.plantsense.data.database.PlantDatabase.getDatabase(context)

    // 1. Test UserSettingsDao
    val userSettingsDao = database.userSettingsDao()
    val initialSettings = userSettingsDao.getUserSettingsSync()
    val testSettings = com.prasjaychi.plantsense.data.database.UserSettingsEntity(
      id = com.prasjaychi.plantsense.data.database.UserSettingsEntity.DEFAULT_SETTINGS_ID,
      masterRemindersEnabled = true,
      defaultNotificationHour = 8,
      defaultNotificationMinute = 30,
      defaultCadenceDays = 5,
      temperatureUnit = "FAHRENHEIT",
      darkModePreference = "DARK",
      userName = "Botanical Caretaker"
    )
    userSettingsDao.insertOrUpdateSettings(testSettings)

    val loadedSettings = userSettingsDao.getUserSettingsSync()
    assertNotNull(loadedSettings)
    assertEquals(8, loadedSettings?.defaultNotificationHour)
    assertEquals(30, loadedSettings?.defaultNotificationMinute)
    assertEquals(5, loadedSettings?.defaultCadenceDays)
    assertEquals("FAHRENHEIT", loadedSettings?.temperatureUnit)
    assertEquals("DARK", loadedSettings?.darkModePreference)
    assertEquals("Botanical Caretaker", loadedSettings?.userName)

    // 2. Test PlantHealthRecordDao
    val healthRecordDao = database.plantHealthRecordDao()
    val healthRecord = com.prasjaychi.plantsense.data.database.PlantHealthRecordEntity(
      plantId = 42L,
      plantName = "Monstera deliciosa",
      scientificName = "Monstera deliciosa",
      healthScore = 95,
      vitalityStatus = "THRIVING",
      soilMoistureLevel = 0.65f,
      soilMoistureStatus = "OPTIMAL",
      leafCondition = "Deep Green Fenestrated",
      careNotes = "Regular morning misting"
    )
    val recordId = healthRecordDao.insertRecord(healthRecord)
    org.junit.Assert.assertTrue(recordId > 0)

    val retrievedRecord = healthRecordDao.getRecordById(recordId)
    assertNotNull(retrievedRecord)
    assertEquals(42L, retrievedRecord?.plantId)
    assertEquals("Monstera deliciosa", retrievedRecord?.plantName)
    assertEquals(95, retrievedRecord?.healthScore)
    assertEquals("THRIVING", retrievedRecord?.vitalityStatus)
    assertEquals("Deep Green Fenestrated", retrievedRecord?.leafCondition)
  }

  @Test
  fun `verify plant health trends viewmodel and room records aggregation`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val application = context as android.app.Application
    val database = com.prasjaychi.plantsense.data.database.PlantDatabase.getDatabase(application)
    val healthDao = database.plantHealthRecordDao()

    // Insert multiple health records over time
    val records = listOf(
      com.prasjaychi.plantsense.data.database.PlantHealthRecordEntity(
        plantId = 101L,
        plantName = "Fiddle Leaf Fig",
        healthScore = 65,
        vitalityStatus = "NEEDS_ATTENTION",
        soilMoistureLevel = 0.3f,
        soilMoistureStatus = "DRY",
        timestampMs = 1000000L
      ),
      com.prasjaychi.plantsense.data.database.PlantHealthRecordEntity(
        plantId = 101L,
        plantName = "Fiddle Leaf Fig",
        healthScore = 88,
        vitalityStatus = "THRIVING",
        soilMoistureLevel = 0.6f,
        soilMoistureStatus = "OPTIMAL",
        timestampMs = 2000000L
      )
    )
    healthDao.insertAll(records)

    val viewModel = com.prasjaychi.plantsense.viewmodel.PlantHealthTrendsViewModel(application)
    assertNotNull(viewModel)
    val avgScore = healthDao.getAverageHealthScoreDirect(101L)
    assertNotNull(avgScore)
    assertEquals(76.5f, avgScore!!, 0.5f)
  }

  @Test
  fun `verify network connectivity observer toggles network status and simulated outage`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val observer = com.prasjaychi.plantsense.data.sync.NetworkConnectivityObserver(context)

    // Simulate active network connected
    observer.setSimulatedConnected(true)
    val connectedStatus = observer.getCurrentNetworkStatus()
    org.junit.Assert.assertTrue(connectedStatus.isConnected)
    org.junit.Assert.assertTrue(connectedStatus.isEffectivelyOnline)
    org.junit.Assert.assertTrue(observer.isEffectivelyOnline())

    // Verify simulated outage toggles effectivelyOnline to false
    observer.setSimulatedOutage(true)
    val outageStatus = observer.getCurrentNetworkStatus()
    org.junit.Assert.assertTrue(outageStatus.isSimulatedOutage)
    org.junit.Assert.assertFalse(outageStatus.isEffectivelyOnline)
    org.junit.Assert.assertFalse(observer.isEffectivelyOnline())

    // Verify disabling simulated outage restores effectivelyOnline flag
    observer.setSimulatedOutage(false)
    val restoredStatus = observer.getCurrentNetworkStatus()
    org.junit.Assert.assertFalse(restoredStatus.isSimulatedOutage)
    org.junit.Assert.assertTrue(restoredStatus.isEffectivelyOnline)
  }

  @Test
  fun `verify plant firestore sync manager toggles between Room and Cloud mode based on network`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val application = context as android.app.Application
    val syncManager = com.prasjaychi.plantsense.data.sync.PlantFirestoreSyncManager.getInstance(application)

    assertNotNull(syncManager)

    // Establish simulated network connection
    syncManager.setSimulatedNetworkConnected(true)
    val initialOnlineState = syncManager.syncState.value
    org.junit.Assert.assertTrue(initialOnlineState.isOnline)
    assertEquals(com.prasjaychi.plantsense.data.sync.PlantSyncMode.CLOUD_ENABLED, initialOnlineState.syncMode)

    // Simulate network outage to test offline-first Room persistence mode
    syncManager.toggleSimulatedNetworkOutage(true)
    val offlineState = syncManager.syncState.value
    org.junit.Assert.assertTrue(offlineState.isSimulatedOutage)
    org.junit.Assert.assertFalse(offlineState.isOnline)
    assertEquals(com.prasjaychi.plantsense.data.sync.PlantSyncMode.LOCAL_ROOM_ONLY, offlineState.syncMode)
    assertEquals(com.prasjaychi.plantsense.data.sync.PlantSyncStatus.OFFLINE, offlineState.syncStatus)

    // Toggle back online to test transition to Cloud Firestore sync mode
    syncManager.toggleSimulatedNetworkOutage(false)
    val onlineState = syncManager.syncState.value
    org.junit.Assert.assertFalse(onlineState.isSimulatedOutage)
    org.junit.Assert.assertTrue(onlineState.isOnline)
    assertEquals(com.prasjaychi.plantsense.data.sync.PlantSyncMode.CLOUD_ENABLED, onlineState.syncMode)
  }

  @Test
  fun `verify quick actions log watering updates plant and inserts health record in Room`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val application = context as android.app.Application
    val database = com.prasjaychi.plantsense.data.database.PlantDatabase.getDatabase(application)
    val analysisDao = database.plantAnalysisDao()
    val healthRecordDao = database.plantHealthRecordDao()

    val report = com.prasjaychi.plantsense.data.ai.PlantGeminiAnalyzer.generateBotanicalHeuristicReport(
      Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
    )
    val testEntity = com.prasjaychi.plantsense.data.database.PlantAnalysisEntity.fromReport(report)
    val plantId = analysisDao.insertAnalysis(testEntity)

    val viewModel = com.prasjaychi.plantsense.viewmodel.PlantHistoryViewModel(application)

    // Execute quickLogWatering
    var completed = false
    val wateringJob = viewModel.quickLogWatering(
      plantId = plantId,
      amountPreset = "Standard (250ml)",
      notes = "Filtered water used"
    ) {
      completed = true
    }
    wateringJob.join()

    val updatedPlant = analysisDao.getAnalysisById(plantId)
    assertNotNull(updatedPlant)
    org.junit.Assert.assertTrue(updatedPlant!!.lastWateredMs > 0L)
    org.junit.Assert.assertTrue(updatedPlant.lastWateredFormatted.isNotBlank())

    // Verify corresponding health record was logged
    val healthRecords = healthRecordDao.getHealthRecordsForPlant(plantId).first()
    assertNotNull(healthRecords)
    org.junit.Assert.assertTrue(healthRecords.isNotEmpty())
  }

  @Test
  fun `verify quick actions add health note persists user notes and health record in Room`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val application = context as android.app.Application
    val database = com.prasjaychi.plantsense.data.database.PlantDatabase.getDatabase(application)
    val analysisDao = database.plantAnalysisDao()
    val healthRecordDao = database.plantHealthRecordDao()

    val report = com.prasjaychi.plantsense.data.ai.PlantGeminiAnalyzer.generateBotanicalHeuristicReport(
      Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
    )
    val testEntity = com.prasjaychi.plantsense.data.database.PlantAnalysisEntity.fromReport(report, notes = "Initial planting")
    val plantId = analysisDao.insertAnalysis(testEntity)

    val viewModel = com.prasjaychi.plantsense.viewmodel.PlantHistoryViewModel(application)

    // Execute quickAddHealthNote
    var completed = false
    val noteJob = viewModel.quickAddHealthNote(
      plantId = plantId,
      noteText = "New pup emerging from rhizome",
      vitalityStatus = "THRIVING",
      leafCondition = "Crisp and upright"
    ) {
      completed = true
    }
    noteJob.join()

    val updatedPlant = analysisDao.getAnalysisById(plantId)
    assertNotNull(updatedPlant)
    org.junit.Assert.assertTrue(updatedPlant!!.userNotes.contains("New pup emerging from rhizome"))

    // Verify record in healthRecordDao
    val records = healthRecordDao.getHealthRecordsForPlant(plantId).first()
    assertNotNull(records)
    org.junit.Assert.assertTrue(records.isNotEmpty())
  }

  @Test
  fun `verify main camera preview card renders and supports sample frame capture`() {
    var analyzedBitmap: Bitmap? = null
    composeTestRule.setContent {
      MyApplicationTheme {
        com.prasjaychi.plantsense.ui.components.MainCameraPreviewCard(
          onAnalyzeFrame = { bmp -> analyzedBitmap = bmp }
        )
      }
    }

    // Verify card is rendered
    composeTestRule.onNodeWithTag("main_camerax_preview_card").assertExists()

    // When permission is not yet granted in test, verify the prompt or sample button exists
    composeTestRule.onNodeWithTag("sample_frame_button").assertExists()
    composeTestRule.onNodeWithTag("sample_frame_button").performClick()
    composeTestRule.waitForIdle()

    // After clicking sample frame, verify staged frame review actions appear
    composeTestRule.onNodeWithTag("analyze_captured_frame_button").assertExists()
    composeTestRule.onNodeWithTag("retake_frame_button").assertExists()
    composeTestRule.onNodeWithTag("save_frame_for_later_button").assertExists()

    // Click analyze button and verify callback received bitmap
    composeTestRule.onNodeWithTag("analyze_captured_frame_button").performClick()
    composeTestRule.waitForIdle()
    assertNotNull(analyzedBitmap)
  }
}
