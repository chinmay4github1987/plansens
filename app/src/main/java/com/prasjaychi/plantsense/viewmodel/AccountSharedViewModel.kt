package com.prasjaychi.plantsense.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TeamMember(
    val id: String,
    val name: String,
    val role: String,
    val email: String,
    val avatarColorHex: Long = 0xFF6366F1
)

data class AccountState(
    val fullName: String = "Sarah Chen",
    val titleRole: String = "Principal Architect",
    val email: String = "sarah.chen@cloudstack.io",
    val organization: String = "CloudStack Global",
    val bio: String = "Specializing in distributed systems, micro-frontends, and reactive Android apps.",
    val avatarColorHex: Long = 0xFF4338CA,
    val twoFactorEnabled: Boolean = true,
    val sessionTimeoutMinutes: Int = 30,
    val biometricUnlock: Boolean = true,
    val teamMembers: List<TeamMember> = listOf(
        TeamMember("1", "Sarah Chen", "Owner / Lead", "sarah.chen@cloudstack.io", 0xFF4338CA),
        TeamMember("2", "Marcus Vance", "Senior DevOps", "m.vance@cloudstack.io", 0xFF0284C7),
        TeamMember("3", "Elena Rostova", "Staff Frontend", "elena.r@cloudstack.io", 0xFF8B5CF6),
        TeamMember("4", "Kenji Sato", "QA Specialist", "k.sato@cloudstack.io", 0xFF10B981)
    )
)

/**
 * Shared ViewModel scoped to the nested [NavRoutes.ACCOUNT_GRAPH].
 *
 * Preserved across:
 * - Profile overview (account/profile)
 * - Profile edit screen (account/edit)
 * - Security configurations (account/security)
 * - Team management (account/team)
 */
class AccountSharedViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AccountState())
    val uiState: StateFlow<AccountState> = _uiState.asStateFlow()

    fun updateProfile(name: String, role: String, email: String, bio: String, avatarColorHex: Long) {
        _uiState.update { current ->
            current.copy(
                fullName = name,
                titleRole = role,
                email = email,
                bio = bio,
                avatarColorHex = avatarColorHex,
                teamMembers = current.teamMembers.map {
                    if (it.id == "1") it.copy(name = name, role = role, email = email, avatarColorHex = avatarColorHex)
                    else it
                }
            )
        }
    }

    fun setTwoFactorEnabled(enabled: Boolean) {
        _uiState.update { it.copy(twoFactorEnabled = enabled) }
    }

    fun setSessionTimeout(minutes: Int) {
        _uiState.update { it.copy(sessionTimeoutMinutes = minutes) }
    }

    fun setBiometricUnlock(enabled: Boolean) {
        _uiState.update { it.copy(biometricUnlock = enabled) }
    }

    fun addTeamMember(name: String, role: String, email: String) {
        val colorPool = listOf(0xFF0284C7, 0xFF8B5CF6, 0xFF10B981, 0xFFF59E0B, 0xFFF43F5E)
        val color = colorPool.random()
        val newMember = TeamMember(
            id = System.currentTimeMillis().toString(),
            name = name,
            role = role,
            email = email,
            avatarColorHex = color
        )
        _uiState.update { it.copy(teamMembers = it.teamMembers + newMember) }
    }

    fun removeTeamMember(id: String) {
        // Prevent removing the owner
        if (id == "1") return
        _uiState.update { it.copy(teamMembers = it.teamMembers.filterNot { m -> m.id == id }) }
    }
}
