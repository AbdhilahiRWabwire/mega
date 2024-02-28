package mega.privacy.android.app.meeting.listeners

import android.util.Pair
import androidx.lifecycle.MutableLiveData
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.CustomCountDownTimer
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_COMPOSITION_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_ON_HOLD_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_OUTGOING_RINGING_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_SPEAK_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_STATUS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_LOCAL_AUDIO_LEVEL_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_LOCAL_AVFLAGS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_LOCAL_NETWORK_QUALITY_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_REMOTE_AUDIO_LEVEL_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_REMOTE_AVFLAGS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_RINGING_STATUS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_ON_HIRES_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_ON_HOLD_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_ON_LOWRES_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_STATUS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_CALL
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

        sendCallEvent(EVENT_UPDATE_CALL, call)

        // Call status has changed
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS)) {
            Timber.d("Call status changed, current status is ${callStatusToString(call.status)}, call id is ${call.callId}. Call is Ringing ${call.isRinging}")
            sendCallEvent(EVENT_CALL_STATUS_CHANGE, call)
            checkFirstParticipant(api, call)
            if (call.status == MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION || call.status == MegaChatCall.CALL_STATUS_DESTROYED) {
                stopCountDown()
            }
        }

        // Local audio/video flags has changed
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_LOCAL_AVFLAGS)) {
            Timber.d("Changes in local av flags. Audio enable ${call.hasLocalAudio()}, Video enable ${call.hasLocalVideo()}")
            sendCallEvent(EVENT_LOCAL_AVFLAGS_CHANGE, call)
        }

        // Peer has changed its ringing state
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_RINGING_STATUS)) {
            Timber.d("Changes in ringing status call. Call is ${call.callId}. Call is Ringing ${call.isRinging}")
            sendCallEvent(EVENT_RINGING_STATUS_CHANGE, call)
        }

        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_OUTGOING_RINGING_STOP)) {
            Timber.d("Changes in outgoing ringing")
            sendCallEvent(EVENT_CALL_OUTGOING_RINGING_CHANGE, call)
        }

        // Call composition has changed (User added or removed from call)
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_COMPOSITION) && call.callCompositionChange != 0) {
            Timber.d("Call composition changed. Call status is ${callStatusToString(call.status)}. Num of participants is ${call.numParticipants}")
            sendCallEvent(EVENT_CALL_COMPOSITION_CHANGE, call)
            stopCountDown()
        }

        // Call is set onHold
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_ON_HOLD)) {
            Timber.d("Call on hold changed")
            sendCallEvent(EVENT_CALL_ON_HOLD_CHANGE, call)
        }

        // Speak has been enabled
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_SPEAK)) {
            Timber.d("Call speak changed")
            sendCallEvent(EVENT_CALL_SPEAK_CHANGE, call)
        }

        // Indicates if we are speaking
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_AUDIO_LEVEL)) {
            Timber.d("Local audio level changed")
            sendCallEvent(EVENT_LOCAL_AUDIO_LEVEL_CHANGE, call)
        }

        // Network quality has changed
        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_NETWORK_QUALITY)) {
            Timber.d("Network quality changed")
            sendCallEvent(EVENT_LOCAL_NETWORK_QUALITY_CHANGE, call)
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
        call?.let {
            sendCallEvent(EVENT_UPDATE_CALL, it)
        }

        // Session status has changed
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_STATUS)) {
            Timber.d("Session status changed, current status is ${sessionStatusToString(session.status)}, of participant with clientID ${session.clientid}")
            sendSessionEvent(session, call)
        }

        // Remote audio/video flags has changed
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_REMOTE_AVFLAGS)) {
            Timber.d("Changes in remote av flags. Client ID  ${session.clientid}")
            sendSessionEvent(EVENT_REMOTE_AVFLAGS_CHANGE, session, callid)
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

        // Session is on hold
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_SESSION_ON_HOLD)) {
            Timber.d("Session on hold changed. Session on hold ${session.isOnHold}. Client ID  ${session.clientid}")
            sendSessionEvent(EVENT_SESSION_ON_HOLD_CHANGE, session, callid)
        }

        // Indicates if peer is speaking
        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_AUDIO_LEVEL)) {
            Timber.d("Remote audio level changed. Client ID  ${session.clientid}")
            sendSessionEvent(EVENT_REMOTE_AUDIO_LEVEL_CHANGE, session, callid)
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
