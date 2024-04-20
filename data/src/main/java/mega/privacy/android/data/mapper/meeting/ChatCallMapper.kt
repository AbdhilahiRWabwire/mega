package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.data.mapper.handles.HandleListMapper
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatSession
import nz.mega.sdk.MegaChatCall
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Chat call mapper
 */
internal class ChatCallMapper @Inject constructor(
    private val handleListMapper: HandleListMapper,
    private val chatCallChangesMapper: ChatCallChangesMapper,
    private val chatCallStatusMapper: ChatCallStatusMapper,
    private val endCallReasonMapper: EndCallReasonMapper,
    private val chatCallTermCodeMapper: ChatCallTermCodeMapper,
    private val callCompositionChangesMapper: CallCompositionChangesMapper,
    private val networkQualityMapper: NetworkQualityMapper,
    private val chatWaitingRoomMapper: ChatWaitingRoomMapper,
    private val waitingRoomStatusMapper: WaitingRoomStatusMapper,
    private val chatSessionMapper: ChatSessionMapper,
    private val callNotificationMapper: CallNotificationMapper,
) {
    operator fun invoke(megaChatCall: MegaChatCall): ChatCall = ChatCall(
        chatId = megaChatCall.chatid,
        callId = megaChatCall.callId,
        status = chatCallStatusMapper(megaChatCall.status),
        hasLocalAudio = megaChatCall.hasLocalAudio(),
        hasLocalVideo = megaChatCall.hasLocalVideo(),
        changes = chatCallChangesMapper(megaChatCall.changes),
        isAudioDetected = megaChatCall.isAudioDetected,
        usersSpeakPermission = megaChatCall.toUserSpeakPermission(),
        duration = megaChatCall.duration.seconds,
        initialTimestamp = megaChatCall.initialTimeStamp,
        finalTimestamp = megaChatCall.finalTimeStamp,
        termCode = chatCallTermCodeMapper(megaChatCall.termCode),
        callDurationLimit = megaChatCall.callDurationLimit,
        callUsersLimit = megaChatCall.callUsersLimit,
        callClientsLimit = megaChatCall.callClientsLimit,
        callClientsPerUserLimit = megaChatCall.callClientsPerUserLimit,
        endCallReason = endCallReasonMapper(megaChatCall.endCallReason),
        isSpeakRequestEnabled = megaChatCall.isSpeakRequestEnabled,
        notificationType = callNotificationMapper(megaChatCall.notificationType),
        auxHandle = megaChatCall.auxHandle,
        isRinging = megaChatCall.isRinging,
        isOwnModerator = megaChatCall.isOwnModerator,
        sessionsClientId = handleListMapper(megaChatCall.sessionsClientid),
        sessionByClientId = megaChatCall.toChatSessionByClientId(),
        peerIdCallCompositionChange = megaChatCall.peeridCallCompositionChange,
        callCompositionChange = callCompositionChangesMapper(megaChatCall.callCompositionChange),
        peerIdParticipants = handleListMapper(megaChatCall.peeridParticipants),
        handle = megaChatCall.handle,
        flag = megaChatCall.flag,
        moderators = handleListMapper(megaChatCall.moderators),
        numParticipants = megaChatCall.numParticipants,
        isIgnored = megaChatCall.isIgnored,
        isIncoming = megaChatCall.isIncoming,
        isOutgoing = megaChatCall.isOutgoing,
        isOwnClientCaller = megaChatCall.isOwnClientCaller,
        caller = megaChatCall.caller,
        isOnHold = megaChatCall.isOnHold,
        genericMessage = megaChatCall.genericMessage,
        networkQuality = networkQualityMapper(megaChatCall.networkQuality),
        usersPendingSpeakRequest = megaChatCall.toUserPendingSpeakRequest(),
        waitingRoomStatus = waitingRoomStatusMapper(megaChatCall.wrJoiningState),
        waitingRoom = chatWaitingRoomMapper(megaChatCall.waitingRoom),
        handleList = if (megaChatCall.handleList == null) null else handleListMapper(megaChatCall.handleList),
        speakersList = handleListMapper(megaChatCall.speakersList),
        speakRequestsList = handleListMapper(megaChatCall.speakRequestsList),
        callWillEndTs = megaChatCall.callWillEndTs
    )

    private fun MegaChatCall.toChatSessionByClientId(): Map<Long, ChatSession> {
        val sessionsMap: MutableMap<Long, ChatSession> = mutableMapOf()
        val listOfClientId = handleListMapper(sessionsClientid)
        for (i in listOfClientId.indices) {
            val clientId = listOfClientId[i]
            getMegaChatSession(clientId)?.let { megaChatSession ->
                sessionsMap[clientId] = chatSessionMapper(megaChatSession)
            }
        }
        return sessionsMap
    }

    private fun MegaChatCall.toUserSpeakPermission(): Map<Long, Boolean> {
        val userSpeakPermissionMap: MutableMap<Long, Boolean> = mutableMapOf()
        val listOfClientId = handleListMapper(sessionsClientid)
        for (i in listOfClientId.indices) {
            val clientId = listOfClientId[i]
            userSpeakPermissionMap[clientId] = hasUserSpeakPermission(clientId)
        }
        return userSpeakPermissionMap
    }

    private fun MegaChatCall.toUserPendingSpeakRequest(): Map<Long, Boolean> {
        val userPendingSpeakRequestMap: MutableMap<Long, Boolean> = mutableMapOf()
        val listOfClientId = handleListMapper(sessionsClientid)
        for (i in listOfClientId.indices) {
            val clientId = listOfClientId[i]
            userPendingSpeakRequestMap[clientId] = hasUserPendingSpeakRequest(clientId)
        }
        return userPendingSpeakRequestMap
    }
}