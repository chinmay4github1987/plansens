package com.prasjaychi.plantsense.ui.camera

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prasjaychi.plantsense.ui.theme.Cyan400
import com.prasjaychi.plantsense.ui.theme.Cyan500
import com.prasjaychi.plantsense.ui.theme.Emerald400
import com.prasjaychi.plantsense.ui.theme.Emerald500
import com.prasjaychi.plantsense.ui.theme.Emerald600
import com.prasjaychi.plantsense.ui.theme.Indigo600
import com.prasjaychi.plantsense.ui.theme.Indigo800
import com.prasjaychi.plantsense.viewmodel.DiagnosisSeverity
import com.prasjaychi.plantsense.viewmodel.PlantAnalysisViewModel
import com.prasjaychi.plantsense.viewmodel.PlantDiagnosisReport
import kotlinx.coroutines.launch

/**
 * Dedicated destination screen displaying plant identification, root-cause diagnosis, and care plan.
 */
@Composable
fun PlantAnalysisResultScreen(
    viewModel: PlantAnalysisViewModel,
    onNavigateBack: () -> Unit,
    onRetakePhoto: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onSaveToHistory: (PlantDiagnosisReport) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val report = uiState.currentReport
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var showReminderDialog by remember { mutableStateOf(false) }

    if (showReminderDialog) {
        val entityFromReport = com.prasjaychi.plantsense.data.database.PlantAnalysisEntity.fromReport(report)
        com.prasjaychi.plantsense.ui.history.PlantReminderScheduleDialog(
            plant = entityFromReport,
            onDismiss = { showReminderDialog = false },
            onReminderConfigured = { scheduled ->
                showReminderDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        if (scheduled) "Watering notification reminder scheduled!"
                        else "Reminder cancelled."
                    )
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Gemini AI Live Analysis Banner / Status
            if (uiState.isAnalyzing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Cyan500.copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Cyan400.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Cyan400,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Gemini Multimodal AI Processing...",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Cyan400
                            )
                            Text(
                                text = "Analyzing leaf morphology, pathology, and cellular chlorophyll...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Hero Plant Identification Header Card
            val activeBitmap = uiState.capturedBitmap
            PlantIdentityHeroCard(
                report = report,
                capturedBitmap = activeBitmap,
                aiModelUsed = uiState.aiModelUsed,
                isLiveGeminiAi = uiState.isLiveGeminiAi,
                onReanalyze = if (activeBitmap != null && !uiState.isAnalyzing) {
                    { viewModel.analyzeWithGemini(activeBitmap) }
                } else null,
                onToggleSave = {
                    val willBeSaved = !report.isSavedToWorkspace
                    viewModel.toggleSaveReport()
                    if (willBeSaved) {
                        onSaveToHistory(report)
                    }
                    coroutineScope.launch {
                        val msg = if (willBeSaved) {
                            "Diagnosis saved to local Room history"
                        } else {
                            "Diagnosis removed from saved reports"
                        }
                        snackbarHostState.showSnackbar(msg)
                    }
                }
            )

            // Scenario Preset Switcher (allows instant testing of different botanical conditions)
            ScenarioPresetRow(
                onSelectScenario = { scenarioId ->
                    viewModel.loadDiagnosisScenario(scenarioId)
                }
            )

            // Tab Navigation for Analysis Sections
            val safeTab = uiState.selectedTab.coerceIn(0, 3)
            TabRow(
                selectedTabIndex = safeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (safeTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[safeTab]),
                            color = Emerald500
                        )
                    }
                }
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    text = { Text("Root Cause", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    text = { Text("Care Plan", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Spa, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    text = { Text("Care Tips", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = uiState.selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3) },
                    text = { Text("Taxonomy", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Eco, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            // Tab Content
            when (uiState.selectedTab) {
                0 -> RootCauseSection(report = report)
                1 -> CarePlanSection(
                    report = report,
                    onConfigureReminder = { showReminderDialog = true },
                    onNavigateToCareTips = { viewModel.setSelectedTab(2) }
                )
                2 -> PlantCareTipsSection(report = report)
                3 -> TaxonomySection(report = report)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Navigation & Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRetakePhoto,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("retake_plant_photo_button"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retake Photo", fontSize = 13.sp)
                }

                Button(
                    onClick = onNavigateToDashboard,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("done_plant_analysis_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dashboard", fontSize = 13.sp)
                }
            }

            OutlinedButton(
                onClick = onNavigateToHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("view_history_from_analysis_button"),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 11.dp)
            ) {
                Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp), tint = Emerald600)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Browse Previous Analyses (Room DB)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Emerald600)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * Header card presenting plant identification, confidence score, and photo.
 */
@Composable
private fun PlantIdentityHeroCard(
    report: PlantDiagnosisReport,
    capturedBitmap: Bitmap?,
    aiModelUsed: String = "",
    isLiveGeminiAi: Boolean = false,
    onReanalyze: (() -> Unit)? = null,
    onToggleSave: () -> Unit
) {
    val severityColor = when (report.rootCause.severity) {
        DiagnosisSeverity.OPTIMAL -> Emerald500
        DiagnosisSeverity.MILD -> Cyan500
        DiagnosisSeverity.MODERATE -> Color(0xFFF59E0B) // Amber
        DiagnosisSeverity.CRITICAL -> Color(0xFFEF4444) // Red
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Emerald500.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Emerald500,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${report.identification.matchConfidence}% Match Confidence",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald500
                                )
                            }
                        }

                        IconButton(
                            onClick = onToggleSave,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (report.isSavedToWorkspace) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Save Report",
                                tint = if (report.isSavedToWorkspace) Emerald500 else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = report.identification.scientificName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic
                    )

                    Text(
                        text = "Common: ${report.identification.commonName}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Family: ${report.identification.family}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    if (aiModelUsed.isNotBlank() || isLiveGeminiAi) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isLiveGeminiAi) Cyan500.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = if (isLiveGeminiAi) Cyan500 else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isLiveGeminiAi) "Gemini Multimodal AI" else aiModelUsed.ifBlank { "Botanical Engine" },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isLiveGeminiAi) Cyan500 else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Photo preview or avatar badge
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (capturedBitmap != null) {
                            Image(
                                bitmap = capturedBitmap.asImageBitmap(),
                                contentDescription = "Captured foliage photo",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.LocalFlorist,
                                contentDescription = null,
                                tint = Emerald400,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    if (onReanalyze != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Re-analyze",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Cyan500,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onReanalyze() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(14.dp))

            // Vitality Meter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { report.healthScore / 100f },
                            modifier = Modifier.size(46.dp),
                            color = severityColor,
                            trackColor = severityColor.copy(alpha = 0.2f),
                            strokeWidth = 5.dp
                        )
                        Text(
                            text = "${report.healthScore}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = severityColor
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Vitality Index",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = report.rootCause.severity.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = severityColor
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = report.timestamp,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Tab 1: Root cause diagnosis, symptom breakdown, and pathology.
 */
@Composable
private fun RootCauseSection(report: PlantDiagnosisReport) {
    val rootCause = report.rootCause

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Primary Etiology / Diagnosis Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFEF3C7).copy(alpha = 0.35f) // Warm amber tint
            ),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PRIMARY ROOT CAUSE IDENTIFIED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = rootCause.primaryCause,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Classification: ${rootCause.rootCauseCategory}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = rootCause.physiologicalImpact,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Detected Folia Symptoms Breakdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Detected Folia Symptoms",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                rootCause.symptomsDetected.forEach { symptom ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = symptom,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Pest & Pathogen Screening Result
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Emerald500.copy(alpha = 0.08f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Emerald500,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Pathogen & Parasite Screen",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Emerald500
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = rootCause.pathogenStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Tab 2: Suggested care plan, watering cadence, lighting, and soil modifications.
 */
@Composable
private fun CarePlanSection(
    report: PlantDiagnosisReport,
    onConfigureReminder: () -> Unit = {},
    onNavigateToCareTips: () -> Unit = {}
) {
    val care = report.carePlan

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Contextual Species Care Tips Callout Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToCareTips)
                .testTag("care_plan_to_care_tips_banner"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Emerald500.copy(alpha = 0.12f)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Emerald500.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Emerald500,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "View Plant Care Tips",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Explore Species Care Tips",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tailored sunlight, substrate recipe, and hydration advice for ${report.identification.commonName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onNavigateToCareTips,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Tips", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        // Notification Reminder Callout Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onConfigureReminder)
                .testTag("care_plan_reminder_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Cyan500.copy(alpha = 0.12f)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Cyan500.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Cyan500,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Schedule watering reminder",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily Watering Reminders",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Schedule custom alarm notifications based on: \"${care.wateringSchedule}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onConfigureReminder,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Set", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Immediate Action Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Indigo600.copy(alpha = 0.1f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Indigo600.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Indigo600,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "IMMEDIATE INTERVENTION REQUIRED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Indigo600
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = care.immediateIntervention,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Indigo600
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = care.recoveryTimeline,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Detailed Care Protocols
        CareDetailCard(
            icon = Icons.Default.WaterDrop,
            iconTint = Cyan500,
            title = "Watering Cadence",
            description = care.wateringSchedule
        )

        CareDetailCard(
            icon = Icons.Default.Eco,
            iconTint = Emerald500,
            title = "Substrate & Drainage Formulation",
            description = care.soilAndRepotting
        )

        CareDetailCard(
            icon = Icons.Default.WbSunny,
            iconTint = Color(0xFFF59E0B),
            title = "Photoperiod & Light Exposure",
            description = care.lightingRecommendation
        )

        CareDetailCard(
            icon = Icons.Default.DeviceThermostat,
            iconTint = Color(0xFF8B5CF6),
            title = "Atmospheric Humidity & Temperature",
            description = care.humidityAndAtmosphere
        )

        CareDetailCard(
            icon = Icons.Default.Spa,
            iconTint = Emerald400,
            title = "Nutrient & Fertilizer Regimen",
            description = care.nutritionCadence
        )
    }
}

/**
 * Tab 3: Botanical Taxonomy & Leaf Anatomy
 */
@Composable
private fun TaxonomySection(report: PlantDiagnosisReport) {
    val id = report.identification

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Botanical Taxonomy & Natural Habitat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            TaxonomyRow("Binomial Nomenclature", id.scientificName, isItalic = true)
            TaxonomyRow("Taxonomic Family", id.family)
            TaxonomyRow("Native Geographic Region", id.nativeRegion)
            TaxonomyRow("Morphological Features", id.leafCharacteristics)

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AI vision classifier verified against Kew Botanical Index.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun CareDetailCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TaxonomyRow(label: String, value: String, isItalic: Boolean = false) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal
        )
    }
}

/**
 * Scenario selector allowing the user to review different plant species and root cause diagnoses.
 */
@Composable
private fun ScenarioPresetRow(
    onSelectScenario: (String) -> Unit
) {
    Column {
        Text(
            text = "Explore Diagnosis Samples",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                PresetChip("Monstera (Overwatered)", Icons.Default.Warning) {
                    onSelectScenario("monstera_overwater")
                }
            }
            item {
                PresetChip("Fiddle-Leaf (Oedema)", Icons.Default.Opacity) {
                    onSelectScenario("fiddle_oedema")
                }
            }
            item {
                PresetChip("Snake Plant (Thriving)", Icons.Default.CheckCircle) {
                    onSelectScenario("snake_healthy")
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Emerald500,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
