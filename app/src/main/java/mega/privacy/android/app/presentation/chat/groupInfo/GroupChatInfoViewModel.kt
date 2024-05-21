package mega.privacy.android.app.presentation.chat.groupInfo

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.chat.groupInfo.model.GroupInfoState
import mega.privacy.android.app.presentation.meeting.model.MeetingState.Companion.FREE_PLAN_PARTICIPANTS_LIMIT
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.CallUtil.openMeetingWithAudioOrVideo
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.ChatRequestParamType
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.statistics.EndCallForAll
import mega.privacy.android.domain.usecase.SetOpenInvite
import mega.privacy.android.domain.usecase.chat.BroadcastChatArchivedUseCase
import mega.privacy.android.domain.usecase.chat.BroadcastLeaveChatUseCase
import mega.privacy.android.domain.usecase.chat.EndCallUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.meeting.GetChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorSFUServerUpgradeUseCase
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.meeting.StartChatCall
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import nz.mega.sdk.MegaChatRoom
import timber.log.Timber
import javax.inject.Inject

/**
 * GroupChatInfoActivity view model.
 *
 * @property setOpenInvite                                  [SetOpenInvite]
 * @property startChatCall                                  [StartChatCall]
 * @property passcodeManagement                             [PasscodeManagement]
 * @property chatApiGateway                                 [MegaChatApiGateway]
 * @property setChatVideoInDeviceUseCase                    [SetChatVideoInDeviceUseCase]
 * @property chatManagement                                 [ChatManagement]
 * @property endCallUseCase                                 [EndCallUseCase]
 * @property sendStatisticsMeetingsUseCase                  [SendStatisticsMeetingsUseCase]
 * @property monitorUpdatePushNotificationSettingsUseCase   [MonitorUpdatePushNotificationSettingsUseCase]
 * @property broadcastChatArchivedUseCase                   [BroadcastChatArchivedUseCase]
 * @property broadcastLeaveChatUseCase                      [BroadcastLeaveChatUseCase]
 * @property get1On1ChatIdUseCase                           [Get1On1ChatIdUseCase]
 * @property state                                          Current view state as [GroupInfoState]
 */
@HiltViewModel
class GroupChatInfoViewModel @Inject constructor(
    private val setOpenInvite: SetOpenInvite,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val startChatCall: StartChatCall,
    private val passcodeManagement: PasscodeManagement,
    private val chatApiGateway: MegaChatApiGateway,
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase,
    private val chatManagement: ChatManagement,
    private val endCallUseCase: EndCallUseCase,
    private val sendStatisticsMeetingsUseCase: SendStatisticsMeetingsUseCase,
    private val monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase,
    private val broadcastChatArchivedUseCase: BroadcastChatArchivedUseCase,
    private val broadcastLeaveChatUseCase: BroadcastLeaveChatUseCase,
    private val monitorSFUServerUpgradeUseCase: MonitorSFUServerUpgradeUseCase,
    private val get1On1ChatIdUseCase: Get1On1ChatIdUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {

    /**
     * private UI state
     */
    private val _state = MutableStateFlow(GroupInfoState())

    private var monitorSFUServerUpgradeJob: Job? = null
    private var monitorChatCallJob: Job? = null

    /**
     * UI State GroupChatInfo
     * Flow of [GroupInfoState]
     */
    val state = _state.asStateFlow()

    private val isConnected =
        monitorConnectivityUseCase().stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val openInviteChangeObserver =
        Observer<MegaChatRoom> { chat ->
            _state.update {
                it.copy(resultSetOpenInvite = chat.isOpenInvite)
            }
        }

    init {
        LiveEventBus.get(EventConstants.EVENT_CHAT_OPEN_INVITE, MegaChatRoom::class.java)
            .observeForever(openInviteChangeObserver)
        viewModelScope.launch {
            monitorUpdatePushNotificationSettingsUseCase().collect {
                _state.update { it.copy(isPushNotificationSettingsUpdatedEvent = true) }
            }
        }
        monitorSFUServerUpgrade()
        viewModelScope.launch {
            getFeatureFlagValueUseCase(AppFeatures.CallUnlimitedProPlan).let { flag ->
                _state.update { state ->
                    state.copy(
                        isCallUnlimitedProPlanFeatureFlagEnabled = flag,
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        LiveEventBus.get(EventConstants.EVENT_CHAT_OPEN_INVITE, MegaChatRoom::class.java)
            .removeObserver(openInviteChangeObserver)
    }

    /**
     * Sets chat id
     *
     * @param newChatId   Chat id.
     */
    fun setChatId(newChatId: Long) {
        if (newChatId != chatApiGateway.getChatInvalidHandle() && newChatId != state.value.chatId) {
            _state.update {
                it.copy(
                    chatId = newChatId
                )
            }
            getChatCall()
        }
    }

    /**
     * Allow add participants
     */
    fun onAllowAddParticipantsTap(chatId: Long) {
        if (isConnected.value) {
            viewModelScope.launch {
                runCatching {
                    setOpenInvite(chatId)
                }.onFailure { exception ->
                    Timber.e(exception)
                    _state.update { it.copy(error = R.string.general_text_error) }
                }.onSuccess { result ->
                    _state.update {
                        it.copy(resultSetOpenInvite = result)
                    }
                }
            }
        } else {
            _state.update { it.copy(error = R.string.check_internet_connection_error) }
        }
    }

    /**
     * Method for processing when clicking on the call option
     *
     * @param userHandle Use handle
     * @param video Start call with video on or off
     * @param audio Start call with audio on or off
     */
    fun onCallTap(userHandle: Long, video: Boolean, audio: Boolean) = viewModelScope.launch {
        runCatching {
            get1On1ChatIdUseCase(userHandle)
        }.onSuccess { chatId ->
            startCall(chatId, video, audio)
        }.onFailure {
            Timber.e(it)
        }
    }

    /**
     * Starts a call
     *
     * @param chatId Chat id
     * @param video Start call with video on or off
     * @param audio Start call with audio on or off
     */
    private fun startCall(chatId: Long, video: Boolean, audio: Boolean) {
        if (chatApiGateway.getChatCall(chatId) != null) {
            Timber.d("There is a call, open it")
            CallUtil.openMeetingInProgress(
                MegaApplication.getInstance().applicationContext,
                chatId,
                true,
                passcodeManagement
            )
            return
        }

        MegaApplication.isWaitingForCall = false

        viewModelScope.launch {
            runCatching {
                setChatVideoInDeviceUseCase()
                startChatCall(chatId, video, audio)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess { resultStartCall ->
                val resultChatId = resultStartCall.chatHandle
                val videoEnable = resultStartCall.flag
                val paramType = resultStartCall.paramType
                val audioEnable: Boolean = paramType == ChatRequestParamType.Video

                CallUtil.addChecksForACall(resultChatId, videoEnable)

                chatApiGateway.getChatCall(resultChatId)?.let { call ->
                    if (call.isOutgoing) {
                        chatManagement.setRequestSentCall(call.callId, true)
                    }
                }

                openMeetingWithAudioOrVideo(
                    MegaApplication.getInstance().applicationContext,
                    resultChatId,
                    audioEnable,
                    videoEnable,
                    passcodeManagement
                )
            }
        }
    }

    /**
     * End for all the current call
     */
    fun endCallForAll() = viewModelScope.launch {
        runCatching {
            endCallUseCase(_state.value.chatId)
            sendStatisticsMeetingsUseCase(EndCallForAll())
        }.onFailure {
            Timber.e(it.stackTraceToString())
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
     * Launch broadcast for a chat archived event
     *
     * @param chatTitle [String]
     */
    fun launchBroadcastChatArchived(chatTitle: String) = viewModelScope.launch {
        broadcastChatArchivedUseCase(chatTitle)
    }

    /**
     * Launch broadcast notifying that should leave a chat
     *
     * @param chatId [Long] ID of the chat to leave.
     */
    fun launchBroadcastLeaveChat(chatId: Long) = viewModelScope.launch {
        broadcastLeaveChatUseCase(chatId)
    }

    /**
     * monitor SFU Server Upgrade
     */
    private fun monitorSFUServerUpgrade() {
        monitorSFUServerUpgradeJob?.cancel()
        monitorSFUServerUpgradeJob = viewModelScope.launch {
            monitorSFUServerUpgradeUseCase()
                .catch {
                    Timber.e(it)
                }
                .collect { shouldUpgrade ->
                    if (shouldUpgrade) {
                        showForceUpdateDialog()
                    }
                }
        }
    }

    private fun showForceUpdateDialog() {
        _state.update { it.copy(showForceUpdateDialog = true) }
    }

    /**
     * Set to false to hide the dialog
     */
    fun onForceUpdateDialogDismissed() {
        _state.update { it.copy(showForceUpdateDialog = false) }
    }

    /**
     * Get chat call updates
     */
    private fun getChatCall() {
        viewModelScope.launch {
            runCatching {
                getChatCallUseCase(_state.value.chatId)?.let { call ->
                    setShouldShowUserLimitsWarning(call)
                    monitorChatCall(call.callId)
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun monitorChatCall(callId:Long) {
        monitorChatCallJob?.cancel()
        monitorChatCallJob = viewModelScope.launch {
            monitorChatCallUpdatesUseCase()
                .filter { it.callId == callId }
                .catch {
                    Timber.e(it)
                }
                .collect { call ->
                    setShouldShowUserLimitsWarning(call)
                    call.changes?.apply {
                        if (contains(ChatCallChanges.Status)) {
                            Timber.d("Chat call status: ${call.status}")
                            when (call.status) {
                                ChatCallStatus.Destroyed -> {
                                    // Call has ended
                                    _state.update { it.copy(shouldShowUserLimitsWarning = false) }
                                    monitorChatCallJob?.cancel()
                                }

                                else -> {}
                            }
                        }
                    }
                }
        }
    }

    private fun setShouldShowUserLimitsWarning(call: ChatCall) {
        Timber.d("Call user limit ${call.callUsersLimit} and users in call ${call.peerIdParticipants?.size}")
        if (call.callUsersLimit != -1) {
            val limit = call.callUsersLimit
                ?: FREE_PLAN_PARTICIPANTS_LIMIT
            val shouldShowWarning =
                (call.peerIdParticipants?.size
                    ?: 0) >= limit && _state.value.isCallUnlimitedProPlanFeatureFlagEnabled
            _state.update { it.copy(shouldShowUserLimitsWarning = shouldShowWarning) }
        } else {
            _state.update { it.copy(shouldShowUserLimitsWarning = false) }
        }
    }
}
