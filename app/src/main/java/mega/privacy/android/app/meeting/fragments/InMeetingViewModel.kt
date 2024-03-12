package mega.privacy.android.app.meeting.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.TextureView
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_CALL
import mega.privacy.android.app.fragments.homepage.Event
import mega.privacy.android.app.listeners.EditChatRoomNameListener
import mega.privacy.android.app.listeners.GetUserEmailListener
import mega.privacy.android.app.main.listeners.CreateGroupChatWithPublicLink
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.meeting.listeners.GroupVideoListener
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.meeting.model.InMeetingUiState
import mega.privacy.android.app.usecase.call.GetCallStatusChangesUseCase
import mega.privacy.android.app.usecase.call.GetCallUseCase
import mega.privacy.android.app.usecase.call.GetNetworkChangesUseCase
import mega.privacy.android.app.usecase.call.GetParticipantsChangesUseCase
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil.getTitleChat
import mega.privacy.android.app.utils.Constants.AVATAR_CHANGE
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.NAME_CHANGE
import mega.privacy.android.app.utils.Constants.TYPE_JOIN
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.entity.meeting.AnotherCallType
import mega.privacy.android.domain.entity.meeting.CallUIStatusType
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.SubtitleCallType
import mega.privacy.android.domain.entity.statistics.EndCallEmptyCall
import mega.privacy.android.domain.entity.statistics.EndCallForAll
import mega.privacy.android.domain.entity.statistics.StayOnCallEmptyCall
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.chat.EndCallUseCase
import mega.privacy.android.domain.usecase.chat.link.JoinPublicChatUseCase
import mega.privacy.android.domain.usecase.login.ChatLogoutUseCase
import mega.privacy.android.domain.usecase.meeting.BroadcastCallEndedUseCase
import mega.privacy.android.domain.usecase.meeting.EnableAudioLevelMonitorUseCase
import mega.privacy.android.domain.usecase.meeting.GetChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.HangChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.IsAudioLevelMonitorEnabledUseCase
import mega.privacy.android.domain.usecase.meeting.JoinMeetingAsGuestUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.RequestHighResolutionVideoUseCase
import mega.privacy.android.domain.usecase.meeting.RequestLowResolutionVideoUseCase
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.meeting.StartChatCall
import mega.privacy.android.domain.usecase.meeting.StopHighResolutionVideoUseCase
import mega.privacy.android.domain.usecase.meeting.StopLowResolutionVideoUseCase
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatCall.CALL_STATUS_CONNECTING
import nz.mega.sdk.MegaChatCall.CALL_STATUS_IN_PROGRESS
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaChatRoom.PRIV_MODERATOR
import nz.mega.sdk.MegaChatSession
import nz.mega.sdk.MegaChatVideoListenerInterface
import nz.mega.sdk.MegaHandleList
import org.jetbrains.anko.defaultSharedPreferences
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * InMeetingFragment view model.
 *
 * @property inMeetingRepository                [InMeetingRepository]
 * @property getCallUseCase                     [GetCallUseCase]
 * @property startChatCall                      [StartChatCall]
 * @property getNetworkChangesUseCase           [GetNetworkChangesUseCase]
 * @property getCallStatusChangesUseCase        [GetCallStatusChangesUseCase]
 * @property endCallUseCase                     [EndCallUseCase]
 * @property getParticipantsChangesUseCase      [GetParticipantsChangesUseCase]
 * @property rtcAudioManagerGateway             [RTCAudioManagerGateway]
 * @property setChatVideoInDeviceUseCase        [SetChatVideoInDeviceUseCase]
 * @property megaChatApiGateway                 [MegaChatApiGateway]
 * @property passcodeManagement                 [PasscodeManagement]
 * @property chatManagement                     [ChatManagement]
 * @property sendStatisticsMeetingsUseCase      [SendStatisticsMeetingsUseCase]
 * @property enableAudioLevelMonitorUseCase     [EnableAudioLevelMonitorUseCase]
 * @property isAudioLevelMonitorEnabledUseCase  [IsAudioLevelMonitorEnabledUseCase]
 * @property requestHighResolutionVideoUseCase  [RequestHighResolutionVideoUseCase]
 * @property requestLowResolutionVideoUseCase   [RequestLowResolutionVideoUseCase]
 * @property stopHighResolutionVideoUseCase     [StopHighResolutionVideoUseCase]
 * @property stopLowResolutionVideoUseCase      [StopLowResolutionVideoUseCase]
 * @property getChatCallUseCase                 [GetChatCallUseCase]
 * @property getChatRoomUseCase                 [GetChatRoomUseCase]
 * @property monitorChatRoomUpdates             [MonitorChatRoomUpdates]
 * @property joinPublicChatUseCase              [JoinPublicChatUseCase]
 * @property joinMeetingAsGuestUseCase          [JoinMeetingAsGuestUseCase]
 * @property chatLogoutUseCase                  [ChatLogoutUseCase]
 * @property state                              Current view state as [InMeetingUiState]
 * @property context                            Application context
 */
@HiltViewModel
@SuppressLint("StaticFieldLeak")
class InMeetingViewModel @Inject constructor(
    private val inMeetingRepository: InMeetingRepository,
    private val getCallUseCase: GetCallUseCase,
    private val startChatCall: StartChatCall,
    private val getNetworkChangesUseCase: GetNetworkChangesUseCase,
    private val getCallStatusChangesUseCase: GetCallStatusChangesUseCase,
    private val endCallUseCase: EndCallUseCase,
    private val getParticipantsChangesUseCase: GetParticipantsChangesUseCase,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase,
    private val megaChatApiGateway: MegaChatApiGateway,
    private val passcodeManagement: PasscodeManagement,
    private val chatManagement: ChatManagement,
    private val sendStatisticsMeetingsUseCase: SendStatisticsMeetingsUseCase,
    private val enableAudioLevelMonitorUseCase: EnableAudioLevelMonitorUseCase,
    private val isAudioLevelMonitorEnabledUseCase: IsAudioLevelMonitorEnabledUseCase,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val monitorChatRoomUpdates: MonitorChatRoomUpdates,
    private val requestHighResolutionVideoUseCase: RequestHighResolutionVideoUseCase,
    private val requestLowResolutionVideoUseCase: RequestLowResolutionVideoUseCase,
    private val stopHighResolutionVideoUseCase: StopHighResolutionVideoUseCase,
    private val stopLowResolutionVideoUseCase: StopLowResolutionVideoUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val broadcastCallEndedUseCase: BroadcastCallEndedUseCase,
    private val hangChatCallUseCase: HangChatCallUseCase,
    private val joinMeetingAsGuestUseCase: JoinMeetingAsGuestUseCase,
    private val joinPublicChatUseCase: JoinPublicChatUseCase,
    private val chatLogoutUseCase: ChatLogoutUseCase,
    @ApplicationContext private val context: Context,
) : BaseRxViewModel(), EditChatRoomNameListener.OnEditedChatRoomNameCallback,
    GetUserEmailListener.OnUserEmailUpdateCallback {

    /**
     * private UI state
     */
    private val _state = MutableStateFlow(InMeetingUiState())

    /**
     * public UI State
     */
    val state = _state.asStateFlow()

    private var anotherCallInProgressDisposable: Disposable? = null
    private var networkQualityDisposable: Disposable? = null
    private var reconnectingDisposable: Disposable? = null

    private val _pinItemEvent = MutableLiveData<Event<Participant>>()
    val pinItemEvent: LiveData<Event<Participant>> = _pinItemEvent

    private var meetingLeftTimerJob: Job? = null

    /**
     * Participant selected
     *
     * @param participant [Participant]
     */
    fun onItemClick(participant: Participant) {
        onSnackbarMessageConsumed()

        _pinItemEvent.value = Event(participant)
        getSession(participant.clientId)?.let {
            if (it.hasScreenShare() && _state.value.callUIStatus == CallUIStatusType.SpeakerView) {
                if (!participant.isScreenShared) {
                    _state.update { state ->
                        state.copy(
                            snackbarMessage = triggered(R.string.meetings_meeting_screen_main_view_participant_is_sharing_screen_warning),
                        )
                    }
                }

                Timber.d("Participant clicked: $participant")
                sortParticipantsListForSpeakerView(participant)
            }
        }
    }

    /**
     * Pin speaker and sort the participants list
     *
     * @param participant   [Participant]
     */
    private fun pinSpeaker(participant: Participant) {
        _pinItemEvent.value = Event(participant)
        sortParticipantsListForSpeakerView(participant)
    }

    /**
     * Chat participant select to be in speaker view
     *
     * @param chatParticipant [ChatParticipant]
     */
    fun onItemClick(chatParticipant: ChatParticipant) =
        participants.value?.find { it.peerId == chatParticipant.handle }?.let {
            onItemClick(it)
        }

    // Meeting
    private val _callLiveData = MutableLiveData<MegaChatCall?>(null)
    val callLiveData: LiveData<MegaChatCall?> = _callLiveData

    // Call ID
    private val _updateCallId = MutableStateFlow(MEGACHAT_INVALID_HANDLE)
    val updateCallId: StateFlow<Long> get() = _updateCallId

    private val _showPoorConnectionBanner = MutableStateFlow(false)
    val showPoorConnectionBanner: StateFlow<Boolean> get() = _showPoorConnectionBanner

    private val _showReconnectingBanner = MutableStateFlow(false)
    val showReconnectingBanner: StateFlow<Boolean> get() = _showReconnectingBanner

    private val _showOnlyMeBanner = MutableStateFlow(false)
    val showOnlyMeBanner: StateFlow<Boolean> get() = _showOnlyMeBanner

    private val _showWaitingForOthersBanner = MutableStateFlow(false)
    val showWaitingForOthersBanner: StateFlow<Boolean> get() = _showWaitingForOthersBanner

    // List of participants in the meeting
    val participants: MutableLiveData<MutableList<Participant>> = MutableLiveData(mutableListOf())

    // List of speaker participants in the meeting
    val speakerParticipants: MutableLiveData<MutableList<Participant>> =
        MutableLiveData(mutableListOf())

    // List of visible participants in the meeting
    var visibleParticipants: MutableList<Participant> = mutableListOf()

    private val _getParticipantsChanges =
        MutableStateFlow<Pair<Int, ((Context) -> String)?>>(Pair(TYPE_JOIN, null))
    val getParticipantsChanges: StateFlow<Pair<Int, ((Context) -> String)?>> get() = _getParticipantsChanges

    private val updateCallObserver =
        Observer<MegaChatCall> {
            if (isSameChatRoom(it.chatid)) {
                _callLiveData.value = it
            }
        }

    private val noOutgoingCallObserver = Observer<Long> {
        _state.value.call?.apply {
            if (it == chatId) {
                status?.let {
                    checkSubtitleToolbar()
                }
            }
        }
    }

    private val waitingForOthersBannerObserver =
        Observer<Pair<Long, Boolean>> { result ->
            val chatId: Long = result.first
            val onlyMeInTheCall: Boolean = result.second
            if (_state.value.currentChatId == chatId) {
                if (onlyMeInTheCall) {
                    _showWaitingForOthersBanner.value = false
                    if (!MegaApplication.getChatManagement().hasEndCallDialogBeenIgnored) {
                        _showOnlyMeBanner.value = true
                    }
                }
            }
        }

    init {
        startMonitorChatRoomUpdates()
        startMonitorChatCallUpdates()

        getParticipantsChangesUseCase.getChangesFromParticipants()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { (chatId, typeChange, peers) ->
                    if (_state.value.currentChatId == chatId) {
                        getChat()?.let { chat ->
                            if (chat.isMeeting || chat.isGroup) {
                                peers?.let { list ->
                                    getParticipantChanges(list, typeChange)
                                }
                            }
                        }
                    }
                },
                onError = Timber::e
            )
            .addTo(composite)

        getParticipantsChangesUseCase.checkIfIAmAloneOnAnyCall()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { (chatId, onlyMeInTheCall, waitingForOthers, isReceivedChange) ->
                    if (_state.value.currentChatId == chatId) {
                        val millisecondsOnlyMeInCallDialog =
                            TimeUnit.MILLISECONDS.toSeconds(MegaApplication.getChatManagement().millisecondsOnlyMeInCallDialog)

                        if (onlyMeInTheCall) {
                            hideBottomPanels()
                            if (waitingForOthers && millisecondsOnlyMeInCallDialog <= 0) {
                                _showOnlyMeBanner.value = false
                                _showWaitingForOthersBanner.value = true
                            } else {
                                _showWaitingForOthersBanner.value = false
                                if (waitingForOthers || !isReceivedChange) {
                                    _showOnlyMeBanner.value = true
                                }
                            }
                        } else {
                            _showWaitingForOthersBanner.value = false
                            _showOnlyMeBanner.value = false
                        }
                    }
                },
                onError = Timber::e
            )
            .addTo(composite)

        LiveEventBus.get(EVENT_UPDATE_CALL, MegaChatCall::class.java)
            .observeForever(updateCallObserver)

        LiveEventBus.get(EventConstants.EVENT_NOT_OUTGOING_CALL, Long::class.java)
            .observeForever(noOutgoingCallObserver)

        LiveEventBus.get<Pair<Long, Boolean>>(EventConstants.EVENT_UPDATE_WAITING_FOR_OTHERS)
            .observeForever(waitingForOthersBannerObserver)
    }

    /**
     * Get chat room
     */
    private fun getChatRoom() {
        viewModelScope.launch {
            runCatching {
                getChatRoomUseCase(_state.value.currentChatId)
            }.onSuccess { chatRoom ->
                chatRoom?.let { chat ->
                    _state.update { state ->
                        state.copy(
                            chatTitle = chat.title,
                            isOpenInvite = chat.isOpenInvite,
                            isOneToOneCall = !chat.isGroup && !chat.isMeeting,
                            isMeeting = chat.isMeeting,
                            isPublicChat = chat.isPublic,
                        )
                    }
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Get chat call
     */
    private fun getChatCall() {
        viewModelScope.launch {
            runCatching {
                getChatCallUseCase(_state.value.currentChatId)
            }.onSuccess { chatCall ->
                chatCall?.let { call ->
                    Timber.d("Call id ${call.callId} and chat id ${call.chatId}")
                    Timber.d("Call limit ${call.callDurationLimit} Call Duration ${call.duration} and initial timestamp ${call.initialTimestamp}")
                    _state.update { it.copy(call = call) }
                    call.status?.let { status ->
                        checkSubtitleToolbar()
                        setCall(_state.value.currentChatId)
                        if (status != ChatCallStatus.Initial && _state.value.previousState == ChatCallStatus.Initial) {
                            _state.update {
                                it.copy(
                                    previousState = status,
                                )
                            }
                        }
                    }
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Get chat room updates
     */
    private fun startMonitorChatRoomUpdates() =
        viewModelScope.launch {
            monitorChatRoomUpdates(_state.value.currentChatId).collectLatest { chat ->
                if (chat.hasChanged(ChatRoomChange.Title)) {
                    Timber.d("Changes in chat title")
                    _state.update { state ->
                        state.copy(
                            chatTitle = chat.title,
                        )
                    }
                }
                if (chat.hasChanged(ChatRoomChange.OpenInvite)) {
                    _state.update {
                        it.copy(isOpenInvite = chat.isOpenInvite)
                    }
                }
            }
        }

    /**
     * Get chat call updates
     */
    private fun startMonitorChatCallUpdates() =
        viewModelScope.launch {
            monitorChatCallUpdatesUseCase()
                .filter { it.chatId == _state.value.currentChatId }
                .collectLatest { call ->
                    _state.update { it.copy(call = call) }
                    checkSubtitleToolbar()
                    call.changes?.apply {
                        Timber.d("Changes in call $this")
                        if (contains(ChatCallChanges.Status)) {
                            Timber.d("Call status changed ${call.status}")
                            call.status?.let { status ->
                                checkSubtitleToolbar()
                                _state.update { state ->
                                    state.copy(
                                        previousState = status,
                                    )
                                }
                            }
                        } else if (contains(ChatCallChanges.CallWillEnd)) {
                            handleFreeCallEndWarning(call)
                        }
                    }
                }
        }

    /**
     * Cancel the timer when call is upgraded or start the end timer if the call is free
     */
    private fun handleFreeCallEndWarning(call: ChatCall) {
        if (call.num == -1) {
            Timber.d("Cancelling Meeting Timer Job")
            // CALL_LIMIT_DURATION_DISABLED
            // to do stop the call duration warning
            meetingLeftTimerJob?.cancel()
            _state.update {
                it.copy(
                    minutesToEndMeeting = null,
                    showMeetingEndWarningDialog = false
                )
            }

        } else {
            // to do show the call duration warning
            Timber.d("Call will end in ${call.num} seconds")
            call.num?.let {
                startMeetingEndWarningTimer(it / 60)
            }
            if (call.isOwnModerator) {
                _state.update {
                    it.copy(showMeetingEndWarningDialog = true)
                }
            }
        }
    }

    private fun startMeetingEndWarningTimer(minutes: Int) {
        meetingLeftTimerJob = viewModelScope.launch {
            (minutes downTo 0).forEach { minute ->
                Timber.d("Meeting will end in $minute minutes")
                _state.update {
                    it.copy(minutesToEndMeeting = minute)
                }
                delay(TimeUnit.MINUTES.toMillis(1))
            }
            _state.update {
                it.copy(minutesToEndMeeting = null)
            }
            meetingLeftTimerJob = null
        }
    }

    /**
     * set showMeetingEndWarningDialog to false when dialog is shown
     */
    fun onMeetingEndWarningDialogDismissed() {
        _state.update {
            it.copy(showMeetingEndWarningDialog = false)
        }
    }

    /**
     * Show meeting info dialog
     *
     * @param shouldShowDialog True,show dialog.
     */
    fun onToolbarTap(shouldShowDialog: Boolean) =
        _state.update {
            it.copy(showMeetingInfoFragment = !it.isOneToOneCall && shouldShowDialog)
        }

    /**
     * Method to check if only me dialog and the call will end banner should be displayed.
     */
    fun checkShowOnlyMeBanner() {
        if (isOneToOneCall())
            return

        _callLiveData.value?.let { call ->
            getParticipantsChangesUseCase.checkIfIAmAloneOnSpecificCall(call).let { result ->
                if (result.onlyMeInTheCall) {
                    if (_showOnlyMeBanner.value) {
                        _showOnlyMeBanner.value = false
                    }

                    _showWaitingForOthersBanner.value = false
                    _showOnlyMeBanner.value = true
                }
            }
        }
    }

    /**
     * Method to get right text to display on the banner
     *
     * @param list List of participants with changes
     * @param type Type of change
     */
    private fun getParticipantChanges(list: ArrayList<Long>, type: Int) {
        val action = when (val numParticipants = list.size) {
            1 -> { context: Context ->
                context.getString(
                    if (type == TYPE_JOIN)
                        R.string.meeting_call_screen_one_participant_joined_call
                    else
                        R.string.meeting_call_screen_one_participant_left_call,
                    getParticipantFullName(list[0])
                )
            }

            2 -> { context: Context ->
                context.getString(
                    if (type == TYPE_JOIN)
                        R.string.meeting_call_screen_two_participants_joined_call
                    else
                        R.string.meeting_call_screen_two_participants_left_call,
                    getParticipantFullName(list[0]), getParticipantFullName(list[1])
                )
            }

            else -> { context: Context ->
                context.resources.getQuantityString(
                    if (type == TYPE_JOIN) R.plurals.meeting_call_screen_more_than_two_participants_joined_call
                    else
                        R.plurals.meeting_call_screen_more_than_two_participants_left_call,
                    numParticipants,
                    getParticipantFullName(list[0]),
                    (numParticipants - 1)
                )
            }
        }

        _getParticipantsChanges.value = Pair(type, action)
    }

    /**
     * Method to check the subtitle in the toolbar
     */
    private fun checkSubtitleToolbar() =
        _state.value.call?.apply {
            when (status) {
                ChatCallStatus.Connecting -> _state.update { state ->
                    state.copy(
                        showCallDuration = false,
                        updateCallSubtitle = SubtitleCallType.Connecting
                    )
                }

                ChatCallStatus.InProgress -> {
                    val isCalling = !_state.value.isMeeting && isRequestSent() && this.isOutgoing
                    _state.update { state ->
                        state.copy(
                            showCallDuration = !isCalling,
                            updateCallSubtitle = if (isCalling) SubtitleCallType.Calling else SubtitleCallType.Established
                        )
                    }
                }

                else -> {}
            }
        }

    /**
     * Method to get the duration of the call
     */
    fun getCallDuration(): Long = getCall()?.duration ?: INVALID_VALUE.toLong()

    /**
     * Method that controls whether the another call banner should be visible or not
     */
    private fun checkAnotherCallBanner() {
        anotherCallInProgressDisposable?.dispose()

        anotherCallInProgressDisposable =
            getCallUseCase.checkAnotherCall(_state.value.currentChatId).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribeBy(
                    onNext = {
                        if (it == MEGACHAT_INVALID_HANDLE) {
                            _state.update { state ->
                                state.copy(
                                    updateAnotherCallBannerType = AnotherCallType.NotCall,
                                )
                            }
                        } else {
                            val call: MegaChatCall =
                                getCallUseCase.getMegaChatCall(it).blockingGet()
                            if (call.isOnHold && _state.value.updateAnotherCallBannerType != AnotherCallType.CallOnHold) {
                                _state.update { state ->
                                    state.copy(
                                        updateAnotherCallBannerType = AnotherCallType.CallOnHold,
                                    )
                                }

                            } else if (!call.isOnHold && _state.value.updateAnotherCallBannerType != AnotherCallType.CallInProgress) {
                                _state.update { state ->
                                    state.copy(
                                        updateAnotherCallBannerType = AnotherCallType.CallInProgress,
                                    )
                                }
                            }

                            inMeetingRepository.getChatRoom(it)?.let { chat ->
                                _state.update { state ->
                                    state.copy(
                                        anotherChatTitle = getTitleChat(chat),
                                    )
                                }
                            }
                        }
                    }, onError = Timber::e
                ).addTo(composite)
    }

    /**
     * Control when Stay on call option is chosen
     */
    fun checkStayCall() {
        MegaApplication.getChatManagement().stopCounterToFinishCall()
        MegaApplication.getChatManagement().hasEndCallDialogBeenIgnored = true
        if (_showOnlyMeBanner.value) {
            _showOnlyMeBanner.value = false
            _showWaitingForOthersBanner.value = true
        }

        viewModelScope.launch {
            kotlin.runCatching {
                sendStatisticsMeetingsUseCase(StayOnCallEmptyCall())
            }
        }
    }

    /**
     * Control when End call now option is chosen
     */
    fun checkEndCall() {
        MegaApplication.getChatManagement().stopCounterToFinishCall()
        _showOnlyMeBanner.value = false
        _showWaitingForOthersBanner.value = false
        hangCall()

        viewModelScope.launch {
            kotlin.runCatching {
                sendStatisticsMeetingsUseCase(EndCallEmptyCall())
            }
        }
    }

    /**
     * Start the counter to end the call after the previous banner has been hidden
     */
    fun startCounterTimerAfterBanner() {
        MegaApplication.getChatManagement().stopCounterToFinishCall()
        MegaApplication.getChatManagement()
            .startCounterToFinishCall(_state.value.currentChatId)
    }

    /**
     * Method to check if a info banner should be displayed
     */
    fun checkBannerInfo() {
        if (_showPoorConnectionBanner.value) {
            _showPoorConnectionBanner.value = false
            _showPoorConnectionBanner.value = true
        }

        if (_showReconnectingBanner.value) {
            _showReconnectingBanner.value = false
            _showReconnectingBanner.value = true
        }
    }

    /**
     * Method that controls whether to display the bad connection banner
     */
    private fun checkNetworkQualityChanges() {
        networkQualityDisposable?.dispose()
        networkQualityDisposable = getNetworkChangesUseCase.get(_state.value.currentChatId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = {
                    _showPoorConnectionBanner.value =
                        it == GetNetworkChangesUseCase.NetworkQuality.NETWORK_QUALITY_BAD
                },
                onError = Timber::e
            ).addTo(composite)
    }

    /**
     * Method that controls whether to display the reconnecting banner
     */
    private fun checkReconnectingChanges() {
        reconnectingDisposable?.dispose()
        reconnectingDisposable =
            getCallStatusChangesUseCase.getReconnectingStatus(_state.value.currentChatId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = {
                        _showReconnectingBanner.value = it
                    },
                    onError = Timber::e
                ).addTo(composite)
    }

    /**
     * Method to know if it is the same chat
     *
     * @param chatId chat ID
     * @return True, if it is the same. False, otherwise
     */
    fun isSameChatRoom(chatId: Long): Boolean =
        chatId != MEGACHAT_INVALID_HANDLE && _state.value.currentChatId == chatId

    /**
     * Method to know if it is the same call
     *
     * @param callId call ID
     * @return True, if it is the same. False, otherwise
     */
    fun isSameCall(callId: Long): Boolean =
        _callLiveData.value?.let { it.callId == callId } ?: run { false }

    /**
     * Method to set a call
     *
     * @param chatId chat ID
     */
    fun setCall(chatId: Long) {
        if (isSameChatRoom(chatId)) {
            _callLiveData.value = inMeetingRepository.getMeeting(chatId)
            _callLiveData.value?.let {
                if (_updateCallId.value != it.callId) {
                    _updateCallId.value = it.callId
                    checkAnotherCallBanner()
                    checkParticipantsList()
                    checkNetworkQualityChanges()
                    checkReconnectingChanges()
                    updateMeetingInfoBottomPanel()
                }
            }
        }
    }

    /**
     * Method to get a call
     *
     * @return MegaChatCall
     */
    fun getCall(): MegaChatCall? =
        if (_state.value.currentChatId == MEGACHAT_INVALID_HANDLE) null
        else inMeetingRepository.getChatRoom(_state.value.currentChatId)
            ?.let { inMeetingRepository.getMeeting(it.chatId) }

    /**
     * If it's just me on the call
     *
     * @param chatId chat ID
     * @return True, if it's just me on the call. False, if there are more participants
     */
    fun amIAloneOnTheCall(chatId: Long): Boolean {
        if (isSameChatRoom(chatId)) {
            inMeetingRepository.getMeeting(_state.value.currentChatId)?.let { call ->
                val sessionsInTheCall: MegaHandleList? = call.sessionsClientid
                if (sessionsInTheCall != null && sessionsInTheCall.size() > 0) {
                    Timber.d("I am not the only participant in the call, num of session in the call is ${sessionsInTheCall.size()}")
                    return false
                }

                Timber.d("I am the only participant in the call")
                return true
            }
        }

        Timber.d("I am not the only participant in the call")
        return false
    }

    /**
     * Method to get a chat
     *
     * @return MegaChatRoom
     */
    fun getChat(): MegaChatRoom? = inMeetingRepository.getChatRoom(_state.value.currentChatId)

    /**
     * Method to set a chat
     *
     * @param newChatId chat ID
     */
    fun setChatId(newChatId: Long) {
        if (newChatId == MEGACHAT_INVALID_HANDLE || _state.value.currentChatId == newChatId)
            return

        _state.update { state ->
            state.copy(
                currentChatId = newChatId,
            )
        }
        getChatRoom()
        getChatCall()
        enableAudioLevelMonitor(_state.value.currentChatId)
    }

    /**
     * Enable audio level monitor
     *
     * @param chatId MegaChatHandle of the chat room where enable audio level monitor
     */
    fun enableAudioLevelMonitor(chatId: Long) {
        viewModelScope.launch {
            if (!isAudioLevelMonitorEnabledUseCase(chatId)) {
                enableAudioLevelMonitorUseCase(true, chatId)
            }
        }
    }

    /**
     * Get the chat ID of the current meeting
     *
     * @return chat ID
     */
    fun getChatId(): Long = _state.value.currentChatId

    /**
     *  Method to know if it is a one-to-one chat call
     *
     *  @return True, if it is a one-to-one chat call. False, otherwise
     */
    fun isOneToOneCall(): Boolean = inMeetingRepository.getChatRoom(_state.value.currentChatId)
        ?.let { (!it.isGroup && !it.isMeeting) } ?: run { false }

    /**
     * Set speaker selection automatic or manual
     *
     * @param isAutomatic True, if it's automatic. False, if it's manual
     */
    fun setSpeakerSelection(isAutomatic: Boolean) =
        _state.update {
            it.copy(
                isSpeakerSelectionAutomatic = isAutomatic,
            )
        }

    /**
     * Method to know if it's me
     *
     * @param peerId User handle of a participant
     * @return True, if it's me. False, otherwise
     */
    fun isMe(peerId: Long?): Boolean = inMeetingRepository.isMe(peerId)

    /**
     * Get the session of a participant
     *
     * @param clientId client ID of a participant
     * @return MegaChatSession of a participant
     */
    fun getSession(clientId: Long): MegaChatSession? =
        if (clientId != MEGACHAT_INVALID_HANDLE) _callLiveData.value?.getMegaChatSession(clientId)
        else null

    /**
     * Method to know if a one-to-one call is audio only
     *
     * @return True, if it's audio call. False, otherwise
     */
    fun isAudioCall(): Boolean {
        _callLiveData.value?.let { call ->
            if (call.isOnHold) {
                return true
            }

            val session = getSessionOneToOneCall(call)
            session?.let { sessionParticipant ->
                if (sessionParticipant.isOnHold || (!call.hasLocalVideo() && !MegaApplication.getChatManagement()
                        .getVideoStatus(call.chatid) && !sessionParticipant.hasVideo())
                ) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Method to know if a call is in progress status
     *
     * @return True, if the chas is in progress. False, otherwise.
     */
    fun isCallEstablished(): Boolean =
        _callLiveData.value?.let { (it.status == CALL_STATUS_IN_PROGRESS) }
            ?: run { false }

    /**
     * Method to know if a call is on hold
     *
     * @return True, if is on hold. False, otherwise
     */
    fun isCallOnHold(): Boolean = _callLiveData.value?.isOnHold ?: false

    /**
     * Method to know if a call or session is on hold in meeting
     *
     * @return True, if is on hold. False, otherwise
     */
    fun isCallOrSessionOnHold(clientId: Long): Boolean =
        if (isCallOnHold()) true
        else getSession(clientId)?.isOnHold ?: false

    /**
     * Method to know if a call or session is on hold in one to one call
     *
     * @return True, if is on hold. False, otherwise
     */
    fun isCallOrSessionOnHoldOfOneToOneCall(): Boolean =
        if (isCallOnHold()) true else isSessionOnHoldOfOneToOneCall()

    /**
     * Control when join a meeting as a guest
     *
     * @param meetingLink   Meeting link
     * @param firstName     Guest first name
     * @param lastName      Guest last name
     */
    fun joinMeetingAsGuest(meetingLink: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            runCatching {
                joinMeetingAsGuestUseCase(meetingLink, firstName, lastName)
            }.onSuccess {
                chatManagement
                    .setOpeningMeetingLink(
                        state.value.currentChatId,
                        true
                    )
                autoJoinPublicChat()

            }.onFailure { exception ->
                Timber.e(exception)
                chatLogout()
            }
        }
    }

    /**
     * Chat logout
     */
    private fun chatLogout() {
        viewModelScope.launch {
            runCatching {
                chatLogoutUseCase()
            }.onSuccess {
                _state.update {
                    it.copy(
                        shouldFinish = true,
                    )
                }
            }.onFailure { exception ->
                Timber.e(exception)
                _state.update {
                    it.copy(
                        shouldFinish = true,
                    )
                }
            }
        }
    }

    /**
     * Auto join public chat
     */
    private fun autoJoinPublicChat() {
        if (!chatManagement.isAlreadyJoining(state.value.currentChatId)) {
            chatManagement.addJoiningChatId(state.value.currentChatId)
            viewModelScope.launch {
                runCatching {
                    joinPublicChatUseCase(state.value.currentChatId)
                }.onSuccess {
                    chatManagement.removeJoiningChatId(state.value.currentChatId)
                    chatManagement.broadcastJoinedSuccessfully()
                    _state.update {
                        it.copy(
                            joinedAsGuest = true,
                        )
                    }
                }.onFailure { exception ->
                    Timber.e(exception)
                    chatManagement.removeJoiningChatId(state.value.currentChatId)
                    _state.update {
                        it.copy(
                            shouldFinish = true,
                        )
                    }
                }
            }
        }
    }

    /**
     * Sets joinedAsGuest in state as consumed.
     */
    fun onJoinedAsGuestConsumed() = _state.update {
        it.copy(
            joinedAsGuest = false,
        )
    }

    /**
     * Method to know if a session is on hold in one to one call
     *
     * @return True, if is on hold. False, otherwise
     */
    private fun isSessionOnHoldOfOneToOneCall(): Boolean {
        _callLiveData.value?.let { call ->
            if (isOneToOneCall()) {
                val session = inMeetingRepository.getSessionOneToOneCall(call)
                session?.let {
                    return it.isOnHold
                }
            }
        }

        return false
    }

    /**
     * Method to obtain a specific call
     *
     * @param chatId Chat ID
     * @return MegaChatCall the another call
     */
    private fun getAnotherCall(chatId: Long): MegaChatCall? =
        if (chatId == MEGACHAT_INVALID_HANDLE) null else inMeetingRepository.getMeeting(chatId)

    /**
     * Method to know if exists another call in progress or on hold.
     *
     * @return MegaChatCall the another call
     */
    fun getAnotherCall(): MegaChatCall? {
        val anotherCallChatId = CallUtil.getAnotherCallParticipating(_state.value.currentChatId)
        if (anotherCallChatId != MEGACHAT_INVALID_HANDLE) {
            val anotherCall = inMeetingRepository.getMeeting(anotherCallChatId)
            anotherCall?.let {
                if (isCallOnHold() && !it.isOnHold) {
                    Timber.d("This call in on hold, another call in progress")
                    return anotherCall
                }

                if (!isCallOnHold() && it.isOnHold) {
                    Timber.d("This call in progress, another call on hold")
                    return anotherCall
                }
            }

        }

        Timber.d("No other calls in progress or on hold")
        return null
    }

    /**
     * Get session of a contact in a one-to-one call
     *
     * @param callChat MegaChatCall
     */
    fun getSessionOneToOneCall(callChat: MegaChatCall?): MegaChatSession? =
        callChat?.getMegaChatSession(callChat.sessionsClientid[0])

    /**
     * Method to obtain the full name of a participant
     *
     * @param peerId User handle of a participant
     * @return The name of a participant
     */
    fun getParticipantFullName(peerId: Long): String =
        CallUtil.getUserNameCall(MegaApplication.getInstance().applicationContext, peerId)

    /**
     * Method to find out if there is a participant in the call
     *
     * @param peerId Use handle of a participant
     * @param typeChange the type of change, name or avatar
     * @return list of participants with changes
     */
    fun updateParticipantsNameOrAvatar(
        peerId: Long,
        typeChange: Int,
        context: Context,
    ): MutableSet<Participant> {
        val listWithChanges = mutableSetOf<Participant>()
        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
            participants.value = participants.value?.map { participant ->
                return@map when {
                    participant.peerId == peerId && typeChange == NAME_CHANGE -> {
                        listWithChanges.add(participant)
                        participant.copy(
                            name = getParticipantFullName(peerId),
                            avatar = getAvatarBitmap(peerId)
                        )
                    }

                    participant.peerId == peerId && typeChange == AVATAR_CHANGE -> {
                        listWithChanges.add(participant)
                        participant.copy(avatar = getAvatarBitmap(peerId))
                    }

                    else -> participant
                }
            }?.toMutableList()
            updateMeetingInfoBottomPanel()
        }
        return listWithChanges
    }

    /**
     * Method that makes the necessary changes to the participant list when my own privileges have changed.
     */
    fun updateOwnPrivileges(context: Context) {
        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
            participants.value = participants.value?.map { participant ->
                return@map participant.copy(
                    hasOptionsAllowed = shouldParticipantsOptionBeVisible(
                        participant.isMe,
                        participant.isGuest
                    )
                )
            }?.toMutableList()
            updateMeetingInfoBottomPanel()
        }
    }

    /**
     * Method for updating participant privileges
     *
     * @return list of participants with changes
     */
    fun updateParticipantsPrivileges(context: Context): MutableSet<Participant> {
        val listWithChanges = mutableSetOf<Participant>()
        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
            participants.value = participants.value?.map { participant ->
                return@map when {
                    participant.isModerator != isParticipantModerator(participant.peerId) -> {
                        listWithChanges.add(participant)
                        participant.copy(isModerator = isParticipantModerator(participant.peerId))
                    }

                    else -> participant
                }
            }?.toMutableList()
            updateMeetingInfoBottomPanel()
        }

        return listWithChanges
    }

    /**
     * Method to switch a call on hold
     *
     * @param isCallOnHold True, if I am going to put it on hold. False, otherwise
     */
    fun setCallOnHold(isCallOnHold: Boolean) {
        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
            inMeetingRepository.setCallOnHold(it.chatId, isCallOnHold)
        }
    }

    /**
     * Method to switch another call on hold
     *
     * @param chatId chat ID
     * @param isCallOnHold True, if I am going to put it on hold. False, otherwise
     */
    fun setAnotherCallOnHold(chatId: Long, isCallOnHold: Boolean) {
        inMeetingRepository.getChatRoom(chatId)?.let {
            inMeetingRepository.setCallOnHold(it.chatId, isCallOnHold)
        }
    }

    /**
     * Method to know if the session of a participants is null
     *
     * @param clientId The client ID of a participant
     */
    fun isSessionOnHold(clientId: Long): Boolean = getSession(clientId)?.isOnHold ?: false

    /**
     * Method for displaying the correct banner: If the call is muted or on hold
     *
     * @param bannerIcon The icon of the banner
     * @param bannerText The textView of the banner
     * @return The text of the banner
     */
    fun showAppropriateBanner(bannerIcon: ImageView?, bannerText: EmojiTextView?): Boolean {
        //Check call or session on hold
        if (isCallOnHold() || isSessionOnHoldOfOneToOneCall()) {
            bannerIcon?.let {
                it.isVisible = false
            }
            bannerText?.let {
                it.text = it.context.getString(R.string.call_on_hold)
            }
            return true
        }

        //Check mute call or session
        _callLiveData.value?.let { call ->
            if (isOneToOneCall()) {
                inMeetingRepository.getSessionOneToOneCall(call)?.let { session ->
                    if (!session.hasAudio() && session.peerid != MEGACHAT_INVALID_HANDLE) {
                        bannerIcon?.let {
                            it.isVisible = true
                        }
                        bannerText?.let {
                            it.text = it.context.getString(
                                R.string.muted_contact_micro,
                                inMeetingRepository.getContactOneToOneCallName(
                                    session.peerid
                                )
                            )
                        }
                        return true
                    }
                }
            }

            if (!call.hasLocalAudio()) {
                bannerIcon?.let {
                    it.isVisible = false
                }
                bannerText?.let {
                    it.text =
                        it.context.getString(R.string.muted_own_micro)
                }
                return true
            }
        }

        return false
    }

    /**
     *  Method to know if it is a outgoing call
     *
     *  @return True, if it is a outgoing call. False, otherwise
     */
    fun isRequestSent(): Boolean {
        val callId = _callLiveData.value?.callId ?: return false

        return callId != MEGACHAT_INVALID_HANDLE && MegaApplication.getChatManagement()
            .isRequestSent(callId)
    }

    /**
     * Method for determining whether to display the camera switching icon.
     *
     * @return True, if it is. False, if not.
     */
    fun isNecessaryToShowSwapCameraOption(): Boolean =
        _callLiveData.value?.let { it.status != CALL_STATUS_CONNECTING && it.hasLocalVideo() && !it.isOnHold }
            ?: run { false }


    /**
     * Start chat call
     *
     * @param enableVideo The video should be enabled
     * @param enableAudio The audio should be enabled
     * @return Chat id
     */
    fun startMeeting(
        enableVideo: Boolean,
        enableAudio: Boolean,
    ): LiveData<Long> {
        val chatIdResult = MutableLiveData<Long>()
        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
            Timber.d("The chat exists")
            if (CallUtil.isStatusConnected(
                    MegaApplication.getInstance().applicationContext,
                    it.chatId
                )
            ) {
                megaChatApiGateway.getChatCall(_state.value.currentChatId)?.let { call ->
                    Timber.d("There is a call, open it")
                    chatIdResult.value = call.chatid
                    CallUtil.openMeetingInProgress(
                        MegaApplication.getInstance().applicationContext,
                        _state.value.currentChatId,
                        true,
                        passcodeManagement
                    )

                    return chatIdResult
                }

                Timber.d("Chat status is connected")
                MegaApplication.isWaitingForCall = false

                viewModelScope.launch {
                    runCatching {
                        setChatVideoInDeviceUseCase()
                        startChatCall(_state.value.currentChatId, enableVideo, enableAudio)
                    }.onFailure { exception ->
                        Timber.e(exception)
                    }.onSuccess { resultStartCall ->
                        val chatId = resultStartCall.chatHandle
                        if (chatId != MEGACHAT_INVALID_HANDLE) {
                            chatManagement.setSpeakerStatus(chatId, resultStartCall.flag)
                            megaChatApiGateway.getChatCall(chatId)?.let { call ->
                                if (call.isOutgoing) {
                                    chatManagement.setRequestSentCall(call.callId, true)
                                }
                            }

                            chatIdResult.value = chatId
                        }
                    }
                }
            }
            return chatIdResult
        }

        Timber.d("The chat doesn't exists")
        inMeetingRepository.createMeeting(
            _state.value.chatTitle,
            CreateGroupChatWithPublicLink()
        )

        return chatIdResult
    }


    /**
     * Get my own privileges in the chat
     *
     * @return the privileges
     */
    fun getOwnPrivileges(): Int = inMeetingRepository.getOwnPrivileges(_state.value.currentChatId)

    /**
     * Method to know if the participant is a moderator.
     *
     * @param peerId User handle of a participant
     */
    private fun isParticipantModerator(peerId: Long): Boolean =
        if (isMe(peerId))
            getOwnPrivileges() == PRIV_MODERATOR
        else
            inMeetingRepository.getChatRoom(_state.value.currentChatId)
                ?.let { it.getPeerPrivilegeByHandle(peerId) == PRIV_MODERATOR }
                ?: run { false }

    /**
     * Method to know if the participant is my contact
     *
     * @param peerId User handle of a participant
     */
    private fun isMyContact(peerId: Long): Boolean =
        if (isMe(peerId))
            true
        else
            inMeetingRepository.isMyContact(peerId)

    /**
     * Method to update whether a user is my contact or not
     *
     * @param peerId User handle
     */
    fun updateParticipantsVisibility(peerId: Long) {
        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
            participants.value?.let { listParticipants ->
                val iterator = listParticipants.iterator()
                iterator.forEach {
                    if (it.peerId == peerId) {
                        it.isContact = isMyContact(peerId)
                    }
                }
            }
        }
    }

    /**
     * Method for updating the speaking participant
     *
     * @param newSpeakerPeerId User handle of a participant
     * @param newSpeakerClientId Client ID of a participant
     * @return list of participants with changes
     */
    fun updatePeerSelected(
        newSpeakerPeerId: Long,
        newSpeakerClientId: Long,
    ): MutableSet<Participant> {
        val listWithChanges = mutableSetOf<Participant>()
        participants.value?.forEach {
            when {
                it.isSpeaker && !it.isScreenShared && (it.peerId != newSpeakerPeerId || it.clientId != newSpeakerClientId) -> {
                    Timber.d("The previous speaker ${it.clientId}, now has isSpeaker false")
                    it.isSpeaker = false
                    listWithChanges.add(it)
                }

                !it.isSpeaker && !it.isScreenShared && it.peerId == newSpeakerPeerId && it.clientId == newSpeakerClientId -> {
                    Timber.d("New speaker selected found ${it.clientId}")
                    it.isSpeaker = true
                    addSpeaker(it)
                    listWithChanges.add(it)
                }
            }
        }

        return listWithChanges
    }

    /**
     * Check screens shared
     */
    fun checkScreensShared() {
        if (_state.value.callUIStatus == CallUIStatusType.SpeakerView) {
            val addScreensSharedParticipantsList = mutableSetOf<Participant>()
            participants.value?.filter { !it.isSpeaker }?.forEach {
                getSession(it.clientId)?.apply {
                    if (hasScreenShare() && !participantHasScreenSharedParticipant(it)) {
                        addScreensSharedParticipantsList.add(it)
                    }
                }
            }

            _state.update { state -> state.copy(addScreensSharedParticipantsList = addScreensSharedParticipantsList.toMutableList()) }
        }

        val removeScreensSharedParticipantsList = mutableSetOf<Participant>()
        participants.value?.forEach {
            getSession(it.clientId)?.apply {
                if (participantHasScreenSharedParticipant(it) && ((it.isSpeaker && hasScreenShare()) || (!it.isSpeaker && !hasScreenShare()))) {
                    getScreenShared(it.peerId, it.clientId)?.let { screenShared ->
                        removeScreensSharedParticipantsList.add(screenShared)
                    }
                }
            }
        }

        _state.update { state -> state.copy(removeScreensSharedParticipantsList = removeScreensSharedParticipantsList.toMutableList()) }
    }

    /**
     * Remove screen shared participant
     *
     * @param list  List of [Participant]
     * @param context   Context
     */
    fun removeScreenShareParticipant(list: List<Participant>?, context: Context): Int {
        _state.update { state -> state.copy(removeScreensSharedParticipantsList = null) }
        list?.forEach { user ->
            participants.value?.indexOf(user)?.let { position ->
                if (position != INVALID_POSITION) {
                    participants.value?.get(position)?.let { participant ->
                        if (participant.isVideoOn) {
                            participant.videoListener?.let { listener ->
                                removeResolutionAndListener(participant, listener)
                            }
                            participant.videoListener = null
                        }
                    }

                    participants.value?.removeAt(position)
                    Timber.d("Removing participant")
                    updateParticipantsList()
                    return position
                }
            }
        }
        return INVALID_POSITION
    }

    /**
     * Add screen shared participant
     *
     * @param list  List of [Participant]
     * @param context   Context
     * @return  Position of the screen shared
     */
    fun addScreenShareParticipant(list: List<Participant>?, context: Context): Int? {
        _state.update { state -> state.copy(addScreensSharedParticipantsList = null) }
        list?.forEach { participant ->
            createParticipant(
                isScreenShared = true,
                participant.clientId
            )?.let { screenSharedParticipant ->
                participants.value?.indexOf(participant)?.let { index ->
                    participants.value?.add(index, screenSharedParticipant)
                    updateParticipantsList()
                    return participants.value?.indexOf(screenSharedParticipant)
                }
            }
        }
        return INVALID_POSITION
    }

    /**
     * Method for create a participant
     *
     * @param isScreenShared True if it's the screen shared. False if not.
     * @param clientId  Client Id.
     * @return [Participant]
     */
    private fun createParticipant(isScreenShared: Boolean, clientId: Long): Participant? {
        _state.value.call?.apply {
            inMeetingRepository.getMegaChatSession(this.chatId, clientId)?.let { session ->
                when {
                    isScreenShared ->
                        participants.value?.filter { it.peerId == session.peerid && it.clientId == session.clientid && it.isScreenShared }
                            ?.apply {
                                if (isNotEmpty()) {
                                    return null
                                }
                            }

                    else ->
                        participants.value?.filter { it.peerId == session.peerid && it.clientId == session.clientid }
                            ?.apply {
                                if (isNotEmpty()) {
                                    Timber.d("Participants already shown")
                                    return null
                                }
                            }
                }

                val isModerator = isParticipantModerator(session.peerid)
                val name = getParticipantName(session.peerid)
                val isContact = isMyContact(session.peerid)
                val hasHiRes = needHiRes()

                val avatar = inMeetingRepository.getAvatarBitmap(session.peerid)
                val email = inMeetingRepository.getEmailParticipant(
                    session.peerid,
                    GetUserEmailListener(
                        MegaApplication.getInstance().applicationContext,
                        this@InMeetingViewModel
                    )
                )
                val isGuest = email == null

                val isSpeaker = getCurrentSpeakerParticipant()?.let { participant ->
                    participant.clientId == session.clientid && participant.peerId == session.peerid && participant.isSpeaker
                } ?: run { false }

                return Participant(
                    peerId = session.peerid,
                    clientId = session.clientid,
                    name = name,
                    avatar = avatar,
                    isMe = false,
                    isModerator = isModerator,
                    isAudioOn = session.hasAudio(),
                    isVideoOn = session.hasVideo(),
                    isAudioDetected = session.isAudioDetected,
                    isContact = isContact,
                    isSpeaker = isSpeaker,
                    hasHiRes = if (isScreenShared) true else hasHiRes,
                    videoListener = null,
                    isChosenForAssign = false,
                    isGuest = isGuest,
                    hasOptionsAllowed = shouldParticipantsOptionBeVisible(false, isGuest),
                    isPresenting = session.hasScreenShare(),
                    isScreenShared = isScreenShared,
                    isCameraOn = session.hasCamera(),
                    isScreenShareOn = session.hasScreenShare()
                )
            }
        }
        return null
    }

    /**
     * Method that creates the participant speaker
     *
     * @param participant The participant who is to be a speaker
     * @return speaker participant
     */
    private fun createSpeakerParticipant(participant: Participant): Participant =
        Participant(
            participant.peerId,
            participant.clientId,
            participant.name,
            participant.avatar,
            isMe = participant.isMe,
            isModerator = participant.isModerator,
            isAudioOn = participant.isAudioOn,
            isVideoOn = participant.isVideoOn,
            isAudioDetected = participant.isAudioDetected,
            isContact = participant.isContact,
            isSpeaker = true,
            hasHiRes = true,
            videoListener = null,
            participant.isChosenForAssign,
            participant.isGuest,
            isPresenting = participant.isPresenting,
            isScreenShared = false
        )

    /**
     * Method to update the current participants list
     */
    fun checkParticipantsList() {
        callLiveData.value?.let {
            val participants: Int = participants.value?.size ?: 0
            if (it.sessionsClientid.size() > 0 && (participants.toLong() != it.sessionsClientid.size())) {
                createCurrentParticipants(it.sessionsClientid)
            }
        }
    }

    /**
     * Method for creating participants already on the call
     *
     * @param list list of participants
     */
    private fun createCurrentParticipants(list: MegaHandleList?) {
        list?.let { listParticipants ->
            participants.value?.clear()
            if (listParticipants.size() > 0) {
                for (i in 0 until list.size()) {
                    createParticipant(isScreenShared = false, list[i])?.let { participantCreated ->
                        participants.value?.add(participantCreated)
                    }
                }

                updateParticipantsList()
            }
        }
    }

    /**
     * Method to control when the number of participants changes
     */
    private fun updateParticipantsList() {
        when (_state.value.callUIStatus) {
            CallUIStatusType.SpeakerView -> sortParticipantsListForSpeakerView()
            else -> participants.value = participants.value
        }
        updateMeetingInfoBottomPanel()
    }

    /**
     * Method for adding a participant to the list
     *
     * @param clientId  Client Id
     * @return the position of the participant
     */
    fun addParticipant(clientId: Long, context: Context): Int? {
        createParticipant(isScreenShared = false, clientId)?.let { participantCreated ->
            participants.value?.add(participantCreated)
            updateParticipantsList()

            return participants.value?.indexOf(participantCreated)
        }

        return INVALID_POSITION
    }

    /**
     * Method for removing the listener from participants who still have
     */
    fun removeListeners() {
        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
            val iterator = participants.value?.iterator()
            iterator?.let { list ->
                list.forEach { participant ->
                    participant.videoListener?.let { listener ->
                        removeResolutionAndListener(participant, listener)
                        participant.videoListener = null
                    }
                }
            }
        }
    }

    /**
     * Method for removing a participant
     *
     * @param session MegaChatSession of a participant
     * @return the position of the participant
     */
    fun removeParticipant(session: MegaChatSession, context: Context): Int {
        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
            val iterator = participants.value?.iterator()
            iterator?.let { list ->
                list.forEach { participant ->
                    if (participant.peerId == session.peerid && participant.clientId == session.clientid) {
                        val position = participants.value?.indexOf(participant)
                        val clientId = participant.clientId
                        val isSpeaker = participant.isSpeaker

                        participant.isSpeaker = false

                        if (position != null && position != INVALID_POSITION) {
                            if (participant.isVideoOn) {
                                participant.videoListener?.let { listener ->
                                    removeResolutionAndListener(participant, listener)
                                }
                                participant.videoListener = null
                            }

                            participants.value?.removeAt(position)
                            Timber.d("Removing participant... $clientId")
                            updateParticipantsList()

                            if (isSpeaker) {
                                Timber.d("The removed participant was speaker, clientID ${participant.clientId}")
                                removePreviousSpeakers()
                                removeCurrentSpeaker()
                            }
                            return position
                        }
                    }
                }
            }
        }

        return INVALID_POSITION
    }

    /**
     * Stop remote video resolution of participant in a meeting.
     *
     * @param participant The participant from whom the video is to be closed
     */
    fun removeRemoteVideoResolution(participant: Participant) {
        if (participant.videoListener == null) return

        getSession(participant.clientId)?.let {
            when {
                participant.hasHiRes && it.canRecvVideoHiRes() -> {
                    Timber.d("Stop HiResolution and remove listener, clientId = ${participant.clientId}")
                    stopHiResVideo(it, _state.value.currentChatId)
                }

                !participant.hasHiRes && it.canRecvVideoLowRes() -> {
                    Timber.d("Stop LowResolution and remove listener, clientId = ${participant.clientId}")
                    stopLowResVideo(it, _state.value.currentChatId)
                }

                else -> {}
            }
        }
    }

    /**
     * Remove remote video listener of participant in a meeting.
     *
     * @param participant The participant from whom the video is to be closed
     */
    fun removeRemoteVideoListener(
        participant: Participant,
        listener: MegaChatVideoListenerInterface,
    ) {
        Timber.d("Remove the remote video listener of clientID ${participant.clientId}")
        removeChatRemoteVideoListener(
            listener,
            participant.clientId,
            _state.value.currentChatId,
            participant.hasHiRes
        )
    }

    /**
     * Close Video of participant in a meeting. Removing resolution and listener.
     *
     * @param participant The participant from whom the video is to be closed
     */
    fun removeResolutionAndListener(
        participant: Participant,
        listener: MegaChatVideoListenerInterface,
    ) {
        if (participant.videoListener == null) return

        removeRemoteVideoResolution(participant)
        removeRemoteVideoListener(participant, listener)
    }

    /**
     * Method to create the GroupVideoListener
     *
     * @param participant The participant whose listener is to be created
     * @param alpha Alpha for TextureView
     * @param rotation Rotation for TextureView
     */
    fun createVideoListener(
        participant: Participant,
        alpha: Float,
        rotation: Float,
    ): GroupVideoListener {
        val myTexture = TextureView(MegaApplication.getInstance().applicationContext)
        myTexture.layoutParams = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        myTexture.alpha = alpha
        myTexture.rotation = rotation

        return GroupVideoListener(
            myTexture,
            participant.peerId,
            participant.clientId,
            participant.isMe,
            participant.isScreenShared
        )
    }

    /**
     * Method for know if the resolution of a participant's video should be high
     *
     * @return True, if should be high. False, otherwise
     */
    private fun needHiRes(): Boolean =
        participants.value?.let { state.value.callUIStatus != CallUIStatusType.SpeakerView }
            ?: run { false }

    /**
     * Method to know if the session has video on and is not on hold
     *
     * @param clientId Client ID of participant
     * @return True, it does. False, if not.
     */
    fun sessionHasVideo(clientId: Long): Boolean =
        getSession(clientId)?.let { it.hasVideo() && !isCallOrSessionOnHold(it.clientid) && it.status == MegaChatSession.SESSION_STATUS_IN_PROGRESS }
            ?: run { false }

    /**
     * Method for get the participant name
     *
     * @param peerId user handle
     * @return the name of a participant
     */
    private fun getParticipantName(peerId: Long): String =
        if (isMe(peerId))
            inMeetingRepository.getMyFullName()
        else
            inMeetingRepository.participantName(peerId) ?: " "

    /**
     * Method that marks a participant as a non-speaker
     *
     * @param peerId User handle of a participant
     * @param clientId Client ID of a participant
     */
    fun removeSelected(peerId: Long, clientId: Long) {
        val iterator = participants.value?.iterator()
        iterator?.let { participant ->
            participant.forEach {
                if (it.peerId == peerId && it.clientId == clientId && it.isSpeaker) {
                    it.isSpeaker = false
                }
            }
        }
    }

    /**
     * Get the avatar
     *
     * @param peerId User handle of a participant
     * @return the avatar of a participant
     */
    fun getAvatarBitmap(peerId: Long): Bitmap? =
        inMeetingRepository.getChatRoom(_state.value.currentChatId)
            ?.let { inMeetingRepository.getAvatarBitmap(peerId) }

    /**
     * Method to get the first participant in the list, who will be the new speaker
     */
    fun getFirstParticipant(peerId: Long, clientId: Long): Participant? {
        participants.value?.let { participantsList ->
            if (participantsList.isEmpty()) return null

            when {
                peerId == -1L && clientId == -1L -> {
                    participants.value?.sortByDescending { it.isPresenting }
                    return participants.value?.first()
                }

                else -> participantsList.filter { it.peerId != peerId || it.clientId != clientId }
                    .forEach {
                        return it
                    }
            }

        }

        return null
    }

    /**
     * Get participant from peerId and clientId
     *
     * @param peerId peer ID of a participant
     * @param clientId client ID of a participant
     */
    fun getParticipant(peerId: Long, clientId: Long): Participant? {
        participants.value?.filter { it.peerId == peerId && it.clientId == clientId && !it.isScreenShared }
            ?.apply {
                return if (isNotEmpty()) first() else null
            }

        return null
    }

    /**
     * Get participant or screen share from peerId and clientId
     *
     * @param peerId peer ID of a participant
     * @param clientId client ID of a participant
     * @param isScreenShared True, it's the screen shared. False if not.
     * @return The participant or screen shared from peerId and clientId.
     */
    fun getParticipantOrScreenShared(
        peerId: Long,
        clientId: Long,
        isScreenShared: Boolean? = false,
    ): Participant? {
        participants.value?.filter { it.peerId == peerId && it.clientId == clientId && isScreenShared == it.isScreenShared }
            ?.apply {
                return if (isNotEmpty()) first() else null
            }

        return null
    }

    /**
     * Get speaker participant
     *
     * @param peerId    Peer Id
     * @param clientId  Client Id
     * @return [Participant]
     */
    fun getSpeaker(peerId: Long, clientId: Long): Participant? {
        speakerParticipants.value?.filter { it.peerId == peerId && it.clientId == clientId }
            ?.apply {
                return if (isNotEmpty()) first() else null
            }

        return null
    }

    /**
     * Get screen shared participant
     *
     * @param peerId    Peer Id
     * @param clientId  Client Id
     * @return [Participant]
     */
    fun getScreenShared(peerId: Long, clientId: Long): Participant? {
        participants.value?.filter { it.peerId == peerId && it.clientId == clientId && it.isScreenShared }
            ?.apply {
                return if (isNotEmpty()) first() else null
            }

        return null
    }

    /**
     * Method for updating participant video
     *
     * @param session of a participant
     * @return True, if there have been changes. False, otherwise
     */
    fun changesInRemoteVideoFlag(session: MegaChatSession): Boolean {
        var hasChanged = false
        participants.value = participants.value?.map { participant ->
            return@map when {
                participant.peerId == session.peerid && participant.clientId == session.clientid -> {
                    if (participant.isVideoOn != session.hasVideo() ||
                        participant.isCameraOn != session.hasCamera() ||
                        participant.isScreenShareOn != session.hasScreenShare()
                    ) {
                        hasChanged = true
                    }

                    return@map participant.copy(
                        isVideoOn = session.hasVideo(),
                        isCameraOn = session.hasCamera(),
                        isScreenShareOn = session.hasScreenShare()
                    )
                }

                else -> participant
            }
        }?.toMutableList()

        speakerParticipants.value = speakerParticipants.value?.map { participant ->
            return@map when {
                participant.peerId == session.peerid && participant.clientId == session.clientid && participant.isVideoOn != session.hasVideo() -> {
                    hasChanged = true
                    participant.copy(isVideoOn = session.hasVideo())
                }

                else -> participant
            }
        }?.toMutableList()

        if (hasChanged) {
            checkScreensShared()
        }

        return hasChanged
    }

    /**
     * Method for updating participant screen sharing
     *
     * @param session of a participant
     * @return True, if there have been changes. False, otherwise
     */
    fun changesInScreenSharing(session: MegaChatSession): Boolean {
        var hasChanged = false
        var participantSharingScreen: Participant? = null
        var participantSharingScreenForSpeaker: Participant? = null

        participants.value = participants.value?.map { participant ->
            return@map when {
                participant.peerId == session.peerid && participant.clientId == session.clientid && participant.isPresenting != session.hasScreenShare() -> {
                    hasChanged = true
                    if (session.hasScreenShare()) {
                        participantSharingScreen = participant
                    }

                    participant.copy(isPresenting = session.hasScreenShare())

                }

                else -> participant
            }
        }?.toMutableList()

        participantSharingScreen?.let {
            if (_state.value.callUIStatus == CallUIStatusType.SpeakerView) {
                pinSpeaker(it)
            }
        } ?: run {
            speakerParticipants.value = speakerParticipants.value?.map { speakerParticipant ->
                return@map when {
                    speakerParticipant.peerId == session.peerid && speakerParticipant.clientId == session.clientid && speakerParticipant.isPresenting != session.hasScreenShare() -> {
                        if (!session.hasScreenShare()) {
                            getAnotherParticipantWhoIsPresenting(speakerParticipant)?.let { newSpeaker ->
                                participantSharingScreenForSpeaker = newSpeaker
                            }
                        }

                        hasChanged = true
                        speakerParticipant.copy(isPresenting = session.hasScreenShare())
                    }

                    else -> speakerParticipant
                }
            }?.toMutableList()
        }

        participantSharingScreenForSpeaker?.let {
            if (_state.value.callUIStatus == CallUIStatusType.SpeakerView) {
                pinSpeaker(it)
            }
        }

        if (hasChanged) {
            checkScreensShared()
        }

        return hasChanged
    }

    /**
     * Get another participant who is presenting
     *
     * @param currentSpeaker    [Participant]
     * @return  [Participant]
     */
    private fun getAnotherParticipantWhoIsPresenting(currentSpeaker: Participant): Participant? {
        participants.value?.filter { it.isPresenting && (it.peerId != currentSpeaker.peerId || it.clientId != currentSpeaker.clientId) }
            ?.apply {
                return if (isNotEmpty()) first() else null
            }

        return null
    }

    /**
     * Method for updating participant audio
     *
     * @param session of a participant
     * @return True, if there have been changes. False, otherwise
     */
    fun changesInRemoteAudioFlag(session: MegaChatSession): Boolean {
        var hasChanged = false
        participants.value = participants.value?.map { participant ->
            return@map when {
                participant.peerId == session.peerid && participant.clientId == session.clientid &&
                        (participant.isAudioOn != session.hasAudio() || participant.isAudioDetected != session.isAudioDetected) -> {
                    hasChanged = true
                    participant.copy(isAudioOn = session.hasAudio())
                }

                else -> participant
            }
        }?.toMutableList()

        return hasChanged
    }

    /**
     * Method that makes the necessary checks before joining a meeting.
     * If there is another call, it must be put on hold.
     * If there are two other calls, the one in progress is hung up.
     *
     * @param chatIdOfCurrentCall chat id of current call
     */
    fun checkAnotherCallsInProgress(chatIdOfCurrentCall: Long) {
        val numCallsParticipating = CallUtil.getCallsParticipating()
        numCallsParticipating?.let {
            if (numCallsParticipating.isEmpty()) {
                return
            }

            if (numCallsParticipating.size == 1) {
                getAnotherCall(numCallsParticipating[0])?.let { anotherCall ->
                    if (chatIdOfCurrentCall != anotherCall.chatid && !anotherCall.isOnHold) {
                        Timber.d("Another call on hold before join the meeting")
                        setAnotherCallOnHold(anotherCall.chatid, true)
                    }
                }
            } else {
                for (i in 0 until numCallsParticipating.size) {
                    getAnotherCall(numCallsParticipating[i])?.let { anotherCall ->
                        if (chatIdOfCurrentCall != anotherCall.chatid && !anotherCall.isOnHold) {
                            Timber.d("Hang up one of the current calls in order to join the meeting")
                            hangUpSpecificCall(anotherCall.callId)
                        }
                    }
                }
            }
        }
    }

    /**
     * Method for ignore a call
     */
    private fun ignoreCall() {
        _callLiveData.value?.let {
            inMeetingRepository.ignoreCall(it.chatid)
        }
    }

    /**
     * Method for remove incoming call notification
     */
    private fun removeIncomingCallNotification(chatId: Long) {
        inMeetingRepository.getMeeting(chatId)?.let { call ->
            rtcAudioManagerGateway.stopSounds()
            CallUtil.clearIncomingCallNotification(call.callId)
        }
    }

    /**
     * Method to hang up a specific call
     *
     * @param callId Call ID
     */
    private fun hangUpSpecificCall(callId: Long) {
        hangCall(callId)
    }

    /**
     * Set a title for the chat
     *
     * @param newTitle the chat title
     */
    fun setTitleChat(newTitle: String) {
        if (_state.value.currentChatId == MEGACHAT_INVALID_HANDLE) {
            _state.update { it.copy(chatTitle = newTitle) }
        } else {
            inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
                inMeetingRepository.setTitleChatRoom(
                    it.chatId,
                    newTitle,
                    EditChatRoomNameListener(MegaApplication.getInstance(), this)
                )
            }
        }
    }

    /**
     * Method of obtaining the remote video
     *
     * @param listener MegaChatVideoListenerInterface
     * @param clientId Client ID of participant
     * @param chatId Chat ID
     * @param isHiRes True, if it has HiRes. False, if it has LowRes
     */
    fun addChatRemoteVideoListener(
        listener: MegaChatVideoListenerInterface,
        clientId: Long,
        chatId: Long,
        isHiRes: Boolean,
    ) {
        Timber.d("Adding remote video listener, clientId $clientId, isHiRes $isHiRes")
        inMeetingRepository.addChatRemoteVideoListener(
            chatId,
            clientId,
            isHiRes,
            listener
        )
    }

    /**
     * Method of remove the remote video
     *
     * @param listener MegaChatVideoListenerInterface
     * @param clientId Client ID of participant
     * @param chatId Chat ID
     * @param isHiRes True, if it has HiRes. False, if it has LowRes
     */
    fun removeChatRemoteVideoListener(
        listener: MegaChatVideoListenerInterface,
        clientId: Long,
        chatId: Long,
        isHiRes: Boolean,
    ) {
        Timber.d("Removing remote video listener, clientId $clientId, isHiRes $isHiRes")
        inMeetingRepository.removeChatRemoteVideoListener(
            chatId,
            clientId,
            isHiRes,
            listener
        )
    }

    /**
     * Add High Resolution for remote video
     *
     * @param session MegaChatSession of a participant
     * @param chatId Chat ID
     */
    fun requestHiResVideo(
        session: MegaChatSession?,
        chatId: Long,
    ) = session?.apply {
        if (!canRecvVideoHiRes() && isHiResVideo) {
            viewModelScope.launch {
                runCatching {
                    Timber.d("Request HiRes for remote video, clientId $clientid")
                    requestHighResolutionVideoUseCase(chatId, clientid)
                }.onFailure { exception ->
                    Timber.e(exception)
                }.onSuccess { request ->
                    Timber.d("Request high res video: chatId = ${request.chatHandle}, hires? ${request.flag}, clientId = ${request.userHandle}")
                }
            }
        }
    }

    /**
     * Remove High Resolution for remote video
     *
     * @param session MegaChatSession of a participant
     * @param chatId Chat ID
     */
    fun stopHiResVideo(
        session: MegaChatSession?,
        chatId: Long,
    ) = session?.apply {
        if (canRecvVideoHiRes()) {
            viewModelScope.launch {
                runCatching {
                    Timber.d("Stop HiRes for remote video, clientId $clientid")
                    stopHighResolutionVideoUseCase(chatId, clientid)
                }.onFailure { exception ->
                    Timber.e(exception)
                }.onSuccess { request ->
                    Timber.d("Stop high res video: chatId = ${request.chatHandle}, hires? ${request.flag}, clientId = ${request.userHandle}")
                }
            }
        }
    }

    /**
     * Add Low Resolution for remote video
     *
     * @param session MegaChatSession of a participant
     * @param chatId Chat ID
     */
    fun requestLowResVideo(
        session: MegaChatSession?,
        chatId: Long,
    ) = session?.apply {
        if (!canRecvVideoLowRes() && isLowResVideo) {
            viewModelScope.launch {
                runCatching {
                    Timber.d("Request LowRes for remote video, clientId $clientid")
                    requestLowResolutionVideoUseCase(chatId, clientid)
                }.onFailure { exception ->
                    Timber.e(exception)
                }.onSuccess { request ->
                    Timber.d("Request low res video: chatId = ${request.chatHandle}, lowRes? ${request.flag}, clientId = ${request.userHandle}")
                }
            }
        }
    }

    /**
     * Remove Low Resolution for remote video
     *
     * @param session MegaChatSession of a participant
     * @param chatId Chat ID
     */
    private fun stopLowResVideo(
        session: MegaChatSession?,
        chatId: Long,
    ) = session?.apply {
        if (canRecvVideoLowRes()) {
            viewModelScope.launch {
                runCatching {
                    Timber.d("Stop LowRes for remote video, clientId $clientid")
                    stopLowResolutionVideoUseCase(chatId, clientid)
                }.onFailure { exception ->
                    Timber.e(exception)
                }.onSuccess { request ->
                    Timber.d("Stop low res video: chatId = ${request.chatHandle}, lowRes? ${request.flag}, clientId = ${request.userHandle}")
                }
            }
        }
    }

    /**
     * Method for checking which participants need to change their resolution when the UI is changed
     *
     * In Speaker view, the list of participants should have low res
     * In Grid view, if there is more than 4, low res. Hi res in the opposite case
     */
    fun updateParticipantResolution() {
        Timber.d("Changing the resolution of participants when the UI changes")
        participants.value?.let { listParticipants ->
            val iterator = listParticipants.iterator()
            iterator.forEach { participant ->
                getSession(participant.clientId)?.let {
                    if (state.value.callUIStatus == CallUIStatusType.SpeakerView && participant.hasHiRes && !participant.isScreenShared) {
                        Timber.d("Change to low resolution, clientID ${participant.clientId}")
                        participant.videoListener?.let {
                            removeResolutionAndListener(participant, it)
                        }

                        participant.videoListener = null
                        participant.hasHiRes = false
                    } else if (state.value.callUIStatus == CallUIStatusType.GridView && !participant.hasHiRes) {
                        Timber.d("Change to high resolution, clientID ${participant.clientId}")
                        participant.videoListener?.let {
                            removeResolutionAndListener(participant, it)
                        }
                        participant.videoListener = null
                        participant.hasHiRes = true
                    }
                }
            }
        }
    }

    /**
     * Adding visible participant
     *
     * @param participant The participant that is now visible
     */
    fun addParticipantVisible(participant: Participant) {
        if (visibleParticipants.size == 0) {
            visibleParticipants.add(participant)
            return
        }

        val checkParticipant = visibleParticipants.filter {
            it.peerId == participant.peerId && it.clientId == participant.clientId
        }

        if (checkParticipant.isEmpty()) {
            visibleParticipants.add(participant)
        }
    }

    /**
     * Removing all visible participants
     */
    fun removeAllParticipantVisible() {
        if (visibleParticipants.isEmpty()) {
            return
        }

        visibleParticipants.clear()
    }

    /**
     * Removing visible participant
     *
     * @param participant The participant that is not now visible
     */
    fun removeParticipantVisible(participant: Participant) {
        if (visibleParticipants.size == 0) {
            return
        }
        val checkParticipant = visibleParticipants.filter {
            it.peerId == participant.peerId && it.clientId == participant.clientId
        }
        if (checkParticipant.isNotEmpty()) {
            visibleParticipants.remove(participant)
        }
    }

    /**
     * Check if a participant is visible
     *
     * @param participant The participant to be checked whether or not he/she is visible
     * @return True, if it's visible. False, otherwise
     */
    fun isParticipantVisible(participant: Participant): Boolean {
        if (visibleParticipants.isNotEmpty()) {
            val participantVisible = visibleParticipants.filter {
                it.peerId == participant.peerId && it.clientId == participant.clientId
            }

            if (participantVisible.isNotEmpty()) {
                return true
            }
        }

        return false
    }

    /**
     * Updating visible participants list
     *
     * @param list new list of visible participants
     */
    fun updateVisibleParticipants(list: List<Participant>?) {
        if (!list.isNullOrEmpty()) {
            val iteratorParticipants = list.iterator()
            iteratorParticipants.forEach { participant ->
                addParticipantVisible(participant)
            }
            Timber.d("Num visible participants is ${visibleParticipants.size}")
        }
    }

    override fun onCleared() {
        super.onCleared()

        LiveEventBus.get(EVENT_UPDATE_CALL, MegaChatCall::class.java)
            .removeObserver(updateCallObserver)

        LiveEventBus.get(EventConstants.EVENT_NOT_OUTGOING_CALL, Long::class.java)
            .removeObserver(noOutgoingCallObserver)

        LiveEventBus.get<Pair<Long, Boolean>>(EventConstants.EVENT_UPDATE_WAITING_FOR_OTHERS)
            .removeObserver(waitingForOthersBannerObserver)
    }

    override fun onEditedChatRoomName(chatId: Long, name: String) {
        if (_state.value.currentChatId == chatId) {
            _state.update { state ->
                state.copy(
                    chatTitle = name,
                )
            }
        }
    }

    /**
     * Determine the chat room has only one moderator and the list is not empty and I am moderator
     *
     * @return True, if you can be assigned as a moderator. False, otherwise.
     */
    private fun shouldAssignModerator(): Boolean {
        if (!isModerator() || numParticipants() == 0) {
            return false
        }

        return participants.value?.toList()?.filter { it.isModerator }.isNullOrEmpty()
    }

    /**
     * Get num of participants in the call
     *
     * @return num of participants
     */
    fun numParticipants(): Int {
        participants.value?.size?.let { numParticipants ->
            return numParticipants
        }

        return 0
    }

    /**
     * Method to open chat preview when joining as a guest
     *
     * @param link The link to the chat room or the meeting
     * @param listener MegaChatRequestListenerInterface
     */
    fun openChatPreview(link: String, listener: MegaChatRequestListenerInterface) =
        inMeetingRepository.openChatPreview(link, listener)

    /**
     * Method to join a chat group
     *
     * @param chatId Chat ID
     * @param listener MegaChatRequestListenerInterface
     */
    fun joinPublicChat(chatId: Long, listener: MegaChatRequestListenerInterface) =
        inMeetingRepository.joinPublicChat(chatId, listener)

    /**
     * Method to rejoin a chat group
     *
     * @param chatId Chat ID
     * @param publicChatHandle MegaChatHandle that corresponds with the public handle of chat room
     * @param listener MegaChatRequestListenerInterface
     */
    fun rejoinPublicChat(
        chatId: Long,
        publicChatHandle: Long,
        listener: MegaChatRequestListenerInterface,
    ) {
        inMeetingRepository.rejoinPublicChat(chatId, publicChatHandle, listener)
    }

    /**
     * Method to add the chat listener when joining as a guest
     *
     * @param chatId Chat ID
     * @param callback
     */
    fun registerConnectionUpdateListener(chatId: Long, callback: () -> Unit) =
        inMeetingRepository.registerConnectionUpdateListener(chatId, callback)

    /**
     * Get my own information
     *
     * @param audio local audio
     * @param video local video
     * @return Me as a Participant
     */
    fun getMyOwnInfo(audio: Boolean, video: Boolean): Participant {
        val participant = inMeetingRepository.getMyInfo(
            getOwnPrivileges() == PRIV_MODERATOR,
            audio,
            video
        )
        participant.hasOptionsAllowed =
            shouldParticipantsOptionBeVisible(participant.isMe, participant.isGuest)

        return participant
    }

    /**
     * Determine if I am a guest
     *
     * @return True, if I am a guest. False if not
     */
    fun amIAGuest(): Boolean = inMeetingRepository.amIAGuest()

    /**
     * Determine if I am a moderator
     *
     * @return True, if I am a moderator. False if not
     */
    private fun amIAModerator(): Boolean = getOwnPrivileges() == PRIV_MODERATOR

    /**
     * Determine if the participant has standard privileges
     *
     * @param peerId User handle of a participant
     */
    fun isStandardUser(peerId: Long): Boolean =
        inMeetingRepository.getChatRoom(_state.value.currentChatId)
            ?.let { it.getPeerPrivilegeByHandle(peerId) == MegaChatRoom.PRIV_STANDARD }
            ?: run { false }

    /**
     * Determine if I am a moderator
     *
     * @return True, if I am a moderator. False, if not
     */
    fun isModerator(): Boolean =
        getOwnPrivileges() == PRIV_MODERATOR

    /**
     * Method to check if tips should be displayed
     *
     * @return True, if tips must be shown. False, if not.
     */
    fun shouldShowTips(): Boolean =
        !MegaApplication.getInstance().applicationContext.defaultSharedPreferences
            .getBoolean(IS_SHOWED_TIPS, false)

    /**
     * Update whether or not to display tips
     */
    fun updateShowTips() {
        MegaApplication.getInstance().applicationContext.defaultSharedPreferences.edit()
            .putBoolean(IS_SHOWED_TIPS, true).apply()
    }

    companion object {
        const val IS_SHOWED_TIPS = "is_showed_meeting_bottom_tips"
    }

    override fun onUserEmailUpdate(email: String?, handler: Long, position: Int) {
        if (email == null)
            return

        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
            participants.value = participants.value?.map { participant ->
                return@map when (participant.peerId) {
                    handler -> {
                        participant.copy(isGuest = false)
                    }

                    else -> participant
                }
            }?.toMutableList()
        }
    }

    /**
     * Update the connection status
     *
     * @param status new status
     */
    fun updateNetworkStatus(status: Boolean) =
        _state.update {
            it.copy(
                haveConnection = status,
            )
        }

    /**
     * Method for updating meeting info panel information
     */
    private fun updateMeetingInfoBottomPanel() {
        var nameList =
            if (isModerator()) inMeetingRepository.getMyName() else ""
        var numParticipantsModerator = if (isModerator()) 1 else 0
        var numParticipants = 1

        participants.value?.let { list ->
            numParticipants += list.count { !it.isScreenShared }
            list.filter { it.isModerator && it.name.isNotEmpty() }
                .map { it.name }
                .forEach {
                    numParticipantsModerator++
                    nameList = if (nameList.isNotEmpty()) "$nameList, $it" else it
                }
        }

        _state.update { state ->
            state.copy(
                updateNumParticipants = numParticipants,
                updateModeratorsName = if (numParticipantsModerator == 0) "" else
                    context.resources.getQuantityString(
                        R.plurals.meeting_call_screen_meeting_info_bottom_panel_name_of_moderators,
                        numParticipantsModerator,
                        nameList
                    )
            )
        }
    }

    /**
     * Send add contact invitation
     *
     * @param context the Context
     * @param peerId the peerId of users
     * @param callback the callback for sending add contact request
     */
    fun addContact(context: Context, peerId: Long, callback: (String) -> Unit) {
        inMeetingRepository.addContact(context, peerId, callback)
    }

    /**
     * Get avatar from sdk
     *
     * @param peerId the peerId of participant
     */
    fun getRemoteAvatar(peerId: Long) {
        inMeetingRepository.getRemoteAvatar(peerId)
    }

    /**
     * Method for clearing the list of speakers
     */
    fun clearSpeakerParticipants() {
        speakerParticipants.value?.clear()
    }

    /**
     * Method to obtain the current speaker
     *
     * @return The speaker
     */
    fun getCurrentSpeakerParticipant(): Participant? {
        speakerParticipants.value?.filter { it.isSpeaker }?.apply {
            return if (isNotEmpty()) first() else null
        }

        return null
    }

    /**
     * Method to remove the current speaker
     */
    private fun removeCurrentSpeaker() {
        speakerParticipants.value?.filter { it.isSpeaker }?.forEach { participant ->
            participant.videoListener?.let {
                removeResolutionAndListener(participant, it)
                participant.videoListener = null
            }
            speakerParticipants.value?.indexOf(participant)?.let { position ->
                if (position != INVALID_POSITION) {
                    speakerParticipants.value?.removeAt(position)
                    Timber.d("Num of speaker participants: ${speakerParticipants.value?.size}")
                    speakerParticipants.value = speakerParticipants.value
                }
            }
        }
    }

    /**
     * Method to eliminate which are no longer speakers
     */
    fun removePreviousSpeakers() {
        speakerParticipants.value?.filter { !it.isSpeaker }?.forEach { participant ->
            participant.videoListener?.let {
                removeResolutionAndListener(participant, it)
                participant.videoListener = null
            }
            speakerParticipants.value?.indexOf(participant)?.let { position ->
                if (position != INVALID_POSITION) {
                    speakerParticipants.value?.removeAt(position)
                    Timber.d("Num of speaker participants: ${speakerParticipants.value?.size}")
                    speakerParticipants.value = speakerParticipants.value
                }
            }
        }
    }

    /**
     * Method to add a new speaker to the list
     *
     * @param participant The participant who is chosen as speaker
     */
    private fun addSpeaker(participant: Participant) {
        if (speakerParticipants.value.isNullOrEmpty()) {
            createSpeaker(participant)
        } else {
            speakerParticipants.value?.let { listParticipants ->
                val iterator = listParticipants.iterator()
                iterator.forEach { speaker ->
                    speaker.isSpeaker =
                        speaker.peerId == participant.peerId && speaker.clientId == participant.clientId
                }
            }

            speakerParticipants.value?.let { listSpeakerParticipants ->
                val listFound = listSpeakerParticipants.filter { speaker ->
                    speaker.peerId == participant.peerId && speaker.clientId == participant.clientId
                }

                if (listFound.isEmpty()) {
                    createSpeaker(participant)
                }
            }
        }
    }

    /**
     * Method for creating a participant speaker
     *
     * @param participant The participant who is chosen as speaker
     */
    private fun createSpeaker(participant: Participant) {
        createSpeakerParticipant(participant).let { speakerParticipantCreated ->
            speakerParticipants.value?.add(speakerParticipantCreated)
            speakerParticipants.value = speakerParticipants.value
        }
    }

    private fun participantHasScreenSharedParticipant(participant: Participant): Boolean {
        participants.value?.filter { it.peerId == participant.peerId && it.clientId == participant.clientId && it.isScreenShared }
            ?.let {
                return it.isNotEmpty()
            }

        return false
    }

    /**
     * Method to get the list of participants who are no longer speakers
     *
     * @param peerId Peer ID of current speaker
     * @param clientId Client ID of current speaker
     * @return a list of participants who are no longer speakers
     */
    fun getPreviousSpeakers(peerId: Long, clientId: Long): List<Participant>? {
        if (speakerParticipants.value.isNullOrEmpty()) {
            return null
        }

        val checkParticipant = (speakerParticipants.value ?: return null).filter {
            it.peerId != peerId || it.clientId != clientId
        }

        if (checkParticipant.isNotEmpty()) {
            return checkParticipant
        }

        return null
    }

    /**
     * Method to know if local video is activated
     *
     * @return True, if it's on. False, if it's off
     */
    fun isLocalCameraOn(): Boolean = getCall()?.hasLocalVideo() ?: false

    /**
     * Method that controls whether a participant's options (3 dots) should be enabled or not
     *
     * @param participantIsMe If the participant is me
     * @param participantIsGuest If the participant is a guest
     * @return True, if should be enabled. False, if not
     */
    private fun shouldParticipantsOptionBeVisible(
        participantIsMe: Boolean,
        participantIsGuest: Boolean,
    ): Boolean = !((!amIAModerator() && participantIsGuest) ||
            (amIAGuest() && participantIsMe) ||
            (!amIAModerator() && amIAGuest() && !participantIsMe))

    /**
     * End for all specified call
     *
     * @param chatId Chat ID
     */
    private fun endCallForAll(chatId: Long) = viewModelScope.launch {
        Timber.d("End for all. Chat id $chatId")
        runCatching {
            endCallUseCase(chatId)
        }.onSuccess {
            broadcastCallEndedUseCase(chatId)
        }.onFailure {
            Timber.e(it.stackTraceToString())
        }
    }

    /**
     * End for all the current call
     */
    fun endCallForAll() {
        callLiveData.value?.let { call ->
            endCallForAll(call.chatid)
        }

        viewModelScope.launch {
            kotlin.runCatching {
                sendStatisticsMeetingsUseCase(EndCallForAll())
            }
        }
    }

    /**
     * Hang up a specified call
     *
     * @param callId Call ID
     */
    private fun hangCall(callId: Long) = viewModelScope.launch {
        Timber.d("Hang up call. Call id $callId")
        runCatching {
            hangChatCallUseCase(callId)
        }.onSuccess {
            broadcastCallEndedUseCase(state.value.currentChatId)
        }.onFailure {
            Timber.e(it.stackTraceToString())
        }
    }

    /**
     * Hang up the current call
     */
    fun hangCall() {
        callLiveData.value?.let { call ->
            hangCall(call.callId)
        }
    }

    /**
     * Control when the hang up button is clicked
     */
    fun checkClickEndButton() {
        if (isOneToOneCall()) {
            hangCall()
            return
        }

        if (amIAGuest()) {
            _callLiveData.value?.let {
                LiveEventBus.get(
                    EventConstants.EVENT_REMOVE_CALL_NOTIFICATION,
                    Long::class.java
                ).post(it.callId)
                hangCall(it.callId)
            }
            return
        }

        if (numParticipants() == 0) {
            hangCall()
            return
        }

        getChat()?.let { chat ->
            when (chat.ownPrivilege) {
                PRIV_MODERATOR -> _state.update { state ->
                    val shouldAssignHost = chat.isMeeting && shouldAssignModerator()
                    state.copy(
                        showEndMeetingAsHostBottomPanel = !shouldAssignHost,
                        showEndMeetingAsOnlyHostBottomPanel = shouldAssignHost,
                    )
                }

                else -> hangCall()
            }
        }
    }

    /**
     * Hide bottom panels
     */
    fun hideBottomPanels() {
        _state.update { state ->
            state.copy(
                showEndMeetingAsHostBottomPanel = false,
                showEndMeetingAsOnlyHostBottomPanel = false,
            )
        }
    }

    /**
     * Set call UI status
     *
     * @param newStatus [CallUIStatusType]
     */
    fun setStatus(newStatus: CallUIStatusType) {
        if (_state.value.callUIStatus != newStatus && newStatus == CallUIStatusType.SpeakerView) {
            sortParticipantsListForSpeakerView()
        }
        _state.update { it.copy(callUIStatus = newStatus) }

    }

    /**
     * On update list consumed
     */
    fun onUpdateListConsumed() =
        _state.update { it.copy(updateListUi = false) }

    /**
     * Sort participants list for speaker view
     *
     * @param participantAtTheBeginning [Participant] to be placed first in the carousel
     */
    private fun sortParticipantsListForSpeakerView(participantAtTheBeginning: Participant? = null) {
        Timber.d("Sort participant list")
        participantAtTheBeginning?.apply {
            participants.value?.sortByDescending { it.peerId == peerId }
        }
        participants.value?.sortByDescending { it.isPresenting }
        _state.update { it.copy(updateListUi = true) }
    }

    /**
     * On reject button tapped
     *
     * @param chatId Chat id of the incoming call
     */
    fun onRejectBottomTap(chatId: Long) {
        removeIncomingCallNotification(chatId)
        if (isOneToOneCall()) {
            checkClickEndButton()
        } else {
            ignoreCall()
        }
    }

    /**
     * Sets snackbarMessage in state as consumed.
     */
    fun onSnackbarMessageConsumed() =
        _state.update { state -> state.copy(snackbarMessage = consumed()) }
}
