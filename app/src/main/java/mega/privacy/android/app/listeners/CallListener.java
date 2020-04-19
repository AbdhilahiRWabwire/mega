package mega.privacy.android.app.listeners;

import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import mega.privacy.android.app.MegaApplication;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatCallListenerInterface;
import nz.mega.sdk.MegaChatSession;

import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.constants.BroadcastConstants.*;

public class CallListener implements MegaChatCallListenerInterface {

    private MegaApplication megaApplication;

    public CallListener() {
        megaApplication = MegaApplication.getInstance();
    }

    @Override
    public void onChatCallUpdate(MegaChatApiJava api, MegaChatCall call) {
        if (call == null) {
            logWarning("Call null");
            return;
        }

        Intent intentGeneral = new Intent(BROADCAST_ACTION_INTENT_CALL_UPDATE);
        intentGeneral.setAction(ACTION_UPDATE_CALL);
        intentGeneral.putExtra(UPDATE_CHAT_CALL_ID, call.getChatid());
        intentGeneral.putExtra(UPDATE_CALL_ID, call.getId());
        LocalBroadcastManager.getInstance(megaApplication).sendBroadcast(intentGeneral);

        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS)) {
            int callStatus = call.getStatus();
            logDebug("Call status changed, current status is " + callStatusToString(callStatus));
            Intent intentStatus = new Intent(BROADCAST_ACTION_INTENT_CALL_UPDATE);
            intentStatus.setAction(ACTION_CALL_STATUS_UPDATE);
            intentStatus.putExtra(UPDATE_CHAT_CALL_ID, call.getChatid());
            intentStatus.putExtra(UPDATE_CALL_ID, call.getId());
            intentStatus.putExtra(UPDATE_CALL_STATUS, callStatus);
            LocalBroadcastManager.getInstance(megaApplication).sendBroadcast(intentStatus);
        }

        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_LOCAL_AVFLAGS)) {
            logDebug("Changes in local av flags ");
            Intent intentLocalFlags = new Intent(BROADCAST_ACTION_INTENT_CALL_UPDATE);
            intentLocalFlags.setAction(ACTION_CHANGE_LOCAL_AVFLAGS);
            intentLocalFlags.putExtra(UPDATE_CHAT_CALL_ID, call.getChatid());
            intentLocalFlags.putExtra(UPDATE_CALL_ID, call.getId());
            LocalBroadcastManager.getInstance(megaApplication).sendBroadcast(intentLocalFlags);
        }

        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_COMPOSITION) && call.getCallCompositionChange() != 0) {
            logDebug("Call composition changed. Call status is " + callStatusToString(call.getStatus()) + ". Num of participants is " + call.getPeeridParticipants().size());
            Intent intentComposition = new Intent(BROADCAST_ACTION_INTENT_CALL_UPDATE);
            intentComposition.setAction(ACTION_CHANGE_COMPOSITION);
            intentComposition.putExtra(UPDATE_CHAT_CALL_ID, call.getChatid());
            intentComposition.putExtra(UPDATE_CALL_ID, call.getId());
            intentComposition.putExtra(TYPE_CHANGE_COMPOSITION, call.getCallCompositionChange());
            intentComposition.putExtra(UPDATE_PEER_ID, call.getPeeridCallCompositionChange());
            intentComposition.putExtra(UPDATE_CLIENT_ID, call.getClientidCallCompositionChange());
            LocalBroadcastManager.getInstance(megaApplication).sendBroadcast(intentComposition);
        }
    }

    @Override
    public void onChatSessionUpdate(MegaChatApiJava api, long chatid, long callid, MegaChatSession session) {
        if (session == null) {
            logWarning("Session null");
            return;
        }

        Intent intentGeneral = new Intent(BROADCAST_ACTION_INTENT_SESSION_UPDATE);
        intentGeneral.setAction(ACTION_UPDATE_CALL);
        intentGeneral.putExtra(UPDATE_CHAT_CALL_ID, chatid);
        intentGeneral.putExtra(UPDATE_CALL_ID, callid);
        LocalBroadcastManager.getInstance(megaApplication).sendBroadcast(intentGeneral);

        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_REMOTE_AVFLAGS)) {
            logDebug("Changes in remote av flags ");
            Intent intentRemoteFlags = new Intent(BROADCAST_ACTION_INTENT_SESSION_UPDATE);
            intentRemoteFlags.setAction(ACTION_CHANGE_REMOTE_AVFLAGS);
            intentRemoteFlags.putExtra(UPDATE_CHAT_CALL_ID, chatid);
            intentRemoteFlags.putExtra(UPDATE_CALL_ID, callid);
            intentRemoteFlags.putExtra(UPDATE_PEER_ID, session.getPeerid());
            intentRemoteFlags.putExtra(UPDATE_CLIENT_ID, session.getClientid());
            LocalBroadcastManager.getInstance(megaApplication).sendBroadcast(intentRemoteFlags);
        }

        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_SESSION_AUDIO_LEVEL)) {
            Intent intentAudio = new Intent(BROADCAST_ACTION_INTENT_SESSION_UPDATE);
            intentAudio.setAction(ACTION_CHANGE_AUDIO_LEVEL);
            intentAudio.putExtra(UPDATE_CHAT_CALL_ID, chatid);
            intentAudio.putExtra(UPDATE_CALL_ID, callid);
            intentAudio.putExtra(UPDATE_PEER_ID, session.getPeerid());
            intentAudio.putExtra(UPDATE_CLIENT_ID, session.getClientid());
            LocalBroadcastManager.getInstance(megaApplication).sendBroadcast(intentAudio);
        }

        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_SESSION_NETWORK_QUALITY)) {
            Intent intentNetwork = new Intent(BROADCAST_ACTION_INTENT_SESSION_UPDATE);
            intentNetwork.setAction(ACTION_CHANGE_NETWORK_QUALITY);
            intentNetwork.putExtra(UPDATE_CHAT_CALL_ID, chatid);
            intentNetwork.putExtra(UPDATE_CALL_ID, callid);
            intentNetwork.putExtra(UPDATE_PEER_ID, session.getPeerid());
            intentNetwork.putExtra(UPDATE_CLIENT_ID, session.getClientid());
            LocalBroadcastManager.getInstance(megaApplication).sendBroadcast(intentNetwork);
        }

        if (session.hasChanged(MegaChatSession.CHANGE_TYPE_STATUS)) {
            logDebug("Session status changed, current status is " + sessionStatusToString(session.getStatus()));
            Intent intentStatus = new Intent(BROADCAST_ACTION_INTENT_SESSION_UPDATE);
            intentStatus.setAction(ACTION_SESSION_STATUS_UPDATE);
            intentStatus.putExtra(UPDATE_SESSION_STATUS, session.getStatus());

            if (session.getStatus() == MegaChatSession.SESSION_STATUS_DESTROYED) {
                logDebug("Term code is " + session.getTermCode());
                intentStatus.putExtra(UPDATE_SESSION_TERM_CODE, session.getTermCode());
            }

            intentStatus.putExtra(UPDATE_CHAT_CALL_ID, chatid);
            intentStatus.putExtra(UPDATE_CALL_ID, callid);
            intentStatus.putExtra(UPDATE_PEER_ID, session.getPeerid());
            intentStatus.putExtra(UPDATE_CLIENT_ID, session.getClientid());
            LocalBroadcastManager.getInstance(megaApplication).sendBroadcast(intentStatus);
        }
    }
}
