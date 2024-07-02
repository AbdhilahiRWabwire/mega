package mega.privacy.android.app.meeting.listeners

import android.util.Pair
import androidx.lifecycle.MutableLiveData
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.CustomCountDownTimer
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_COMPOSITION_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_ON_HIRES_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_ON_LOWRES_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_STATUS_CHANGE
import mega.privacy.android.app.data.extensions.observeOnce
import mega.privacy.android.app.utils.CallUtil.callStatusToString
import mega.privacy.android.app.utils.CallUtil.sessionStatusToString
import mega.privacy.android.app.utils.Constants.SECONDS_IN_MINUTE
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatCall.CALL_STATUS_IN_PROGRESS
import nz.mega.sdk.MegaChatCallListenerInterface
import nz.mega.sdk.MegaChatSession
import timber.log.Timber

class MeetingListener : MegaChatCallListenerInterface {

    var customCountDownTimer: CustomCountDownTimer? = null

    override fun onChatCallUpdate(api: MegaChatApiJava?, call: MegaChatCall?) {
        if (api == null || call == null) {
            Timber.w("MegaChatApiJava or call is null")
            return
        }

        if (MegaApplication.isLoggingOut) {
            Timber.w("Logging out")
            return
        }

        // Call status has changed
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS)) {
            Timber.d("Call status changed, current status is ${callStatusToString(call.status)}, call id is ${call.callId}. Call is Ringing ${call.isRinging}")
            checkFirstParticipant(api, call)
            if (call.status == MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION || call.status == MegaChatCall.CALL_STATUS_DESTROYED) {
                stopCountDown()
            }
        }

        // Call composition has changed (User added or removed from call)
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_COMPOSITION) && call.callCompositionChange != 0) {
            Timber.d("Call composition changed. Call status is ${callStatusToString(call.status)}. Num of participants is ${call.numParticipants}")
            sendCallEvent(EVENT_CALL_COMPOSITION_CHANGE, call)
            stopCountDown()
        }

    }

    override fun onChatSessionUpdate(
        api: MegaChatApiJava,
        chatid: Long,
        callid: Long,
        session: MegaChatSession?,
    ) {
        if (session == null) {
            Timber.w("Session is null")
            return
        }

        if (MegaApplication.isLoggingOut) {
            Timber.w("Logging out")
            return
        }

        val call = api.getChatCallByCallId(callid)

        // Session status has changed
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_STATUS)) {
            Timber.d("Session status changed, current status is ${sessionStatusToString(session.status)}, of participant with clientID ${session.clientid}")
            sendSessionEvent(session, call)
        }

        // Hi-Res video received
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_SESSION_ON_HIRES)) {
            Timber.d("Session on high resolution changed. Client ID  ${session.clientid}")
            sendSessionEvent(EVENT_SESSION_ON_HIRES_CHANGE, session, callid)
        }

        // Low-Res video received
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_SESSION_ON_LOWRES)) {
            Timber.d("Session on low resolution changed. Client ID  ${session.clientid}")
            sendSessionEvent(EVENT_SESSION_ON_LOWRES_CHANGE, session, callid)
        }
    }

    private fun sendCallEvent(type: String, call: MegaChatCall) {
        LiveEventBus.get(
            type,
            MegaChatCall::class.java
        ).post(call)
    }

    private fun sendSessionEvent(type: String, session: MegaChatSession, callId: Long) {
        val sessionAndCall = Pair.create(callId, session)
        LiveEventBus.get(
            type,
            Pair::class.java
        ).post(sessionAndCall)
    }

    /**
     * Method to post a LiveEventBus with MegaChatCall and MegaChatSession
     *
     * @param session MegaChatSession
     * @param call MegaChatCall
     */
    private fun sendSessionEvent(session: MegaChatSession, call: MegaChatCall?) {
        val sessionAndCall = Pair.create(call, session)
        LiveEventBus.get(
            EVENT_SESSION_STATUS_CHANGE,
            Pair::class.java
        ).post(sessionAndCall)
    }

    /**
     * Control when I am the only one on the call, no one has joined and more than 1 minute has expired
     *
     * @param call MegaChatCall
     * @param api MegaChatApiJava
     */
    private fun checkFirstParticipant(api: MegaChatApiJava, call: MegaChatCall) {
        api.getChatRoom(call.chatid)?.let { chat ->
            if (chat.isMeeting || chat.isGroup) {
                if (call.hasLocalAudio() && call.status == CALL_STATUS_IN_PROGRESS &&
                    MegaApplication.getChatManagement().isRequestSent(call.callId)
                ) {
                    stopCountDown()
                    if (customCountDownTimer == null) {
                        val timerLiveData: MutableLiveData<Boolean> = MutableLiveData()
                        customCountDownTimer = CustomCountDownTimer(timerLiveData)

                        timerLiveData.observeOnce { counterState ->
                            counterState?.let { isFinished ->
                                if (isFinished) {
                                    Timber.d("Nobody has joined the group call/meeting, muted micro")
                                    customCountDownTimer = null
                                    api.disableAudio(call.chatid, null)
                                }
                            }
                        }
                    }

                    customCountDownTimer?.start(SECONDS_IN_MINUTE)
                }
            }
        }
    }

    /**
     * Stop count down timer
     */
    private fun stopCountDown() {
        customCountDownTimer?.stop()
        customCountDownTimer = null
    }
}
