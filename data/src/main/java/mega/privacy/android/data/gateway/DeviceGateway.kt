package mega.privacy.android.data.gateway

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.BatteryInfo

/**
 * Device gateway
 *
 * @constructor Create empty Device gateway
 */
interface DeviceGateway {
    /**
     * Get manufacturer name
     */
    fun getManufacturerName(): String

    /**
     * Get device model
     */
    fun getDeviceModel(): String

    /**
     * Get current device language
     */
    fun getCurrentDeviceLanguage(): String

    /**
     * Get sdk version int
     *
     */
    suspend fun getSdkVersionInt(): Int

    /**
     * Get sdk version name
     *
     */
    suspend fun getSdkVersionName(): String

    /**
     * Get current time in millis
     *
     */
    fun getCurrentTimeInMillis(): Long

    /**
     * Elapsed realtime
     *
     */
    fun getElapsedRealtime(): Long

    /**
     * Get device memory
     *
     * @return the total device memory if available
     */
    suspend fun getDeviceMemory(): Long?

    /**
     * Get disk space bytes
     *
     * @param path
     */
    suspend fun getDiskSpaceBytes(path: String): Long

    /**
     * Based on the device locale and other preferences, check if times should be
     * formatted as 24 hour times or 12 hour (AM/PM).
     *
     * @return true if 24 hour time format is selected, false otherwise.
     */
    fun is24HourFormat(): Boolean

    /**
     * get current time
     */
    val now: Long

    /**
     * get nano time
     */
    val nanoTime: Long

    /**
     * isCharging
     * @return [Boolean] whether device is charging or not
     */
    suspend fun isCharging(): Boolean

    /**
     * Get Local Ip Address
     * @return [String]
     */
    suspend fun getLocalIpAddress(): String?

    /**
     * Get current hour of day
     *
     * @return hour of day
     */
    fun getCurrentHourOfDay(): Int


    /**
     * Get current minute
     *
     * @return minute
     */
    fun getCurrentMinute(): Int

    /**
     * Monitor battery info
     */
    val monitorBatteryInfo: Flow<BatteryInfo>

    /**
     * Monitor charging state
     */
    val monitorChargingStoppedState: Flow<Boolean>

    /**
     * Monitor thermal state
     */
    fun monitorThermalState(): Flow<Int>
}
