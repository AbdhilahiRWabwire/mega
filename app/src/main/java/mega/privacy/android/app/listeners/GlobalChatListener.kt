package mega.privacy.android.app.listeners

import android.content.Intent
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.EVENT_CHAT_STATUS_CHANGE
import mega.privacy.android.app.utils.Constants.EVENT_PRIVILEGES_CHANGE
import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.*

class GlobalChatListener(private val application: MegaApplication) : MegaChatListenerInterface {
    override fun onChatListItemUpdate(api: MegaChatApiJava?, item: MegaChatListItem?) {
        LiveEventBus.get(
            EVENT_PRIVILEGES_CHANGE,
            MegaChatListItem::class.java
        ).post(item)
        application.onChatListItemUpdate(api, item)
    }

    override fun onChatInitStateUpdate(api: MegaChatApiJava?, newState: Int) {
    }

    override fun onChatOnlineStatusUpdate(
        api: MegaChatApiJava?,
        userhandle: Long,
        status: Int,
        inProgress: Boolean
    ) {
        if (userhandle == api?.myUserHandle) {
            LiveEventBus.get(EVENT_CHAT_STATUS_CHANGE, Int::class.java).post(status)
        }
    }

    override fun onChatPresenceConfigUpdate(
        api: MegaChatApiJava?,
        config: MegaChatPresenceConfig?
    ) {
        if (config?.isPending == false) {
            LogUtil.logDebug("Launch local broadcast")
            val intent = Intent(Constants.BROADCAST_ACTION_INTENT_SIGNAL_PRESENCE)
            application.sendBroadcast(intent)
        }
    }

    override fun onChatConnectionStateUpdate(api: MegaChatApiJava?, chatid: Long, newState: Int) {
    }

    override fun onChatPresenceLastGreen(api: MegaChatApiJava?, userhandle: Long, lastGreen: Int) {
    }
}
