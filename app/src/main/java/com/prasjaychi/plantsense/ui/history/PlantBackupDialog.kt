package com.prasjaychi.plantsense.ui.history

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prasjaychi.plantsense.data.backup.CsvExportResult
import com.prasjaychi.plantsense.data.backup.CsvImportResult
import com.prasjaychi.plantsense.ui.theme.Cyan400
import com.prasjaychi.plantsense.ui.theme.Cyan500
import com.prasjaychi.plantsense.ui.theme.Emerald400
import com.prasjaychi.plantsense.ui.theme.Emerald500
import com.prasjaychi.plantsense.ui.theme.Emerald600
import com.prasjaychi.plantsense.ui.theme.Indigo600
import com.prasjaychi.plantsense.ui.theme.Indigo900
import com.prasjaychi.plantsense.viewmodel.PlantHistoryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-craft Dialog for Exporting and Importing PlantSense botanical history via CSV backup files.
 */
@Composable
fun PlantBackupDialog(
    viewModel: PlantHistoryViewModel,
    totalRecords: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Export, 1 = Import
    var isProcessing by remember { mutableStateOf(false) }

    // Export states
    var exportResult by remember { mutableStateOf<CsvExportResult?>(null) }
    var previewCsvText by remember { mutableStateOf<String?>(null) }

    // Import states
    var importStrategy by remember { mutableStateOf("MERGE") } // "MERGE" or "OVERWRITE"
    var importResult by remember { mutableStateOf<CsvImportResult?>(null) }

    // SAF Document Creator launcher for CSV export
    val timeStamp = remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date()) }
    val defaultFileName = "PlantSense_Backup_$timeStamp.csv"

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            viewModel.exportToUri(context, uri) { res ->
                isProcessing = false
                exportResult = res
                if (res.success) {
                    Toast.makeText(context, "Exported ${res.count} plant records to CSV successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Export failed: ${res.errorMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // SAF Document Picker launcher for CSV import
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            val isOverwrite = importStrategy == "OVERWRITE"
            viewModel.importFromUri(context, uri, overwrite = isOverwrite) { res ->
                isProcessing = false
                importResult = res
                if (res.success) {
                    val msg = if (isOverwrite) {
                        "Restored ${res.insertedCount} botanical records!"
                    } else {
                        "Merged ${res.insertedCount} new, updated ${res.updatedCount} records!"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Import failed: ${res.errorMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .testTag("plant_backup_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Emerald500.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = Emerald600,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Database Backup & Restore",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Room Botanical History CSV Manager",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Selector (Export vs Import)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = Emerald600,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Emerald500
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export CSV", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import Backup", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Tab Content
                if (selectedTab == 0) {
                    // EXPORT TAB
                    ExportTabContent(
                        totalRecords = totalRecords,
                        isProcessing = isProcessing,
                        exportResult = exportResult,
                        onShareExport = {
                            isProcessing = true
                            viewModel.exportToShareableFile(context) { res, intent ->
                                isProcessing = false
                                exportResult = res
                                if (intent != null) {
                                    context.startActivity(android.content.Intent.createChooser(intent, "Share PlantSense CSV Backup"))
                                } else if (!res.success) {
                                    Toast.makeText(context, res.errorMessage ?: "Failed to generate file", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onSaveToDevice = {
                            createDocumentLauncher.launch(defaultFileName)
                        }
                    )
                } else {
                    // IMPORT TAB
                    ImportTabContent(
                        importStrategy = importStrategy,
                        onStrategyChanged = { importStrategy = it },
                        isProcessing = isProcessing,
                        importResult = importResult,
                        onSelectFile = {
                            openDocumentLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
                        }
                    )
                }
            }
        }
    }
}

/**
 * Content for Exporting Plant history to CSV.
 */
@Composable
private fun ExportTabContent(
    totalRecords: Int,
    isProcessing: Boolean,
    exportResult: CsvExportResult?,
    onShareExport: () -> Unit,
    onSaveToDevice: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Emerald500.copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Emerald600,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Ready to Export $totalRecords Records",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald600
                    )
                    Text(
                        text = "Generates an RFC-4180 compliant CSV backup including scientific names, symptoms, care regimes, health indices, and watering logs.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Export Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onSaveToDevice,
                enabled = !isProcessing && totalRecords > 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                modifier = Modifier
                    .weight(1f)
                    .testTag("export_save_file_button")
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = onShareExport,
                enabled = !isProcessing && totalRecords > 0,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("export_share_button")
            ) {
                Icon(imageVector = Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(16.dp), tint = Emerald600)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share Backup", fontSize = 12.sp, color = Emerald600, fontWeight = FontWeight.Bold)
            }
        }

        // Export Result Status
        if (exportResult != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (exportResult.success) Emerald500.copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (exportResult.success) Emerald500.copy(alpha = 0.3f) else Color(0xFFEF4444).copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (exportResult.success) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (exportResult.success) Emerald600 else Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (exportResult.success) {
                            "Successfully exported ${exportResult.count} plant history records (${exportResult.fileName})!"
                        } else {
                            "Export failed: ${exportResult.errorMessage}"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (exportResult.success) Emerald600 else Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

/**
 * Content for Importing / Restoring Plant history from CSV.
 */
@Composable
private fun ImportTabContent(
    importStrategy: String,
    onStrategyChanged: (String) -> Unit,
    isProcessing: Boolean,
    importResult: CsvImportResult?,
    onSelectFile: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Select Import Strategy:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Strategy 1: Merge & Append
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (importStrategy == "MERGE") Emerald500.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (importStrategy == "MERGE") Emerald500 else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onStrategyChanged("MERGE") }
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = importStrategy == "MERGE",
                    onClick = { onStrategyChanged("MERGE") },
                    colors = RadioButtonDefaults.colors(selectedColor = Emerald500)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Merge & Append", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Emerald500.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Safe",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald600,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Preserves existing records in Room, adds new plants, and updates matches.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Strategy 2: Overwrite / Replace
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (importStrategy == "OVERWRITE") Color(0xFFEF4444).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (importStrategy == "OVERWRITE") Color(0xFFEF4444) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onStrategyChanged("OVERWRITE") }
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = importStrategy == "OVERWRITE",
                    onClick = { onStrategyChanged("OVERWRITE") },
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFEF4444))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Full Restore (Overwrite)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Replace All",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Clears the local Room database and replaces it with records from the CSV file.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pick File Button
        Button(
            onClick = onSelectFile,
            enabled = !isProcessing,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (importStrategy == "MERGE") Emerald500 else Color(0xFFEF4444)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("import_select_file_button")
        ) {
            if (isProcessing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Parsing & Restoring Database...", fontSize = 13.sp)
            } else {
                Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select CSV File to Import", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Import Result Status
        if (importResult != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (importResult.success) Emerald500.copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (importResult.success) Emerald500.copy(alpha = 0.3f) else Color(0xFFEF4444).copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (importResult.success) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (importResult.success) Emerald600 else Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (importResult.success) "Import Completed Successfully" else "Import Failed",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (importResult.success) Emerald600 else Color(0xFFEF4444)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (importResult.success) {
                            "Total parsed: ${importResult.totalParsed} • Inserted: ${importResult.insertedCount} • Updated: ${importResult.updatedCount} • Skipped: ${importResult.skippedCount}"
                        } else {
                            importResult.errorMessage ?: "Unknown error while parsing CSV."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
