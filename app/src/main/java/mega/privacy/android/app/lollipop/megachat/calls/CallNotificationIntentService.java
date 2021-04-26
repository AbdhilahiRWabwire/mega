package mega.privacy.android.app.lollipop.megachat.calls;

import android.app.IntentService;
import android.content.Intent;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.meeting.activity.MeetingActivity;
import mega.privacy.android.app.meeting.listeners.AnswerChatCallListener;
import mega.privacy.android.app.meeting.listeners.HangChatCallListener;
import mega.privacy.android.app.meeting.listeners.StartChatCallListener;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;

import static mega.privacy.android.app.meeting.activity.MeetingActivity.MEETING_ACTION_IN;
import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class CallNotificationIntentService extends IntentService implements MegaChatRequestListenerInterface, HangChatCallListener.OnCallHungUpCallback, AnswerChatCallListener.OnCallAnsweredCallback {

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
        MegaChatCall incomingCall = megaChatApi.getChatCall(chatIdIncomingCall);
        if(incomingCall != null){
            callIdIncomingCall = incomingCall.getCallId();
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
                    if (call != null && call.getStatus() == MegaChatCall.CALL_STATUS_USER_NO_PRESENT) {
                        logDebug("Answering incoming call with status " + callStatusToString(call.getStatus()));
                        addChecksForACall(chatIdIncomingCall, false);
                        if (call.isRinging()) {
                            megaChatApi.answerChatCall(chatIdIncomingCall, false, true, new AnswerChatCallListener(this, this));
                        } else {
                            megaChatApi.startChatCall(chatIdIncomingCall, false, true, new StartChatCallListener(this));
                        }
                    }

                } else {
                    MegaChatCall currentCall = megaChatApi.getChatCall(chatIdCurrentCall);
                    if (currentCall == null) {
                        logDebug("Answering incoming call ...");
                        addChecksForACall(chatIdIncomingCall, false);
                        megaChatApi.answerChatCall(chatIdIncomingCall, false, true, new AnswerChatCallListener(this, this));
                    } else {
                        logDebug("Hanging up current call ... ");
                        megaChatApi.hangChatCall(callIdIncomingCall, new HangChatCallListener(this, this));
                    }

                }
                break;

            case DECLINE:
                logDebug("Hanging up incoming call ... ");
                megaChatApi.hangChatCall(callIdIncomingCall, new HangChatCallListener(this, this));
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
                    megaChatApi.answerChatCall(chatIdIncomingCall, false, true, new AnswerChatCallListener(this, this));
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
       if (request.getType() == MegaChatRequest.TYPE_SET_CALL_ON_HOLD) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                logDebug("Current call on hold. Answering incoming call ...");
                addChecksForACall(chatIdIncomingCall, false);
                api.answerChatCall(chatIdIncomingCall, false, true, new AnswerChatCallListener(this, this));
            } else {
                logError("Error putting the call on hold" + e.getErrorCode());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
    }

    @Override
    public void onCallHungUp(long chatId) {
        if (chatId == chatIdIncomingCall) {
            logDebug("Incoming call hung up. ");
            clearIncomingCallNotification(callIdIncomingCall);
            stopSelf();
        } else if (chatId == chatIdCurrentCall) {
            logDebug("Current call hung up. Answering incoming call ...");
            addChecksForACall(chatIdIncomingCall, false);
            megaChatApi.answerChatCall(chatIdIncomingCall, false, true, new AnswerChatCallListener(this, this));
        }
    }

    @Override
    public void onCallAnswered(long chatId, boolean falg) {
        if (chatId != chatIdIncomingCall)
            return;

        logDebug("Incoming call answered.");
        MegaApplication.getPasscodeManagement().setShowPasscodeScreen(false);
        MegaApplication.getInstance().openCallService(chatIdIncomingCall);
        Intent meetingIntent = new Intent(this, MeetingActivity.class);
        meetingIntent.setAction(MEETING_ACTION_IN);
        startActivity(meetingIntent);
        clearIncomingCallNotification(callIdIncomingCall);
        stopSelf();
        Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        this.sendBroadcast(closeIntent);
    }

    @Override
    public void onErrorAnsweredCall(int errorCode) {

    }
}