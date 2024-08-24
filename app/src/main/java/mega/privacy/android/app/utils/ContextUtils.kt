package mega.privacy.android.app.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.Context


object ContextUtils {

    fun Context.isValid(): Boolean =
        !(this as Activity).isFinishing && !isDestroyed

    /**
     * Return general information about the memory state of the system.
     */
    fun Context.getMemoryInfo(): ActivityManager.MemoryInfo =
        ActivityManager.MemoryInfo().also { memoryInfo ->
            (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memoryInfo)
        }

    /**
     * Get the available memory on the system
     *
     * @return  Available memory in Bytes
     */
    @JvmStatic
    fun Context.getAvailableMemory(): Long =
        getMemoryInfo().availMem

    /**
     * Return true if the system considers itself to currently be in a low
     * memory situation.
     *
     * @return  true if low memory, false otherwise
     */
    fun Context.isLowMemory(): Boolean =
        getMemoryInfo().lowMemory
}
