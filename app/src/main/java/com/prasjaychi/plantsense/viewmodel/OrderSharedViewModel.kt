package com.prasjaychi.plantsense.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class PlanTier(
    val id: String,
    val title: String,
    val baseMonthlyPrice: Int,
    val badge: String?,
    val seats: String,
    val storage: String,
    val description: String
) {
    STARTER(
        id = "starter",
        title = "Starter",
        baseMonthlyPrice = 19,
        badge = null,
        seats = "Up to 3 members",
        storage = "25 GB Cloud Storage",
        description = "Ideal for agile indie developers and early stage side-projects."
    ),
    PROFESSIONAL(
        id = "pro",
        title = "Professional",
        baseMonthlyPrice = 49,
        badge = "Most Popular",
        seats = "Up to 15 members",
        storage = "200 GB Cloud Storage",
        description = "High performance infrastructure with priority SLA and real-time metrics."
    ),
    ENTERPRISE(
        id = "enterprise",
        title = "Enterprise",
        baseMonthlyPrice = 129,
        badge = "Full Scale",
        seats = "Unlimited members",
        storage = "2 TB Dedicated Storage",
        description = "Isolated multi-region clusters, custom VPC peering, and 24/7 phone support."
    )
}

enum class BillingCycle(val label: String, val discountPercent: Int) {
    MONTHLY("Monthly", 0),
    ANNUAL("Annual (Save 20%)", 20)
}

data class PlanAddon(
    val id: String,
    val name: String,
    val description: String,
    val monthlyCost: Int,
    val iconName: String
)

data class OrderState(
    val selectedTier: PlanTier = PlanTier.PROFESSIONAL,
    val billingCycle: BillingCycle = BillingCycle.MONTHLY,
    val enabledAddonIds: Set<String> = setOf("backup", "ssl"),
    val workspaceName: String = "Acme Innovations",
    val adminEmail: String = "dev-lead@acme.org",
    val selectedRegion: String = "US-East (N. Virginia)",
    val isSubmitted: Boolean = false,
    val orderId: String = "",
    val completedTimestamp: String = ""
) {
    val availableAddons = listOf(
        PlanAddon(
            id = "support",
            name = "Dedicated 24/7 SLA",
            description = "15-minute guaranteed response time for critical incidents.",
            monthlyCost = 15,
            iconName = "Support"
        ),
        PlanAddon(
            id = "backup",
            name = "Automated Real-Time Backups",
            description = "Continuous point-in-time recovery across 3 availability zones.",
            monthlyCost = 10,
            iconName = "Backup"
        ),
        PlanAddon(
            id = "ssl",
            name = "Custom Domain & Wildcard SSL",
            description = "Zero-config auto-renewing TLS certificates for your domains.",
            monthlyCost = 5,
            iconName = "Security"
        ),
        PlanAddon(
            id = "audit",
            name = "SOC-2 Compliance & Audit Logs",
            description = "Cryptographically signed event ledger with 3-year retention.",
            monthlyCost = 25,
            iconName = "Audit"
        )
    )

    val monthlySubtotal: Int
        get() {
            val tierPrice = selectedTier.baseMonthlyPrice
            val addonsPrice = availableAddons
                .filter { it.id in enabledAddonIds }
                .sumOf { it.monthlyCost }
            return tierPrice + addonsPrice
        }

    val finalMonthlyCost: Int
        get() {
            val sub = monthlySubtotal
            return if (billingCycle == BillingCycle.ANNUAL) {
                (sub * (100 - billingCycle.discountPercent)) / 100
            } else {
                sub
            }
        }

    val annualBilledTotal: Int
        get() = finalMonthlyCost * 12
}

/**
 * Shared ViewModel scoped to the nested [NavRoutes.ORDER_GRAPH].
 *
 * It is preserved across Step 1 (Plan Selection) -> Step 2 (Add-ons) -> Step 3 (Review & Workspace)
 * -> Step 4 (Order Confirmation).
 */
class OrderSharedViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OrderState())
    val uiState: StateFlow<OrderState> = _uiState.asStateFlow()

    fun selectPlan(tier: PlanTier) {
        _uiState.update { it.copy(selectedTier = tier) }
    }

    fun setBillingCycle(cycle: BillingCycle) {
        _uiState.update { it.copy(billingCycle = cycle) }
    }

    fun toggleAddon(addonId: String) {
        _uiState.update { current ->
            val updated = current.enabledAddonIds.toMutableSet()
            if (updated.contains(addonId)) {
                updated.remove(addonId)
            } else {
                updated.add(addonId)
            }
            current.copy(enabledAddonIds = updated)
        }
    }

    fun updateWorkspaceName(name: String) {
        _uiState.update { it.copy(workspaceName = name) }
    }

    fun updateAdminEmail(email: String) {
        _uiState.update { it.copy(adminEmail = email) }
    }

    fun updateRegion(region: String) {
        _uiState.update { it.copy(selectedRegion = region) }
    }

    fun submitOrder(): String {
        val generatedId = "ORD-" + UUID.randomUUID().toString().take(8).uppercase()
        val timeStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())
        _uiState.update {
            it.copy(
                isSubmitted = true,
                orderId = generatedId,
                completedTimestamp = timeStr
            )
        }
        return generatedId
    }

    fun resetOrder() {
        _uiState.value = OrderState()
    }
}
