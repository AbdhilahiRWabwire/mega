package mega.privacy.android.app.lollipop.megachat.calls;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;

import mega.privacy.android.app.MegaApplication;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;

import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class CallNotificationIntentService extends IntentService implements MegaChatRequestListenerInterface {

    public static final String ANSWER = "ANSWER";
    public static final String DECLINE = "DECLINE";
    public static final String HOLD_ANSWER = "HOLD_ANSWER";
    public static final String END_ANSWER = "END_ANSWER";
    public static final String IGNORE = "IGNORE";
    public static final String HOLD_JOIN = "HOLD_JOIN";
    public static final String END_JOIN = "END_JOIN";

    MegaChatApiAndroid megaChatApi;
    MegaApiAndroid megaApi;
    MegaApplication app;

    private long chatIdIncomingCall;
    private long callIdIncomingCall = MEGACHAT_INVALID_HANDLE;

    private long chatIdCurrentCall;
    private boolean hasVideoInitialCall;

    public CallNotificationIntentService() {
        super("CallNotificationIntentService");
    }

    public void onCreate() {
        super.onCreate();

        app = (MegaApplication) getApplication();
        megaChatApi = app.getMegaChatApi();
        megaApi = app.getMegaApi();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        logDebug("onHandleIntent");
        chatIdCurrentCall = intent.getExtras().getLong(CHAT_ID_OF_CURRENT_CALL, MEGACHAT_INVALID_HANDLE);
        chatIdIncomingCall = intent.getExtras().getLong(CHAT_ID_OF_INCOMING_CALL, MEGACHAT_INVALID_HANDLE);
        hasVideoInitialCall = intent.getExtras().getBoolean(INCOMING_VIDEO_CALL, false);
        MegaChatCall incomingCall = megaChatApi.getChatCall(chatIdIncomingCall);
        if(incomingCall != null){
            callIdIncomingCall = incomingCall.getId();
            clearIncomingCallNotification(callIdIncomingCall);
        }

        final String action = intent.getAction();
        if (action == null)
            return;

        logDebug("The button clicked is : " + action);
        switch (action) {
            case ANSWER:
            case END_ANSWER:
            case END_JOIN:
                if (chatIdCurrentCall == MEGACHAT_INVALID_HANDLE) {
                    MegaChatCall call = megaChatApi.getChatCall(chatIdIncomingCall);
                    if (call != null) {
                        logDebug("Answering incoming call with status " + callStatusToString(call.getStatus()));
                        if (call.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                            addChecksForACall(chatIdIncomingCall, false);
                            megaChatApi.answerChatCall(chatIdIncomingCall, hasVideoInitialCall, this);
                        } else if (call.getStatus() == MegaChatCall.CALL_STATUS_USER_NO_PRESENT) {
                            addChecksForACall(chatIdIncomingCall, false);
                            megaChatApi.startChatCall(chatIdIncomingCall, hasVideoInitialCall, this);
                        }
                    }
                } else {
                    MegaChatCall currentCall = megaChatApi.getChatCall(chatIdCurrentCall);
                    if (currentCall == null) {
                        logDebug("Answering incoming call ...");
                        addChecksForACall(chatIdIncomingCall, false);
                        megaChatApi.answerChatCall(chatIdIncomingCall, hasVideoInitialCall, this);
                    } else {
                        logDebug("Hanging up current call ... ");
                        megaChatApi.hangChatCall(chatIdCurrentCall, this);
                    }
                }
                break;

            case DECLINE:
                logDebug("Hanging up incoming call ... ");
                megaChatApi.hangChatCall(chatIdIncomingCall, this);
                break;

            case IGNORE:
                logDebug("Ignore incoming call... ");
                megaChatApi.setIgnoredCall(chatIdIncomingCall);
                MegaApplication.getInstance().stopSounds();
                clearIncomingCallNotification(callIdIncomingCall);
                stopSelf();
                break;

            case HOLD_ANSWER:
            case HOLD_JOIN:
                MegaChatCall currentCall = megaChatApi.getChatCall(chatIdCurrentCall);
                if (currentCall == null || currentCall.isOnHold()) {
                    logDebug("Answering incoming call ...");
                    addChecksForACall(chatIdIncomingCall, false);
                    megaChatApi.answerChatCall(chatIdIncomingCall, hasVideoInitialCall, this);
                } else {
                    logDebug("Putting the current call on hold...");
                    megaChatApi.setCallOnHold(chatIdCurrentCall, true, this);
                }
                break;

            default:
                throw new IllegalArgumentException("Unsupported action: " + action);
        }
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {
    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        if (request.getType() == MegaChatRequest.TYPE_HANG_CHAT_CALL) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                if (request.getChatHandle() == chatIdIncomingCall) {
                    logDebug("Incoming call hung up. ");
                    clearIncomingCallNotification(callIdIncomingCall);
                    stopSelf();
                } else if (request.getChatHandle() == chatIdCurrentCall) {
                    logDebug("Current call hung up. Answering incoming call ...");
                    addChecksForACall(chatIdIncomingCall, false);
                    api.answerChatCall(chatIdIncomingCall, hasVideoInitialCall, this);
                }

            } else {
                logError("Error hanging up call" + e.getErrorCode());
            }

        } else if (request.getType() == MegaChatRequest.TYPE_START_CHAT_CALL) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                logDebug("Call started successfully");
            } else {
                logError("Error starting a call" + e.getErrorCode());
            }
        } else if (request.getType() == MegaChatRequest.TYPE_ANSWER_CHAT_CALL) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                logDebug("Incoming call answered.");
                MegaApplication.getPasscodeManagement().setShowPasscodeScreen(false);
                Intent i = new Intent(this, ChatCallActivity.class);
                i.putExtra(CHAT_ID, chatIdIncomingCall);
                i.setAction(SECOND_CALL);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                this.startActivity(i);
                clearIncomingCallNotification(callIdIncomingCall);
                stopSelf();
                Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                this.sendBroadcast(closeIntent);
            } else {
                logError("Error answering the call" + e.getErrorCode());
            }
        } else if (request.getType() == MegaChatRequest.TYPE_SET_CALL_ON_HOLD) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                logDebug("Current call on hold. Answering incoming call ...");
                addChecksForACall(chatIdIncomingCall, false);
                api.answerChatCall(chatIdIncomingCall, hasVideoInitialCall, this);
            } else {
                logError("Error putting the call on hold" + e.getErrorCode());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
    }

}