package mega.privacy.android.app.receivers

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import timber.log.Timber
import javax.inject.Inject

/**
 * Network state receiver
 */
class GlobalNetworkStateHandler @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val application: Application,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val startCameraUploadUseCase: StartCameraUploadUseCase,
) {
    init {
        applicationScope.launch {
            monitorConnectivityUseCase().collectLatest { isConnected ->
                if (isConnected) {
                    Timber.d("Network state: CONNECTED")
                    val previousIP = (application as MegaApplication).localIpAddress
                    val currentIP = Util.getLocalIpAddress(application)
                    Timber.d("Previous IP: %s", previousIP)
                    Timber.d("Current IP: %s", currentIP)
                    application.localIpAddress = currentIP
                    if (currentIP != null && currentIP.isNotEmpty() && currentIP.compareTo("127.0.0.1") != 0) {
                        if (previousIP == null || currentIP.compareTo(previousIP) != 0) {
                            Timber.d("Reconnecting...")
                            megaApi.reconnect()
                            megaChatApi.retryPendingConnections(true)
                        } else {
                            Timber.d("Retrying pending connections...")
                            megaApi.retryPendingConnections()
                            megaChatApi.retryPendingConnections(false)
                        }
                    }
                    runCatching {
                        startCameraUploadUseCase()
                    }.onFailure { Timber.e(it) }
                } else {
                    Timber.d("Network state: DISCONNECTED")
                    (application as MegaApplication).localIpAddress = null
                }
            }
        }
    }
}
