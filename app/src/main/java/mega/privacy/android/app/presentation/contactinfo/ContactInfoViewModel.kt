package mega.privacy.android.app.presentation.contactinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.contactinfo.model.ContactInfoState
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.extensions.isAwayOrOffline
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.meeting.CallNotificationType
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.entity.meeting.ChatCallChanges.OnHold
import mega.privacy.android.domain.entity.meeting.ChatCallChanges.Status
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.ChatCallTermCodeType
import mega.privacy.android.domain.entity.meeting.ChatSessionChanges
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.CreateChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.GetChatRoomByUserUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatConnectionStateUseCase
import mega.privacy.android.domain.usecase.chat.StartConversationUseCase
import mega.privacy.android.domain.usecase.contact.ApplyContactUpdatesUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromChatUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromEmailUseCase
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import mega.privacy.android.domain.usecase.contact.MonitorChatOnlineStatusUseCase
import mega.privacy.android.domain.usecase.contact.MonitorChatPresenceLastGreenUpdatesUseCase
import mega.privacy.android.domain.usecase.contact.RemoveContactByEmailUseCase
import mega.privacy.android.domain.usecase.contact.RequestUserLastGreenUseCase
import mega.privacy.android.domain.usecase.contact.SetUserAliasUseCase
import mega.privacy.android.domain.usecase.meeting.IsChatConnectedToInitiateCallUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatSessionUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.OpenOrStartCallUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.CopyNodesUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import mega.privacy.android.domain.usecase.shares.CreateShareKeyUseCase
import mega.privacy.android.domain.usecase.shares.GetInSharesUseCase
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * View Model for [ContactInfoActivity]
 *
 * @property monitorStorageStateEventUseCase    [MonitorStorageStateEventUseCase]
 * @property isConnectedToInternetUseCase       [IsConnectedToInternetUseCase]
 * @property passcodeManagement                 [PasscodeManagement]
 * @property setChatVideoInDeviceUseCase        [SetChatVideoInDeviceUseCase]
 * @property chatManagement                     [ChatManagement]
 * @property monitorContactUpdates              [MonitorContactUpdates]
 * @property requestUserLastGreenUseCase        [RequestUserLastGreenUseCase]
 * @property getChatRoomUseCase                        [GetChatRoomUseCase]
 * @property getUserOnlineStatusByHandleUseCase [GetUserOnlineStatusByHandleUseCase]
 * @property getChatRoomByUserUseCase           [GetChatRoomByUserUseCase]
 * @property getContactFromChatUseCase          [GetContactFromChatUseCase]
 * @property getContactFromEmailUseCase         [GetContactFromEmailUseCase]
 * @property applyContactUpdatesUseCase         [ApplyContactUpdatesUseCase]
 * @property setUserAliasUseCase                [SetUserAliasUseCase]
 * @property removeContactByEmailUseCase        [RemoveContactByEmailUseCase]
 * @property getInSharesUseCase                 [GetInSharesUseCase]
 */
@HiltViewModel
class ContactInfoViewModel @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val passcodeManagement: PasscodeManagement,
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase,
    private val chatManagement: ChatManagement,
    private val monitorContactUpdates: MonitorContactUpdates,
    private val getUserOnlineStatusByHandleUseCase: GetUserOnlineStatusByHandleUseCase,
    private val requestUserLastGreenUseCase: RequestUserLastGreenUseCase,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val getChatRoomByUserUseCase: GetChatRoomByUserUseCase,
    private val getContactFromChatUseCase: GetContactFromChatUseCase,
    private val getContactFromEmailUseCase: GetContactFromEmailUseCase,
    private val applyContactUpdatesUseCase: ApplyContactUpdatesUseCase,
    private val setUserAliasUseCase: SetUserAliasUseCase,
    private val removeContactByEmailUseCase: RemoveContactByEmailUseCase,
    private val getInSharesUseCase: GetInSharesUseCase,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val monitorChatSessionUpdatesUseCase: MonitorChatSessionUpdatesUseCase,
    private val monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase,
    private val startConversationUseCase: StartConversationUseCase,
    private val createChatRoomUseCase: CreateChatRoomUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val createShareKeyUseCase: CreateShareKeyUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val monitorChatConnectionStateUseCase: MonitorChatConnectionStateUseCase,
    private val monitorChatOnlineStatusUseCase: MonitorChatOnlineStatusUseCase,
    private val monitorChatPresenceLastGreenUpdatesUseCase: MonitorChatPresenceLastGreenUpdatesUseCase,
    private val isChatConnectedToInitiateCallUseCase: IsChatConnectedToInitiateCallUseCase,
    private val openOrStartCallUseCase: OpenOrStartCallUseCase,
    private val checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase,
    private val copyNodesUseCase: CopyNodesUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val _state = MutableStateFlow(ContactInfoState())

    /**
     * UI State ContactInfo
     * Flow of [ContactInfoState]
     */
    val state = _state.asStateFlow()

    /**
     * Parent Handle
     */
    var parentHandle: Long? = null

    /**
     * Checks if contact info launched from contacts screen
     */
    val isFromContacts: Boolean
        get() = state.value.isFromContacts

    /**
     * User status
     */
    val userChatStatus: UserChatStatus
        get() = state.value.userChatStatus

    /**
     * User status
     */
    val userHandle: Long?
        get() = state.value.contactItem?.handle

    /**
     * User email
     */
    val userEmail: String?
        get() = state.value.contactItem?.email

    /**
     * chat id
     */
    val chatId: Long?
        get() = state.value.chatRoom?.chatId


    /**
     * Nick name
     */
    val nickName: String?
        get() = state.value.contactItem?.contactData?.alias

    init {
        getContactUpdates()
        monitorCallUpdates()
        startMonitorChatSessionUpdates()
        chatMuteUpdates()
        monitorNodeChanges()
        monitorChatOnlineStatusUpdates()
        monitorChatPresenceGreenUpdates()
        monitorChatConnectionStateUpdates()
    }

    private fun monitorChatConnectionStateUpdates() = viewModelScope.launch {
        monitorChatConnectionStateUseCase().collectLatest {
            val shouldInitiateCall = isChatConnectedToInitiateCallUseCase(
                newState = it.chatConnectionStatus,
                chatRoom = getChatRoomUseCase(it.chatId),
                isWaitingForCall = MegaApplication.isWaitingForCall,
                userWaitingForCall = MegaApplication.userWaitingForCall,
            )
            if (shouldInitiateCall && chatId != INVALID_CHAT_HANDLE) {
                _state.update { state -> state.copy(shouldInitiateCall = true) }
            }
        }
    }

    private fun monitorChatPresenceGreenUpdates() = viewModelScope.launch {
        monitorChatPresenceLastGreenUpdatesUseCase().collectLatest {
            updateLastGreen(userHandle = it.handle, lastGreen = it.lastGreen)
        }
    }

    private fun monitorChatOnlineStatusUpdates() = viewModelScope.launch {
        monitorChatOnlineStatusUseCase().collectLatest {
            getUserStatusAndRequestForLastGreen()
        }
    }


    private fun monitorNodeChanges() = viewModelScope.launch {
        monitorNodeUpdatesUseCase()
            .filter { nodeUpdate ->
                nodeUpdate.changes.keys.any { node -> node.isIncomingShare }
                        || nodeUpdate.changes.values.any { it.contains(NodeChanges.Remove) }
            }.conflate().collect {
                getInShares()
            }
    }

    private fun chatMuteUpdates() = viewModelScope.launch {
        monitorUpdatePushNotificationSettingsUseCase().collect {
            _state.update { it.copy(isPushNotificationSettingsUpdated = true) }
        }
    }

    /**
     * Get chat session updates
     */
    private fun startMonitorChatSessionUpdates() =
        viewModelScope.launch {
            monitorChatSessionUpdatesUseCase()
                .collectLatest { result ->
                    result.session?.let { session ->

                        session.changes?.apply {
                            if (contains(ChatSessionChanges.Status) ||
                                contains(ChatSessionChanges.RemoteAvFlags) ||
                                contains(ChatSessionChanges.SessionSpeakRequested) ||
                                contains(ChatSessionChanges.SessionOnHiRes) ||
                                contains(ChatSessionChanges.SessionOnLowRes) ||
                                contains(ChatSessionChanges.SessionOnHold) ||
                                contains(ChatSessionChanges.AudioLevel)
                            ) {
                                _state.update { it.copy(callStatusChanged = true) }
                            }
                        }
                    }
                }
        }

    private fun monitorCallUpdates() = viewModelScope.launch {
        monitorChatCallUpdatesUseCase()
            .collectLatest { call ->
                call.changes?.apply {
                    if (contains(Status)) observeCallStatus(call)
                    if (shouldShowForceUpdateDialog(call)) showForceUpdateDialog()
                    if (contains(OnHold)) _state.update { it.copy(callStatusChanged = true) }
                }
            }
    }

    private fun shouldShowForceUpdateDialog(call: ChatCall): Boolean {
        Timber.d("Call status: ${call.status}, termCode: ${call.termCode}, notificationType: ${call.notificationType}")
        return if (call.changes?.contains(Status) == true) {
            call.status == ChatCallStatus.TerminatingUserParticipation && call.termCode == ChatCallTermCodeType.ProtocolVersion
        } else if (call.changes?.contains(ChatCallChanges.GenericNotification) == true) {
            call.notificationType == CallNotificationType.SFUError && call.termCode == ChatCallTermCodeType.ProtocolVersion
        } else {
            false
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

    private fun observeCallStatus(call: ChatCall) {
        if (call.status in listOf(
                ChatCallStatus.Connecting,
                ChatCallStatus.InProgress,
                ChatCallStatus.Destroyed,
                ChatCallStatus.TerminatingUserParticipation,
                ChatCallStatus.UserNoPresent
            )
        ) {
            _state.update { it.copy(callStatusChanged = true) }
            if (call.status == ChatCallStatus.TerminatingUserParticipation &&
                call.termCode == ChatCallTermCodeType.TooManyParticipants
            ) {
                _state.update { it.copy(snackBarMessage = R.string.call_error_too_many_participants) }
            }
        }
    }

    /**
     * Monitors contact changes.
     */
    private fun getContactUpdates() = viewModelScope.launch {
        monitorContactUpdates().collectLatest { updates ->
            val userInfo = state.value.contactItem?.let { applyContactUpdatesUseCase(it, updates) }
            if (updates.changes.containsValue(listOf(UserChanges.Avatar))) {
                updateAvatar(userInfo)
            } else {
                _state.update { it.copy(contactItem = userInfo) }
            }
        }
    }

    /**
     * Refreshes user info
     *
     * User online status, Credential verification, User info change will be updated
     */
    fun refreshUserInfo() = viewModelScope.launch {
        val email = state.value.email ?: return@launch
        runCatching {
            getContactFromEmailUseCase(email, isOnline())
        }.onSuccess {
            _state.update { state -> state.copy(contactItem = it) }
            it?.handle?.let { handle -> runCatching { requestUserLastGreenUseCase(handle) } }
        }
    }

    /**
     * Get latest [StorageState] from [MonitorStorageStateEventUseCase] use case.
     * @return the latest [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEventUseCase.getState()

    /**
     * Is online
     *
     * @return
     */
    fun isOnline(): Boolean = isConnectedToInternetUseCase()

    /**
     * Starts a call
     *
     * @param hasVideo Start call with video on or off
     */
    fun joinCall(hasVideo: Boolean) = viewModelScope.launch {
        val chatId = chatId ?: return@launch
        Timber.d("Start call")
        _state.update { it.copy(enableCallLayout = false, shouldInitiateCall = false) }
        MegaApplication.isWaitingForCall = false
        runCatching {
            setChatVideoInDeviceUseCase()
            openOrStartCallUseCase(chatId = chatId, video = hasVideo)
        }.onSuccess { call ->
            call?.let { chatCall ->
                Timber.d("Call started")
                openCurrentCall(call = chatCall)
            } ?: _state.update { it.copy(enableCallLayout = true) }
        }.onFailure {
            _state.update { state -> state.copy(enableCallLayout = true) }
            Timber.w("Exception opening or starting call: $it")
        }
    }

    /**
     * Open current call
     *
     * @param call  [ChatCall]
     */
    private fun openCurrentCall(call: ChatCall) {
        chatManagement.setSpeakerStatus(call.chatId, call.hasLocalVideo)
        chatManagement.setRequestSentCall(call.callId, call.isOutgoing)
        passcodeManagement.showPasscodeScreen = true
        MegaApplication.getInstance().openCallService(call.chatId)
        _state.update {
            it.copy(
                currentCallChatId = call.chatId,
                currentCallAudioStatus = call.hasLocalAudio,
                currentCallVideoStatus = call.hasLocalVideo,
                enableCallLayout = true,
            )
        }
    }

    /**
     * Gets user online status by user handle
     * Requests for lastGreen
     */
    fun getUserStatusAndRequestForLastGreen() = viewModelScope.launch {
        val handle = state.value.contactItem?.handle ?: return@launch
        runCatching { getUserOnlineStatusByHandleUseCase(handle) }.onSuccess { status ->
            if (status.isAwayOrOffline()) {
                requestUserLastGreenUseCase(handle)
            }
            _state.update { it.copy(userChatStatus = status) }
        }
    }

    /**
     * Method updates the last green status to contact info state
     *
     * @param userHandle user handle of the user
     * @param lastGreen last green status
     */
    fun updateLastGreen(userHandle: Long, lastGreen: Int) = viewModelScope.launch {
        runCatching {
            getUserOnlineStatusByHandleUseCase(userHandle = userHandle)
        }.onSuccess { status ->
            _state.update {
                it.copy(
                    lastGreen = lastGreen,
                    userChatStatus = status,
                )
            }
        }
    }

    /**
     * Method to get chat room and contact info
     *
     * @param chatHandle chat handle of selected chat
     * @param email email id of selected user
     */
    fun updateContactInfo(chatHandle: Long, email: String? = null) = viewModelScope.launch {
        val isFromContacts = chatHandle == -1L
        _state.update { it.copy(isFromContacts = isFromContacts) }
        var userInfo: ContactItem? = null
        var chatRoom: ChatRoom? = null
        if (isFromContacts) {
            runCatching {
                email?.let { mail -> getContactFromEmailUseCase(mail, isOnline()) }
            }.onSuccess { contact ->
                userInfo = contact
                chatRoom = runCatching {
                    contact?.handle?.let { getChatRoomByUserUseCase(it) }
                }.getOrNull()
            }
        } else {
            runCatching {
                chatRoom = getChatRoomUseCase(chatHandle)
                userInfo = getContactFromChatUseCase(chatHandle, isOnline())
            }
        }

        _state.update {
            it.copy(
                lastGreen = userInfo?.lastSeen ?: 0,
                userChatStatus = userInfo?.status ?: UserChatStatus.Invalid,
                contactItem = userInfo,
                chatRoom = chatRoom,
            )
        }
        updateAvatar(userInfo)
        getInShares()
    }

    private fun updateAvatar(userInfo: ContactItem?) = viewModelScope.launch(ioDispatcher) {
        val avatarFile = userInfo?.contactData?.avatarUri?.let { File(it) }
        val avatar = AvatarUtil.getAvatarBitmap(avatarFile)
        _state.update { it.copy(avatar = avatar) }
    }

    /**
     * Sets parent handle
     *
     * @param handle parent handle
     */
    fun setParentHandle(handle: Long) {
        parentHandle = handle
    }

    /**
     * Method to update the nick name of the user
     *
     * @param newNickname new nick name given by the user
     */
    fun updateNickName(newNickname: String?) = viewModelScope.launch {
        val handle = state.value.contactItem?.handle
        if (handle == null || (nickName != null && nickName == newNickname)) return@launch
        runCatching {
            setUserAliasUseCase(newNickname, handle)
        }.onSuccess { alias ->
            _state.update {
                it.copy(
                    snackBarMessage = alias?.let { R.string.snackbar_nickname_added }
                        ?: run { R.string.snackbar_nickname_removed },
                )
            }
        }
    }

    /**
     * on Consume Snack Bar Message event
     */
    fun onConsumeSnackBarMessageEvent() {
        viewModelScope.launch {
            _state.update { it.copy(snackBarMessage = null, snackBarMessageString = null) }
        }
    }

    /**
     * on Consume chat call status change
     */
    fun onConsumeChatCallStatusChangeEvent() {
        viewModelScope.launch {
            _state.update { it.copy(callStatusChanged = false) }
        }
    }

    /**
     * on Consume Push notification settings updated event
     */
    fun onConsumePushNotificationSettingsUpdateEvent() {
        viewModelScope.launch {
            _state.update { it.copy(isPushNotificationSettingsUpdated = false) }
        }
    }


    /**
     * on Consume navigate to chat activity event
     */
    fun onConsumeNavigateToChatEvent() {
        viewModelScope.launch {
            _state.update { it.copy(shouldNavigateToChat = false) }
        }
    }

    /**
     * on Consume chat notification change event
     */
    fun onConsumeChatNotificationChangeEvent() {
        viewModelScope.launch {
            _state.update { it.copy(isChatNotificationChange = false) }
        }
    }

    /**
     * on Consume storage over quota event
     */
    fun onConsumeStorageOverQuotaEvent() {
        viewModelScope.launch {
            _state.update { it.copy(isStorageOverQuota = false) }
        }
    }

    /**
     * on Consume node update event
     */
    fun onConsumeNodeUpdateEvent() {
        viewModelScope.launch {
            _state.update { it.copy(isNodeUpdated = false) }
        }
    }

    /**
     * on Consume copy exception
     */
    fun onConsumeCopyException() {
        viewModelScope.launch {
            _state.update { it.copy(copyError = null) }
        }
    }

    /**
     * on Consume copy exception
     */
    fun onConsumeNameCollisions() {
        viewModelScope.launch {
            _state.update { it.copy(nameCollisions = emptyList()) }
        }
    }

    /**
     * on Consume initiate call
     */
    fun onConsumeNavigateToMeeting() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    currentCallChatId = INVALID_CHAT_HANDLE,
                    currentCallAudioStatus = false,
                    currentCallVideoStatus = false
                )
            }
        }
    }

    /**
     * Remove selected contact from user account
     * InShares are removed and existing calls are closed
     * Exits from contact info page if succeeds
     */
    fun removeContact() = applicationScope.launch {
        val isRemoved = state.value.email?.let { email ->
            runCatching { removeContactByEmailUseCase(email) }.getOrElse {
                Timber.w("Exception removing contact.", it)
                false
            }
        } ?: false
        _state.update {
            it.copy(isUserRemoved = isRemoved)
        }
    }

    /**
     * Gets the in shared nodes for the user
     * value is updated to contact info state
     */
    fun getInShares() = viewModelScope.launch {
        val email = state.value.email ?: return@launch
        val inShares = getInSharesUseCase(email)
        if (inShares == state.value.inShares) return@launch
        parentHandle = inShares.firstOrNull()?.parentId?.longValue ?: INVALID_NODE_HANDLE
        _state.update { it.copy(inShares = inShares, isNodeUpdated = true) }
    }

    /**
     * Method handles sent message to chat click from UI
     *
     * returns if user is not online
     * updates [ContactInfoState.isStorageOverQuota] if storage state is [StorageState.PayWall]
     * creates chatroom exists else returns existing chat room
     * updates [ContactInfoState.shouldNavigateToChat] to true
     */
    fun sendMessageToChat() = viewModelScope.launch {
        if (!isOnline()) return@launch
        if (getStorageState() === StorageState.PayWall) {
            _state.update { it.copy(isStorageOverQuota = true) }
        } else {
            startConversation()
        }
    }

    private suspend fun startConversation() {
        userHandle?.let {
            runCatching {
                startConversationUseCase(isGroup = false, userHandles = listOf(it))
            }.onSuccess {
                val chatRoom = getChatRoomUseCase(it)
                chatRoom?.let {
                    _state.update { state ->
                        state.copy(chatRoom = chatRoom, shouldNavigateToChat = true)
                    }
                }
            }.onFailure {
                _state.update { state ->
                    state.copy(snackBarMessage = R.string.create_chat_error)
                }
            }
        }
    }

    /**
     * Method handles chat notification click
     *
     * Creates chatroom if chatroom is not existing
     * updates [ContactInfoState.isChatNotificationChange] to true
     */
    fun chatNotificationsClicked() = viewModelScope.launch {
        if (chatId == null || chatId == INVALID_CHAT_HANDLE) {
            createChatRoom()
        }
        _state.update { it.copy(isChatNotificationChange = true) }
    }

    private suspend fun createChatRoom() {
        userHandle?.let {
            runCatching {
                createChatRoomUseCase(isGroup = false, userHandles = listOf(it))
            }.onSuccess {
                val chatRoom = getChatRoomUseCase(it)
                _state.update { state ->
                    state.copy(chatRoom = chatRoom)
                }
            }.onFailure {
                _state.update { state ->
                    state.copy(snackBarMessage = R.string.create_chat_error)
                }
            }
        }
    }

    /**
     * Init share key
     *
     * @param node
     */
    suspend fun initShareKey(node: MegaNode) = runCatching {
        val typedNode = getNodeByIdUseCase(NodeId(node.handle))
        require(typedNode is FolderNode) { "Cannot create a share key for a non-folder node" }
        createShareKeyUseCase(typedNode)
    }.onFailure {
        Timber.e(it)
    }

    /**
     * Check copy name collision
     *
     * Verifies duplicate name is available in target folder
     * @param handles results from copy result launcher
     */
    fun checkCopyNameCollision(handles: Pair<LongArray, Long>?) = viewModelScope.launch {
        if (!isOnline()) {
            _state.update { it.copy(snackBarMessage = R.string.error_server_connection_problem) }
            return@launch
        }
        val copyHandles = handles?.first
        val toHandle = handles?.second
        if (copyHandles == null || toHandle == null) return@launch
        _state.update { it.copy(isCopyInProgress = true) }
        runCatching {
            checkNodesNameCollisionUseCase(
                nodes = copyHandles.associateWith { toHandle },
                type = NodeNameCollisionType.COPY
            )
        }.onSuccess { result ->
            val collisions = result.conflictNodes.values.map {
                NameCollision.Copy.getCopyCollision(it)
            }
            _state.update {
                it.copy(isCopyInProgress = false, nameCollisions = collisions)
            }
            if (result.noConflictNodes.isNotEmpty()) {
                copyNodes(result.noConflictNodes)
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    private suspend fun copyNodes(nodes: Map<Long, Long>) {
        val result = runCatching {
            copyNodesUseCase(nodes)
        }.onFailure { error ->
            Timber.e(error)
        }
        _state.update {
            it.copy(moveRequestResult = result)
        }
    }

    /**
     * updateNickNameDialogVisibility
     *
     * @param shouldShow checks if dialog should be visible or not
     */
    fun updateNickNameDialogVisibility(shouldShow: Boolean) {
        _state.update { it.copy(showUpdateAliasDialog = shouldShow) }
    }

    /**
     * Mark handle node name collision result
     *
     */
    fun markHandleNodeNameCollisionResult() {
        _state.update { it.copy(nameCollisions = emptyList()) }
    }

    /**
     * Mark handle move request result
     *
     */
    fun markHandleMoveRequestResult() {
        _state.update { it.copy(moveRequestResult = null) }
    }

    companion object {
        private const val INVALID_CHAT_HANDLE = -1L
        private const val INVALID_NODE_HANDLE = -1L
    }
}
