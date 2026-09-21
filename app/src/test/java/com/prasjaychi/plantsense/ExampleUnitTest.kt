package com.prasjaychi.plantsense

import com.prasjaychi.plantsense.data.database.PlantAnalysisEntity
import com.prasjaychi.plantsense.viewmodel.DiagnosisSeverity
import com.prasjaychi.plantsense.viewmodel.PlantDiagnosisReport
import com.prasjaychi.plantsense.viewmodel.PlantIdentification
import com.prasjaychi.plantsense.viewmodel.PrescribedCarePlan
import com.prasjaychi.plantsense.viewmodel.RootCauseDiagnosis
import org.junit.Assert.*
import org.junit.Test

/**
 * Local unit tests verifying Room database entity conversion and default report mapping.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testPlantAnalysisEntity_conversionRoundTrip() {
    val sampleReport = PlantDiagnosisReport(
      identification = PlantIdentification(
        scientificName = "Monstera deliciosa",
        commonName = "Swiss Cheese Plant",
        family = "Araceae",
        matchConfidence = 98.4f
      ),
      rootCause = RootCauseDiagnosis(
        primaryCause = "Substrate Compaction",
        rootCauseCategory = "Overwatering",
        severity = DiagnosisSeverity.MODERATE,
        symptomsDetected = listOf("Yellowing lower foliage", "Tip browning")
      ),
      carePlan = PrescribedCarePlan(
        immediateIntervention = "Suspend watering for 7 days"
      ),
      healthScore = 78
    )

    val entity = PlantAnalysisEntity.fromReport(sampleReport, notes = "Balcony plant", isFavorite = true)
    assertEquals("Monstera deliciosa", entity.scientificName)
    assertEquals("Swiss Cheese Plant", entity.commonName)
    assertEquals("MODERATE", entity.severity)
    assertTrue(entity.isFavorite)
    assertEquals("Balcony plant", entity.userNotes)

    val convertedReport = entity.toReport()
    assertEquals(sampleReport.identification.scientificName, convertedReport.identification.scientificName)
    assertEquals(sampleReport.identification.commonName, convertedReport.identification.commonName)
    assertEquals(DiagnosisSeverity.MODERATE, convertedReport.rootCause.severity)
    assertEquals(2, convertedReport.rootCause.symptomsDetected.size)
    assertEquals("Yellowing lower foliage", convertedReport.rootCause.symptomsDetected[0])
    assertTrue(convertedReport.isSavedToWorkspace)
  }

  @Test
  fun testPlantHistoryViewModel_hasApplicationConstructor() {
    val constructor = com.prasjaychi.plantsense.viewmodel.PlantHistoryViewModel::class.java.getConstructor(android.app.Application::class.java)
    assertNotNull("PlantHistoryViewModel must have a constructor taking Application for AndroidViewModelFactory", constructor)
  }
}

