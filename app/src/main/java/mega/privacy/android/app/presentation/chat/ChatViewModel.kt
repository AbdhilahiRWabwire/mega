package mega.privacy.android.app.presentation.chat

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.chat.model.ChatStateLegacy
import mega.privacy.android.app.presentation.extensions.getErrorStringId
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.extensions.isPast
import mega.privacy.android.app.usecase.call.EndCallUseCase
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.RichLinkConfig
import mega.privacy.android.domain.entity.chat.ScheduledMeetingChanges
import mega.privacy.android.domain.entity.contacts.ContactLink
import mega.privacy.android.domain.entity.meeting.CallNotificationType
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.ChatCallTermCodeType
import mega.privacy.android.domain.entity.meeting.ChatSessionChanges
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingStatus
import mega.privacy.android.domain.entity.statistics.EndCallEmptyCall
import mega.privacy.android.domain.entity.statistics.EndCallForAll
import mega.privacy.android.domain.entity.statistics.StayOnCallEmptyCall
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.BroadcastChatArchivedUseCase
import mega.privacy.android.domain.usecase.chat.LeaveChatUseCase
import mega.privacy.android.domain.usecase.chat.LoadPendingMessagesUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatArchivedUseCase
import mega.privacy.android.domain.usecase.chat.MonitorJoinedSuccessfullyUseCase
import mega.privacy.android.domain.usecase.chat.MonitorLeaveChatUseCase
import mega.privacy.android.domain.usecase.chat.link.MonitorRichLinkPreviewConfigUseCase
import mega.privacy.android.domain.usecase.chat.link.SetRichLinkWarningCounterUseCase
import mega.privacy.android.domain.usecase.contact.GetContactLinkUseCase
import mega.privacy.android.domain.usecase.contact.IsContactRequestSentUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.meeting.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.BroadcastCallRecordingConsentEventUseCase
import mega.privacy.android.domain.usecase.meeting.GetChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.meeting.HangChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorCallEndedUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorCallRecordingConsentEventUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatSessionUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.meeting.StartCallUseCase
import mega.privacy.android.domain.usecase.meeting.StartChatCallNoRingingUseCase
import mega.privacy.android.domain.usecase.meeting.StartMeetingInWaitingRoomChatUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import nz.mega.sdk.MegaChatError
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for [ChatActivity]
 *
 * @property monitorStorageStateEventUseCase                [MonitorStorageStateEventUseCase]
 * @property startCallUseCase                               [startCallUseCase]
 * @property answerChatCallUseCase                          [AnswerChatCallUseCase]
 * @property passcodeManagement                             [PasscodeManagement]
 * @property setChatVideoInDeviceUseCase                    [SetChatVideoInDeviceUseCase]
 * @property chatManagement                                 [ChatManagement]
 * @property rtcAudioManagerGateway                         [RTCAudioManagerGateway]
 * @property startChatCallNoRingingUseCase                  [StartChatCallNoRingingUseCase]
 * @property startMeetingInWaitingRoomChatUseCase           [StartMeetingInWaitingRoomChatUseCase]
 * @property getScheduledMeetingByChat                      [GetScheduledMeetingByChat]
 * @property getChatCallUseCase                             [GetChatCallUseCase]
 * @property monitorChatCallUpdatesUseCase                  [MonitorChatCallUpdatesUseCase]
 * @property endCallUseCase                                 [EndCallUseCase]
 * @property sendStatisticsMeetingsUseCase                  [SendStatisticsMeetingsUseCase]
 * @property isConnected                                    True if the app has some network connection, false otherwise.
 * @property monitorUpdatePushNotificationSettingsUseCase   monitors push notification settings update
 * @property deviceGateway                                  [DeviceGateway]
 * @property getChatRoomUseCase                                    [GetChatRoomUseCase]
 * @property getFeatureFlagValueUseCase                     [GetFeatureFlagValueUseCase]
 * @property monitorChatArchivedUseCase                     [MonitorChatArchivedUseCase]
 * @property broadcastChatArchivedUseCase                   [BroadcastChatArchivedUseCase]
 * @property monitorJoinedSuccessfullyUseCase               [MonitorJoinedSuccessfullyUseCase]
 * @property monitorLeaveChatUseCase                        [MonitorLeaveChatUseCase]
 * @property monitorScheduledMeetingUpdates                 [MonitorScheduledMeetingUpdatesUseCase]
 * @property monitorChatRoomUpdates                         [MonitorChatRoomUpdates]
 * @property leaveChatUseCase                               [LeaveChatUseCase]
 * @property monitorChatSessionUpdatesUseCase               [MonitorChatSessionUpdatesUseCase]
 * @property hangChatCallUseCase                            [HangChatCallUseCase]
 * @property broadcastCallRecordingConsentEventUseCase      [BroadcastCallRecordingConsentEventUseCase]
 * @property monitorCallRecordingConsentEventUseCase        [MonitorCallRecordingConsentEventUseCase]
 * @property monitorCallEndedUseCase                        [MonitorCallEndedUseCase]
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val startCallUseCase: StartCallUseCase,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val answerChatCallUseCase: AnswerChatCallUseCase,
    private val passcodeManagement: PasscodeManagement,
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase,
    private val chatManagement: ChatManagement,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val startChatCallNoRingingUseCase: StartChatCallNoRingingUseCase,
    private val startMeetingInWaitingRoomChatUseCase: StartMeetingInWaitingRoomChatUseCase,
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat,
    private val getChatCallUseCase: GetChatCallUseCase,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val endCallUseCase: EndCallUseCase,
    private val sendStatisticsMeetingsUseCase: SendStatisticsMeetingsUseCase,
    private val monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase,
    private val deviceGateway: DeviceGateway,
    private val monitorChatArchivedUseCase: MonitorChatArchivedUseCase,
    private val broadcastChatArchivedUseCase: BroadcastChatArchivedUseCase,
    private val monitorJoinedSuccessfullyUseCase: MonitorJoinedSuccessfullyUseCase,
    private val monitorLeaveChatUseCase: MonitorLeaveChatUseCase,
    private val leaveChatUseCase: LeaveChatUseCase,
    private val getContactLinkUseCase: GetContactLinkUseCase,
    private val isContactRequestSentUseCase: IsContactRequestSentUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val loadPendingMessagesUseCase: LoadPendingMessagesUseCase,
    private val monitorScheduledMeetingUpdates: MonitorScheduledMeetingUpdatesUseCase,
    private val monitorChatRoomUpdates: MonitorChatRoomUpdates,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val monitorChatSessionUpdatesUseCase: MonitorChatSessionUpdatesUseCase,
    private val hangChatCallUseCase: HangChatCallUseCase,
    private val broadcastCallRecordingConsentEventUseCase: BroadcastCallRecordingConsentEventUseCase,
    private val monitorCallRecordingConsentEventUseCase: MonitorCallRecordingConsentEventUseCase,
    private val monitorCallEndedUseCase: MonitorCallEndedUseCase,
    private val setRichLinkWarningCounterUseCase: SetRichLinkWarningCounterUseCase,
    monitorRichLinkPreviewConfigUseCase: MonitorRichLinkPreviewConfigUseCase,
    monitorPausedTransfersUseCase: MonitorPausedTransfersUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatStateLegacy())

    /**
     * UI State Chat
     * Flow of [ChatStateLegacy]
     */
    val state = _state.asStateFlow()

    /**
     * Check if it's 24 hour format
     */
    val is24HourFormat by lazy { deviceGateway.is24HourFormat() }

    /**
     * Get latest [StorageState] from [MonitorStorageStateEventUseCase] use case.
     * @return the latest [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEventUseCase.getState()

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent = monitorConnectivityUseCase()

    /**
     * Flow emitting true if transfers are paused globally, false otherwise
     */
    val areTransfersPausedFlow by lazy {
        monitorPausedTransfersUseCase()
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    }

    private val richLinkConfig = monitorRichLinkPreviewConfigUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, RichLinkConfig())

    /**
     * Is show rich link warning
     */
    val isShowRichLinkWarning: Boolean
        get() = richLinkConfig.value.isShowRichLinkWarning

    /**
     * Counter not now rich link warning
     */
    val counterNotNowRichLinkWarning: Int
        get() = richLinkConfig.value.counterNotNowRichLinkWarning

    /**
     * Current value of areTransfersPausedFlow
     */
    val areTransfersPaused get() = areTransfersPausedFlow.value

    val isConnected: Boolean
        get() = isConnectedToInternetUseCase()

    private val rxSubscriptions = CompositeDisposable()

    private val newPendingMessage = SingleLiveEvent<PendingMessage>()

    init {
        viewModelScope.launch {
            monitorUpdatePushNotificationSettingsUseCase().collect {
                _state.update { it.copy(isPushNotificationSettingsUpdatedEvent = true) }
            }
        }

        viewModelScope.launch {
            monitorChatArchivedUseCase().conflate().collect { chatTitle ->
                _state.update { it.copy(titleChatArchivedEvent = chatTitle) }
            }
        }

        viewModelScope.launch {
            monitorJoinedSuccessfullyUseCase().conflate().collect {
                _state.update { it.copy(isJoiningOrLeaving = false) }
            }
        }

        viewModelScope.launch {
            monitorLeaveChatUseCase().conflate().collect { chatId ->
                if (chatId != INVALID_HANDLE) {
                    if (state.value.chatId == chatId) {
                        _state.update { state ->
                            state.copy(
                                isJoiningOrLeaving = true,
                                joiningOrLeavingAction = R.string.leaving_label
                            )
                        }
                    }
                    performLeaveChat(chatId)
                }
            }
        }

        viewModelScope.launch {
            monitorCallRecordingConsentEventUseCase().conflate()
                .collect { isRecordingConsentAccepted ->
                    _state.update {
                        it.copy(
                            isSessionOnRecording = true,
                            showRecordingConsentDialog = false,
                            isRecordingConsentAccepted = isRecordingConsentAccepted
                        )
                    }
                    if (!isRecordingConsentAccepted) {
                        hangChatCall(state.value.chatId)
                    }
                }
        }

        viewModelScope.launch {
            monitorCallEndedUseCase().conflate().collect { chatId ->
                if (chatId == state.value.chatId) {
                    resetCallRecordingState()
                }
            }
        }

        startMonitorChatSessionUpdates()
    }

    override fun onCleared() {
        rxSubscriptions.clear()
        super.onCleared()
    }

    /**
     * Check if given feature flag is enabled or not
     */
    fun isFeatureEnabled(feature: Feature) = state.value.enabledFeatureFlags.contains(feature)

    /**
     * Leave a chat
     *
     * @param chatId    [Long] ID of the chat to leave.
     */
    private fun performLeaveChat(chatId: Long) =
        viewModelScope.launch {
            runCatching {
                chatManagement.addLeavingChatId(chatId)
                leaveChatUseCase(chatId)
            }.onFailure { exception ->
                chatManagement.removeLeavingChatId(chatId)
                if (exception is MegaException) {
                    _state.update { state -> state.copy(snackbarMessage = exception.getErrorStringId()) }
                }
            }.onSuccess {
                chatManagement.removeLeavingChatId(chatId)
                setIsJoiningOrLeaving(false, null)
            }
        }

    /**
     * Sets snackbarMessage in state as consumed.
     */
    fun onSnackbarMessageConsumed() =
        _state.update { state -> state.copy(snackbarMessage = null) }

    /**
     * Call button clicked
     *
     * @param video True, video on. False, video off.
     * @param shouldCallRing True, calls should ring. False, otherwise.
     */
    fun onCallTap(video: Boolean, shouldCallRing: Boolean) {
        MegaApplication.isWaitingForCall = false
        viewModelScope.launch { setChatVideoInDeviceUseCase() }

        val isWaitingRoom = _state.value.isWaitingRoom
        val isHost = _state.value.isHost
        val hasSchedMeeting = _state.value.scheduledMeeting != null

        when {
            isWaitingRoom -> {
                when {
                    isHost && state.value.scheduledMeetingStatus is ScheduledMeetingStatus.NotJoined -> {
                        answerCall(
                            _state.value.chatId,
                            video = false,
                            audio = true
                        )
                    }

                    isHost && state.value.scheduledMeetingStatus is ScheduledMeetingStatus.NotStarted -> {
                        val schedIdWr: Long =
                            if (!hasSchedMeeting || shouldCallRing) -1L else state.value.scheduledMeeting?.schedId
                                ?: -1L
                        startSchedMeetingWithWaitingRoom(schedIdWr)
                    }

                    !isHost -> {
                        _state.update {
                            it.copy(
                                openWaitingRoomScreen = true
                            )
                        }
                    }
                }

            }

            _state.value.scheduledMeetingStatus is ScheduledMeetingStatus.NotStarted ->
                if (shouldCallRing)
                    startCall(video = video)
                else
                    startSchedMeeting()

            _state.value.scheduledMeetingStatus is ScheduledMeetingStatus.NotJoined ->
                answerCall(
                    _state.value.chatId,
                    video = false,
                    audio = true
                )

            !hasSchedMeeting ->
                viewModelScope.launch {
                    runCatching {
                        getChatCallUseCase(state.value.chatId)
                    }.onSuccess { call ->
                        when (call) {
                            null -> startCall(video = video)
                            else -> answerCall(state.value.chatId, video = video, audio = true)
                        }
                    }
                }
        }
    }

    /**
     * Sets open waiting room as consumed.
     */
    fun setOpenWaitingRoomConsumed() {
        _state.update { state -> state.copy(openWaitingRoomScreen = false) }
    }

    /**
     * Set if the chat has been initialised.
     *
     * @param value  True, if the chat has been initialised. False, otherwise.
     */
    fun setChatInitialised(value: Boolean) {
        _state.update { it.copy(isChatInitialised = value) }
    }

    /**
     * Check if the chat has been initialised.
     *
     * @return True, if the chat has been initialised. False, otherwise.
     */
    fun isChatInitialised(): Boolean = state.value.isChatInitialised

    /**
     * Check if joining or leaving the chat.
     *
     * @return True, if user is joining or leaving the chat. False, otherwise.
     */
    fun isJoiningOrLeaving(): Boolean = _state.value.isJoiningOrLeaving

    /**
     * Set if the user is joining or leaving the chat.
     *
     * @param value True, if user is joining or leaving the chat. False, otherwise.
     */
    fun setIsJoiningOrLeaving(value: Boolean, @StringRes actionId: Int?) {
        _state.update { it.copy(isJoiningOrLeaving = value, joiningOrLeavingAction = actionId) }
    }

    /**
     * Sets chat id
     *
     * @param newChatId   Chat id.
     */
    fun setChatId(newChatId: Long) {
        if (newChatId != INVALID_HANDLE && newChatId != state.value.chatId) {
            _state.update {
                it.copy(
                    chatId = newChatId
                )
            }
            getChat()
            getScheduledMeeting()
            getScheduledMeetingUpdates()
        }
    }

    /**
     * Get chat room
     */
    private fun getChat() =
        viewModelScope.launch {
            runCatching {
                getChatRoomUseCase(state.value.chatId)
            }.onFailure { exception ->
                Timber.e("Chat room does not exist, finish $exception")
            }.onSuccess { chatRoom ->
                Timber.d("Chat room exists")
                chatRoom?.apply {
                    if (isActive) {
                        Timber.d("Chat room is active")
                        chatRoomUpdated(
                            isWaitingRoom = isWaitingRoom,
                            isHost = ownPrivilege == ChatRoomPermission.Moderator
                        )
                    }
                }
            }
        }

    /**
     * Update waiting room and host values
     */
    fun chatRoomUpdated(
        isWaitingRoom: Boolean = state.value.isWaitingRoom,
        isHost: Boolean = state.value.isHost,
    ) {
        _state.update { state ->
            state.copy(
                isWaitingRoom = isWaitingRoom,
                isHost = isHost
            )
        }
    }


    /**
     * Get scheduled meeting
     */
    private fun getScheduledMeeting() =
        viewModelScope.launch {
            runCatching {
                getScheduledMeetingByChat(state.value.chatId)
            }.onFailure {
                Timber.d("Scheduled meeting does not exist")
                _state.update {
                    it.copy(
                        scheduledMeetingStatus = null,
                        scheduledMeeting = null
                    )
                }
            }.onSuccess { scheduledMeetingList ->
                scheduledMeetingList?.let { list ->
                    list.forEach { scheduledMeetReceived ->
                        if (scheduledMeetReceived.parentSchedId == INVALID_HANDLE) {
                            var scheduledMeetingStatus: ScheduledMeetingStatus =
                                ScheduledMeetingStatus.NotStarted
                            if (!scheduledMeetReceived.isPast()) {
                                Timber.d("Has scheduled meeting")
                                getChatCallUseCase(scheduledMeetReceived.chatId)?.let { call ->
                                    scheduledMeetingStatus = when (call.status) {
                                        ChatCallStatus.UserNoPresent ->
                                            ScheduledMeetingStatus.NotJoined(call.duration)

                                        ChatCallStatus.Connecting,
                                        ChatCallStatus.Joining,
                                        ChatCallStatus.InProgress,
                                        -> ScheduledMeetingStatus.Joined(call.duration)

                                        else -> ScheduledMeetingStatus.NotStarted
                                    }
                                }
                            }

                            _state.update {
                                it.copy(
                                    schedIsPending = !scheduledMeetReceived.isPast(),
                                    scheduledMeetingStatus = scheduledMeetingStatus,
                                    scheduledMeeting = scheduledMeetReceived
                                )
                            }
                            return@forEach
                        }
                    }
                }

                getChatCallUpdates()
            }
        }

    /**
     * Get scheduled meeting updates
     */
    private fun getScheduledMeetingUpdates() =
        viewModelScope.launch {
            monitorScheduledMeetingUpdates().collectLatest { scheduledMeetReceived ->
                if (state.value.chatId != scheduledMeetReceived.chatId) {
                    return@collectLatest
                }

                if (scheduledMeetReceived.parentSchedId == INVALID_HANDLE) {
                    return@collectLatest
                }

                scheduledMeetReceived.changes?.let { changes ->
                    Timber.d("Monitor scheduled meeting updated, changes ${scheduledMeetReceived.changes}")
                    changes.forEach {
                        when (it) {
                            ScheduledMeetingChanges.NewScheduledMeeting,
                            ScheduledMeetingChanges.Title,
                            ->
                                _state.update { state ->
                                    state.copy(
                                        schedIsPending = !scheduledMeetReceived.isPast(),
                                        scheduledMeeting = scheduledMeetReceived
                                    )
                                }

                            else -> {}
                        }
                    }
                }
            }
        }

    /**
     * Get chat call updates
     */
    private fun getChatCallUpdates() =
        viewModelScope.launch {
            monitorChatCallUpdatesUseCase()
                .filter { it.chatId == _state.value.chatId }
                .collectLatest { call ->
                    call.changes?.apply {
                        Timber.d("Monitor chat call updated, changes ${call.changes}")
                        if (contains(ChatCallChanges.Status) && _state.value.schedIsPending) {
                            val scheduledMeetingStatus = when (call.status) {
                                ChatCallStatus.UserNoPresent ->
                                    ScheduledMeetingStatus.NotJoined(call.duration)

                                ChatCallStatus.Connecting,
                                ChatCallStatus.Joining,
                                ChatCallStatus.InProgress,
                                -> ScheduledMeetingStatus.Joined(call.duration)

                                ChatCallStatus.TerminatingUserParticipation -> {
                                    when (call.termCode) {
                                        ChatCallTermCodeType.TooManyParticipants -> {
                                            _state.update {
                                                it.copy(snackbarMessage = R.string.call_error_too_many_participants)
                                            }
                                            ScheduledMeetingStatus.NotJoined(call.duration)
                                        }

                                        ChatCallTermCodeType.ProtocolVersion -> {
                                            showForceUpdateDialog()
                                            ScheduledMeetingStatus.NotStarted
                                        }

                                        else -> {
                                            ScheduledMeetingStatus.NotStarted
                                        }
                                    }
                                }

                                else -> ScheduledMeetingStatus.NotStarted
                            }
                            _state.update {
                                it.copy(
                                    scheduledMeetingStatus = scheduledMeetingStatus
                                )
                            }
                        } else if (contains(ChatCallChanges.GenericNotification)) {
                            if (call.notificationType == CallNotificationType.SFUError && call.termCode == ChatCallTermCodeType.ProtocolVersion) {
                                showForceUpdateDialog()
                            }
                        }
                    }
                }
        }

    /**
     * Show force update dialog
     */
    private fun showForceUpdateDialog() {
        _state.update {
            it.copy(
                showForceUpdateDialog = true
            )
        }
    }

    /**
     * Set to consumed when showForceUpdateDialog event is processed.
     */
    fun onForceUpdateDialogConsumed() {
        _state.update {
            it.copy(
                showForceUpdateDialog = false
            )
        }
    }

    /**
     * Start call
     *
     * @param video True, video on. False, video off.
     */
    private fun startCall(video: Boolean) = viewModelScope.launch {
        Timber.d("Start call")
        runCatching {
            startCallUseCase(chatId = _state.value.chatId, video = video)
        }.onSuccess { call ->
            call?.let {
                Timber.d("Call started")
                openCurrentCall(call = it, isRinging = true)
            }
        }.onFailure {
            Timber.e("Exception opening or starting call: $it")
        }
    }

    /**
     * Start scheduled meeting with waiting room
     *
     * @param schedIdWr   Scheduled meeting id
     */
    private fun startSchedMeetingWithWaitingRoom(schedIdWr: Long) =
        viewModelScope.launch {
            Timber.d("Start scheduled meeting with waiting room schedIdWr")
            runCatching {
                startMeetingInWaitingRoomChatUseCase(
                    chatId = _state.value.chatId,
                    schedIdWr = schedIdWr,
                    enabledVideo = false,
                    enabledAudio = true
                )
            }.onSuccess { call ->
                call?.let {
                    call.chatId.takeIf { it != INVALID_HANDLE }
                        ?.let {
                            Timber.d("Meeting started")
                            openCurrentCall(call = call, isRinging = (schedIdWr == -1L))
                        }
                }
            }.onFailure {
                Timber.e(it)
            }
        }

    /**
     * Start scheduled meeting
     */
    private fun startSchedMeeting() =
        viewModelScope.launch {
            _state.value.scheduledMeeting?.let { sched ->
                Timber.d("Start scheduled meeting")
                runCatching {
                    startChatCallNoRingingUseCase(
                        chatId = _state.value.chatId,
                        schedId = sched.schedId,
                        enabledVideo = false,
                        enabledAudio = true
                    )
                }.onSuccess { call ->
                    call?.let {
                        call.chatId.takeIf { it != INVALID_HANDLE }
                            ?.let {
                                Timber.d("Meeting started")
                                openCurrentCall(call = call)
                            }
                    }
                }.onFailure {
                    Timber.e(it)
                }
            }
        }

    /**
     * Open current call
     *
     * @param call      [ChatCall]
     * @param isRinging True if is ringing or False otherwise
     */
    private fun openCurrentCall(call: ChatCall, isRinging: Boolean = false) {
        chatManagement.setSpeakerStatus(call.chatId, call.hasLocalVideo)
        chatManagement.setRequestSentCall(call.callId, call.isOutgoing)
        passcodeManagement.showPasscodeScreen = true
        getInstance().openCallService(call.chatId)
        _state.update {
            it.copy(
                currentCallChatId = call.chatId,
                currentCallAudioStatus = call.hasLocalAudio,
                currentCallVideoStatus = call.hasLocalVideo,
                isRingingAll = isRinging
            )
        }
    }

    /**
     * Remove current chat call
     */
    fun removeCurrentCall() {
        _state.update {
            it.copy(
                currentCallChatId = INVALID_HANDLE,
                currentCallVideoStatus = false,
                currentCallAudioStatus = false,
                isRingingAll = false
            )
        }
    }

    /**
     * Answer call
     *
     * @param chatId    Chat Id.
     * @param video     True, video on. False, video off
     * @param audio     True, audio on. False, audio off
     */
    private fun answerCall(chatId: Long, video: Boolean, audio: Boolean) {
        chatManagement.addJoiningCallChatId(chatId)

        viewModelScope.launch {
            Timber.d("Answer call")
            runCatching {
                setChatVideoInDeviceUseCase()
                answerChatCallUseCase(chatId = chatId, video = video, audio = audio)
            }.onSuccess { call ->
                call?.apply {
                    chatManagement.removeJoiningCallChatId(chatId)
                    rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
                    CallUtil.clearIncomingCallNotification(callId)
                    openCurrentCall(this)
                    _state.update { it.copy(isCallAnswered = true) }
                }
            }.onFailure { error ->
                if (error is MegaException && error.errorCode == MegaChatError.ERROR_ACCESS) {
                    _state.update {
                        it.copy(
                            isWaitingRoom = true,
                            openWaitingRoomScreen = true
                        )
                    }
                } else {
                    Timber.w("Exception answering call: $error")
                }
            }
        }
    }

    /**
     * Answers a call.
     *
     * @param chatId
     * @param video True, video on. False, video off.
     * @param audio True, audio on. False, video off.
     */
    fun onAnswerCall(chatId: Long, video: Boolean, audio: Boolean) {
        if (CallUtil.amIParticipatingInThisMeeting(chatId)) {
            Timber.d("Already participating in this call")
            _state.update { it.copy(isCallAnswered = true) }
            return
        }

        if (MegaApplication.getChatManagement().isAlreadyJoiningCall(chatId)) {
            Timber.d("The call has been answered")
            _state.update { it.copy(isCallAnswered = true) }
            return
        }

        answerCall(chatId, video, audio)
    }

    /**
     * Control when Stay on call option is chosen
     */
    fun checkStayOnCall() {
        MegaApplication.getChatManagement().stopCounterToFinishCall()
        MegaApplication.getChatManagement().hasEndCallDialogBeenIgnored = true

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
        viewModelScope.launch {
            runCatching {
                getChatCallUseCase(state.value.chatId)
            }.onFailure { exception ->
                Timber.e("Call does not exist $exception")
            }.onSuccess { call ->
                Timber.d("Call exists")
                call?.apply {
                    endCallUseCase.hangCall(call.callId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(onError = { error ->
                            Timber.e(error.stackTraceToString())
                        })
                        .addTo(rxSubscriptions)
                }
            }
        }

        viewModelScope.launch {
            kotlin.runCatching {
                sendStatisticsMeetingsUseCase(EndCallEmptyCall())
            }
        }
    }

    /**
     * End for all the current call
     */
    fun endCallForAll() {
        endCallUseCase.run {
            endCallForAllWithChatId(_state.value.chatId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onError = { error ->
                    Timber.e(error.stackTraceToString())
                })
                .addTo(rxSubscriptions)
        }

        viewModelScope.launch {
            kotlin.runCatching {
                sendStatisticsMeetingsUseCase(EndCallForAll())
            }
        }
    }

    /**
     * on Consume Push notification settings updated event
     */
    fun onConsumePushNotificationSettingsUpdateEvent() {
        viewModelScope.launch {
            _state.update { it.copy(isPushNotificationSettingsUpdatedEvent = false) }
        }
    }

    /**
     * Get scheduled meeting
     *
     * @return  [ChatScheduledMeeting]
     */
    fun getMeeting(): ChatScheduledMeeting? =
        _state.value.scheduledMeeting

    /**
     * Check if it's a pending scheduled meeting
     *
     * @return  True, if it's pending scheduled meeting. False, if not.
     */
    fun isPendingMeeting() =
        _state.value.schedIsPending

    /**
     * Launch broadcast for a chat archived event
     *
     * @param chatTitle [String]
     */
    fun launchBroadcastChatArchived(chatTitle: String) = viewModelScope.launch {
        broadcastChatArchivedUseCase(chatTitle)
    }

    /**
     * Consume chat archive event
     */
    fun onChatArchivedEventConsumed() =
        _state.update { it.copy(titleChatArchivedEvent = null) }

    /**
     * Get contact link by handle
     */
    fun getContactLinkByHandle(
        userHandle: Long,
        onLoadContactLink: (contactLink: ContactLink) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                getContactLinkUseCase(userHandle)
            }.onSuccess { contactLink ->
                onLoadContactLink(contactLink)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Check contact request sent
     *
     * @param email
     * @param name
     */
    fun checkContactRequestSent(email: String, name: String) {
        viewModelScope.launch {
            runCatching {
                isContactRequestSentUseCase(email)
            }.onSuccess { isSent ->
                _state.update {
                    it.copy(
                        contactInvitation = ContactInvitation(
                            isSent = isSent,
                            email = email,
                            name = name
                        )
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * On contact invitation consumed
     */
    fun onContactInvitationConsumed() {
        _state.update { it.copy(contactInvitation = null) }
    }

    /**
     * Load pending messages.
     *
     */
    fun loadPendingMessages() = viewModelScope.launch {
        loadPendingMessagesUseCase(state.value.chatId)
            .collect { pendingMessage -> newPendingMessage.value = pendingMessage }
    }


    /**
     * On pending message loaded
     *
     * @return
     */
    fun onPendingMessageLoaded(): LiveData<PendingMessage> = newPendingMessage

    companion object {
        private const val INVALID_HANDLE = -1L
    }

    /**
     * Check whether the call option should be enabled. It depends on several factors:
     * - If the Waiting Room is enabled or not.
     * - The rol of the participant (host or non-host).
     *
     * @return True if the call option should be enabled or False otherwise.
     */
    fun shouldEnableCallOption(): Boolean = !_state.value.isWaitingRoom || _state.value.isHost

    /**
     * Start or answer a meeting of other chat room with waiting room as a host
     *
     * @param chatId   Chat ID
     */
    fun startOrAnswerMeetingOfOtherChatRoomWithWaitingRoomAsHost(chatId: Long) {
        viewModelScope.launch {
            runCatching {
                val call = getChatCallUseCase(chatId)
                val scheduledMeetingStatus = when (call?.status) {
                    ChatCallStatus.UserNoPresent -> ScheduledMeetingStatus.NotJoined(call.duration)

                    ChatCallStatus.Connecting,
                    ChatCallStatus.Joining,
                    ChatCallStatus.InProgress,
                    -> ScheduledMeetingStatus.Joined(call.duration)

                    else -> ScheduledMeetingStatus.NotStarted
                }
                if (scheduledMeetingStatus is ScheduledMeetingStatus.NotStarted) {
                    runCatching {
                        getScheduledMeetingByChat(chatId)
                    }.onSuccess { scheduledMeetingList ->
                        scheduledMeetingList?.first()?.schedId?.let { schedId ->
                            startSchedMeetingOfOtherChatRoomWithWaitingRoom(
                                chatId = chatId, schedIdWr = schedId
                            )
                        }
                    }.onFailure { exception ->
                        Timber.e(exception)
                    }
                } else {
                    answerSchedMeetingOfOtherChatRoom(chatId = chatId)
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Start scheduled meeting of other chat room with waiting room
     *
     * @param chatId    Chat ID
     * @param schedIdWr Scheduled meeting ID
     */
    private fun startSchedMeetingOfOtherChatRoomWithWaitingRoom(chatId: Long, schedIdWr: Long) =
        viewModelScope.launch {
            Timber.d("Start scheduled meeting with waiting room")
            runCatching {
                startMeetingInWaitingRoomChatUseCase(
                    chatId = chatId,
                    schedIdWr = schedIdWr,
                    enabledVideo = false,
                    enabledAudio = true
                )
            }.onSuccess { call ->
                call?.let {
                    call.chatId.takeIf { it != INVALID_HANDLE }?.let {
                        Timber.d("Meeting started")
                        openCall(call)
                    }
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }

    /**
     * Answer scheduled meeting of other chat room
     *
     * @param chatId    Chat Id.
     */
    private fun answerSchedMeetingOfOtherChatRoom(chatId: Long) {
        chatManagement.addJoiningCallChatId(chatId)

        viewModelScope.launch {
            Timber.d("Answer call")
            runCatching {
                setChatVideoInDeviceUseCase()
                answerChatCallUseCase(chatId = chatId, video = false, audio = true)
            }.onSuccess { call ->
                call?.apply {
                    chatManagement.removeJoiningCallChatId(chatId)
                    rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
                    CallUtil.clearIncomingCallNotification(callId)
                    openCall(call)
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Open call
     *
     * @param call  [ChatCall]
     */
    private fun openCall(call: ChatCall) {
        chatManagement.setSpeakerStatus(call.chatId, call.hasLocalVideo)
        chatManagement.setRequestSentCall(call.callId, call.isOutgoing)
        CallUtil.openMeetingInProgress(
            getInstance().applicationContext,
            call.chatId,
            true,
            passcodeManagement,
            state.value.isSessionOnRecording
        )
    }

    /**
     * Sets showRecordingConsentDialog as consumed.
     */
    fun setShowRecordingConsentDialogConsumed() =
        _state.update { state -> state.copy(showRecordingConsentDialog = false) }

    /**
     * Sets isRecordingConsentAccepted.
     */
    fun setIsRecordingConsentAccepted(value: Boolean) {
        _state.update { state -> state.copy(isRecordingConsentAccepted = value) }
        launchBroadcastCallRecordingConsentEvent(isRecordingConsentAccepted = value)
    }

    /**
     * Sets isSessionOnRecording.
     */
    fun setIsSessionOnRecording(value: Boolean) {
        _state.update { state -> state.copy(isSessionOnRecording = value) }
    }

    /**
     * Hang chat call
     */
    fun hangChatCall(chatId: Long) = viewModelScope.launch {
        runCatching {
            getChatCallUseCase(chatId)?.let { chatCall ->
                hangChatCallUseCase(chatCall.callId)
            }
        }.onSuccess {
            resetCallRecordingState()
        }.onFailure { exception ->
            Timber.e(exception)
        }
    }

    /**
     * Monitor chat session updates
     */
    fun startMonitorChatSessionUpdates() {
        viewModelScope.launch {
            monitorChatSessionUpdatesUseCase()
                .filter { it.chatId == state.value.chatId }
                .collectLatest { result ->
                    result.session?.let { session ->
                        session.changes?.apply {
                            if (contains(ChatSessionChanges.SessionOnRecording)) {
                                _state.update { state ->
                                    state.copy(
                                        isSessionOnRecording = session.isRecording,
                                        showRecordingConsentDialog = if (!state.isRecordingConsentAccepted) session.isRecording else false
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }

    /**
     * Launch broadcast for recording consent event (accepted/rejected)
     *
     * @param isRecordingConsentAccepted True if recording consent has been accepted or False otherwise.
     */
    private fun launchBroadcastCallRecordingConsentEvent(isRecordingConsentAccepted: Boolean) =
        viewModelScope.launch {
            broadcastCallRecordingConsentEventUseCase(isRecordingConsentAccepted)
        }

    /**
     * Reset call recording status properties
     */
    fun resetCallRecordingState() {
        _state.update {
            it.copy(
                isSessionOnRecording = false,
                showRecordingConsentDialog = false,
                isRecordingConsentAccepted = false
            )
        }
    }

    /**
     * Set rich link warning counter
     *
     */
    fun setRichLinkWarningCounter(counter: Int) {
        viewModelScope.launch {
            runCatching {
                setRichLinkWarningCounterUseCase(counter)
            }.onFailure {
                Timber.e(it)
            }
        }
    }
}
