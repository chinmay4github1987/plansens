package com.prasjaychi.plantsense.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

enum class ConnectionType {
    WIFI,
    CELLULAR,
    ETHERNET,
    OTHER,
    NONE
}

data class NetworkStatus(
    val isConnected: Boolean = false,
    val connectionType: ConnectionType = ConnectionType.NONE,
    val isMetered: Boolean = false,
    val isSimulatedOutage: Boolean = false,
    val isInternetValidated: Boolean = false,
    val networkDescription: String = "No Connection"
) {
    val isEffectivelyOnline: Boolean
        get() = isConnected && !isSimulatedOutage
}

/**
 * Monitors real-time network availability via ConnectivityManager.NetworkCallback
 * and toggles seamlessly between local Room database synchronization and cloud Firestore updates.
 * Also supports simulated network outages to test offline resilience.
 */
class NetworkConnectivityObserver(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Simulated network outage toggle for testing offline resilience and Room-first behavior
    private val _isSimulatedOutage = MutableStateFlow(false)
    val isSimulatedOutage = _isSimulatedOutage.asStateFlow()

    // Optional simulated network connectivity override (useful for testing in headless JVM environments)
    private val _simulatedConnectedOverride = MutableStateFlow<Boolean?>(null)
    val simulatedConnectedOverride = _simulatedConnectedOverride.asStateFlow()

    fun setSimulatedOutage(isOutage: Boolean) {
        _isSimulatedOutage.update { isOutage }
    }

    fun setSimulatedConnected(connected: Boolean?) {
        _simulatedConnectedOverride.update { connected }
    }

    /**
     * Inspects the active network and extracts current NetworkStatus synchronously.
     */
    fun getCurrentNetworkStatus(): NetworkStatus {
        val simulated = _isSimulatedOutage.value
        val override = _simulatedConnectedOverride.value

        if (override != null) {
            return NetworkStatus(
                isConnected = override,
                connectionType = if (override) ConnectionType.WIFI else ConnectionType.NONE,
                isMetered = false,
                isSimulatedOutage = simulated,
                isInternetValidated = override,
                networkDescription = when {
                    simulated -> "Simulated Outage (Room DB Priority)"
                    override -> "Wi-Fi (Active Test Network)"
                    else -> "Disconnected"
                }
            )
        }

        val activeNetwork = connectivityManager.activeNetwork
        if (activeNetwork == null) {
            return NetworkStatus(
                isConnected = false,
                connectionType = ConnectionType.NONE,
                isMetered = false,
                isSimulatedOutage = simulated,
                isInternetValidated = false,
                networkDescription = if (simulated) "Simulated Outage" else "Disconnected"
            )
        }

        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        if (capabilities == null) {
            return NetworkStatus(
                isConnected = false,
                connectionType = ConnectionType.NONE,
                isMetered = false,
                isSimulatedOutage = simulated,
                isInternetValidated = false,
                networkDescription = if (simulated) "Simulated Outage" else "No Capabilities"
            )
        }

        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val isNotMetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

        val type = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            hasInternet -> ConnectionType.OTHER
            else -> ConnectionType.NONE
        }

        val desc = when {
            simulated -> "Simulated Outage (Room DB Priority)"
            type == ConnectionType.WIFI -> "Wi-Fi (${if (isNotMetered) "Unmetered" else "Metered"})"
            type == ConnectionType.CELLULAR -> "Cellular Mobile Data"
            type == ConnectionType.ETHERNET -> "Ethernet Network"
            hasInternet -> "Connected"
            else -> "Disconnected"
        }

        return NetworkStatus(
            isConnected = hasInternet,
            connectionType = type,
            isMetered = !isNotMetered,
            isSimulatedOutage = simulated,
            isInternetValidated = isValidated,
            networkDescription = desc
        )
    }

    /**
     * Checks if actual physical network connectivity is active and validated.
     */
    fun isConnected(): Boolean {
        if (_isSimulatedOutage.value) return false
        val status = getCurrentNetworkStatus()
        return status.isConnected
    }

    /**
     * Returns whether the device is effectively online (connected and not under simulated outage).
     */
    fun isEffectivelyOnline(): Boolean {
        return getCurrentNetworkStatus().isEffectivelyOnline
    }

    /**
     * Flow tracking underlying physical network state via ConnectivityManager.NetworkCallback.
     */
    private fun observePhysicalNetwork(): Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                trySend(getCurrentNetworkStatus())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                trySend(getCurrentNetworkStatus())
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                super.onLosing(network, maxMsToLive)
                trySend(getCurrentNetworkStatus())
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                trySend(getCurrentNetworkStatus())
            }

            override fun onUnavailable() {
                super.onUnavailable()
                trySend(getCurrentNetworkStatus())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            // Fallback for environments where callback registration is restricted
            trySend(getCurrentNetworkStatus())
        }

        // Emit initial value immediately
        trySend(getCurrentNetworkStatus())

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Observable stream emitting comprehensive [NetworkStatus] updates,
     * reactively combining physical connectivity events, overrides, and simulated outages.
     */
    fun observeNetworkStatus(): Flow<NetworkStatus> = combine(
        observePhysicalNetwork(),
        _isSimulatedOutage,
        _simulatedConnectedOverride
    ) { physicalStatus, simulatedOutage, override ->
        if (override != null) {
            NetworkStatus(
                isConnected = override,
                connectionType = if (override) ConnectionType.WIFI else ConnectionType.NONE,
                isMetered = false,
                isSimulatedOutage = simulatedOutage,
                isInternetValidated = override,
                networkDescription = when {
                    simulatedOutage -> "Simulated Outage (Room DB Priority)"
                    override -> "Wi-Fi (Active Test Network)"
                    else -> "Disconnected"
                }
            )
        } else {
            physicalStatus.copy(
                isSimulatedOutage = simulatedOutage,
                networkDescription = if (simulatedOutage) "Simulated Outage (Room DB Priority)" else physicalStatus.networkDescription
            )
        }
    }.distinctUntilChanged()

    /**
     * Observable stream emitting boolean connectivity states (taking simulated outages into account).
     */
    fun observe(): Flow<Boolean> = observeNetworkStatus()
        .map { it.isEffectivelyOnline }
        .distinctUntilChanged()
}
