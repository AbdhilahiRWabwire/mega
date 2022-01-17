package mega.privacy.android.app.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.Observer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.data.facade.EventBusFacade
import mega.privacy.android.app.domain.entity.ConnectivityState
import mega.privacy.android.app.domain.repository.NetworkRepository
import javax.inject.Inject

class DefaultNetworkRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventBusFacade: EventBusFacade,
) : NetworkRepository {

    private val connectivityManager = getSystemService(context, ConnectivityManager::class.java)


    override fun getCurrentConnectivityState(): ConnectivityState {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getConnectivityStateSDK23()
        } else {
            getConnectivityState()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getConnectivityStateSDK23(): ConnectivityState {
        val activeNetwork =
            connectivityManager?.activeNetwork ?: return ConnectivityState.Disconnected
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return ConnectivityState.Disconnected
        return if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            ConnectivityState.Connected(
                meteredConnection = !capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_NOT_METERED
                )
            )
        } else {
            ConnectivityState.Disconnected
        }
    }

    @Suppress("DEPRECATION")
    private fun getConnectivityState(): ConnectivityState {
        return if (connectivityManager?.activeNetworkInfo?.isConnectedOrConnecting == true) {
            ConnectivityState.Connected(connectivityManager.isActiveNetworkMetered)
        } else {
            ConnectivityState.Disconnected
        }
    }

    override fun monitorConnectivityChanges(): Flow<ConnectivityState> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            monitorConnectivitySDK26()
        } else {
            monitorConnectivity()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun monitorConnectivitySDK26(): Flow<ConnectivityState> {
        return callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    super.onLost(network)
                    trySend(ConnectivityState.Disconnected)
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    trySend(
                        ConnectivityState.Connected(
                            meteredConnection = !networkCapabilities.hasCapability(
                                NetworkCapabilities.NET_CAPABILITY_NOT_METERED
                            )
                        )
                    )
                }
            }
            connectivityManager?.registerDefaultNetworkCallback(callback)

            awaitClose { connectivityManager?.unregisterNetworkCallback(callback) }
        }
    }

    private fun monitorConnectivity(): Flow<ConnectivityState> {
        return eventBusFacade.getEventFlow(
                EventConstants.EVENT_NETWORK_CHANGE,
                Boolean::class.java
            ).map { connected ->
            val metered = connectivityManager?.isActiveNetworkMetered
            if (connected && metered != null) {
                ConnectivityState.Connected(metered)
            } else {
                ConnectivityState.Disconnected
            }
        }
    }

}