package com.prasjaychi.plantsense.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NavEventLog(
    val id: Long = System.currentTimeMillis(),
    val timestamp: String,
    val actionType: String,
    val destination: String,
    val parentGraph: String?,
    val details: String
)

data class NavInspectorState(
    val currentRoute: String = "dashboard",
    val currentParentGraph: String? = null,
    val backstackDepth: Int = 1,
    val eventLogs: List<NavEventLog> = emptyList(),
    val isInspectorSheetVisible: Boolean = false
)

class NavInspectorViewModel : ViewModel() {

    private val _state = MutableStateFlow(NavInspectorState())
    val state: StateFlow<NavInspectorState> = _state.asStateFlow()

    fun logNavigation(
        actionType: String,
        destination: String,
        parentGraph: String? = null,
        details: String = ""
    ) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val newLog = NavEventLog(
            timestamp = time,
            actionType = actionType,
            destination = destination,
            parentGraph = parentGraph,
            details = details
        )
        _state.update {
            it.copy(
                currentRoute = destination,
                currentParentGraph = parentGraph,
                eventLogs = (listOf(newLog) + it.eventLogs).take(40)
            )
        }
    }

    fun updateBackstackDepth(depth: Int) {
        _state.update { it.copy(backstackDepth = depth) }
    }

    fun toggleInspectorSheet(visible: Boolean? = null) {
        _state.update { it.copy(isInspectorSheetVisible = visible ?: !it.isInspectorSheetVisible) }
    }

    fun clearLogs() {
        _state.update { it.copy(eventLogs = emptyList()) }
    }
}
