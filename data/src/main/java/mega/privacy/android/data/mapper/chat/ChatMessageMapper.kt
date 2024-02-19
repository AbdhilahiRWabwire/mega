package mega.privacy.android.data.mapper.chat

import mega.privacy.android.data.mapper.handles.HandleListMapper
import mega.privacy.android.data.mapper.node.NodeListMapper
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageChange
import mega.privacy.android.domain.entity.chat.ChatMessageCode
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.ChatMessageTermCode
import mega.privacy.android.domain.entity.chat.ChatMessageType
import nz.mega.sdk.MegaChatMessage
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

internal class ChatMessageMapper @Inject constructor(
    private val chatPermissionsMapper: ChatPermissionsMapper,
    private val nodeListMapper: NodeListMapper,
    private val handleListMapper: HandleListMapper,
    private val containsMetaMapper: ContainsMetaMapper,
) {

    suspend operator fun invoke(msg: MegaChatMessage) = ChatMessage(
        status = msg.status.toChatMessageStatus(),
        messageId = msg.msgId,
        tempId = msg.tempId,
        msgIndex = msg.msgIndex,
        userHandle = msg.userHandle,
        type = msg.type.toChatMessageType(),
        hasConfirmedReactions = msg.hasConfirmedReactions(),
        timestamp = msg.timestamp,
        content = msg.content.orEmpty(),
        isEdited = msg.isEdited,
        isDeleted = msg.isDeleted,
        isEditable = msg.isEditable,
        isDeletable = msg.isDeletable,
        isManagementMessage = msg.isManagementMessage,
        handleOfAction = msg.handleOfAction,
        privilege = chatPermissionsMapper(msg.privilege),
        code = toChatMessageCode(msg.status, msg.code),
        usersCount = msg.usersCount,
        userHandles = msg.toUserHandles(),
        userNames = msg.toUserNames(),
        userEmails = msg.toUserEmails(),
        nodeList = msg.megaNodeList?.let { nodeListMapper(it) }.orEmpty(),
        handleList = msg.megaHandleList?.let { handleListMapper(it) }.orEmpty(),
        duration = msg.duration.seconds,
        retentionTime = msg.retentionTime,
        termCode = msg.termCode.toChatMessageTermCode(),
        rowId = msg.rowId,
        changes = msg.changes.toChatMessageChanges(),
        containsMeta = msg.containsMeta?.let { containsMetaMapper(it) }
    )

    private fun Int.toChatMessageStatus(): ChatMessageStatus = when (this) {
        MegaChatMessage.STATUS_UNKNOWN -> ChatMessageStatus.UNKNOWN
        MegaChatMessage.STATUS_SENDING -> ChatMessageStatus.SENDING
        MegaChatMessage.STATUS_SENDING_MANUAL -> ChatMessageStatus.SENDING_MANUAL
        MegaChatMessage.STATUS_SERVER_RECEIVED -> ChatMessageStatus.SERVER_RECEIVED
        MegaChatMessage.STATUS_SERVER_REJECTED -> ChatMessageStatus.SERVER_REJECTED
        MegaChatMessage.STATUS_DELIVERED -> ChatMessageStatus.DELIVERED
        MegaChatMessage.STATUS_NOT_SEEN -> ChatMessageStatus.NOT_SENT
        MegaChatMessage.STATUS_SEEN -> ChatMessageStatus.SEEN
        else -> ChatMessageStatus.UNKNOWN
    }


    private fun Int.toChatMessageType(): ChatMessageType = when (this) {
        MegaChatMessage.TYPE_UNKNOWN -> ChatMessageType.UNKNOWN
        MegaChatMessage.TYPE_INVALID -> ChatMessageType.INVALID
        MegaChatMessage.TYPE_NORMAL -> ChatMessageType.NORMAL
        MegaChatMessage.TYPE_ALTER_PARTICIPANTS -> ChatMessageType.ALTER_PARTICIPANTS
        MegaChatMessage.TYPE_TRUNCATE -> ChatMessageType.TRUNCATE
        MegaChatMessage.TYPE_PRIV_CHANGE -> ChatMessageType.PRIV_CHANGE
        MegaChatMessage.TYPE_CHAT_TITLE -> ChatMessageType.CHAT_TITLE
        MegaChatMessage.TYPE_CALL_ENDED -> ChatMessageType.CALL_ENDED
        MegaChatMessage.TYPE_CALL_STARTED -> ChatMessageType.CALL_STARTED
        MegaChatMessage.TYPE_PUBLIC_HANDLE_CREATE -> ChatMessageType.PUBLIC_HANDLE_CREATE
        MegaChatMessage.TYPE_PUBLIC_HANDLE_DELETE -> ChatMessageType.PUBLIC_HANDLE_DELETE
        MegaChatMessage.TYPE_SET_PRIVATE_MODE -> ChatMessageType.SET_PRIVATE_MODE
        MegaChatMessage.TYPE_SET_RETENTION_TIME -> ChatMessageType.SET_RETENTION_TIME
        MegaChatMessage.TYPE_SCHED_MEETING -> ChatMessageType.SCHED_MEETING
        MegaChatMessage.TYPE_NODE_ATTACHMENT -> ChatMessageType.NODE_ATTACHMENT
        MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT -> ChatMessageType.REVOKE_NODE_ATTACHMENT
        MegaChatMessage.TYPE_CONTACT_ATTACHMENT -> ChatMessageType.CONTACT_ATTACHMENT
        MegaChatMessage.TYPE_CONTAINS_META -> ChatMessageType.CONTAINS_META
        MegaChatMessage.TYPE_VOICE_CLIP -> ChatMessageType.VOICE_CLIP
        else -> ChatMessageType.UNKNOWN
    }

    private fun toChatMessageCode(status: Int, code: Int): ChatMessageCode = when (status) {
        MegaChatMessage.STATUS_SENDING_MANUAL -> when (code) {
            MegaChatMessage.REASON_PEERS_CHANGED -> ChatMessageCode.REASON_PEERS_CHANGED
            MegaChatMessage.REASON_TOO_OLD -> ChatMessageCode.REASON_TOO_OLD
            MegaChatMessage.REASON_GENERAL_REJECT -> ChatMessageCode.REASON_GENERAL_REJECT
            MegaChatMessage.REASON_NO_WRITE_ACCESS -> ChatMessageCode.REASON_NO_WRITE_ACCESS
            MegaChatMessage.REASON_NO_CHANGES -> ChatMessageCode.REASON_NO_CHANGES
            else -> ChatMessageCode.UNKNOWN
        }

        else -> when (code) {
            MegaChatMessage.DECRYPTING -> ChatMessageCode.DECRYPTING
            MegaChatMessage.INVALID_KEY -> ChatMessageCode.INVALID_KEY
            MegaChatMessage.INVALID_SIGNATURE -> ChatMessageCode.INVALID_SIGNATURE
            MegaChatMessage.INVALID_FORMAT -> ChatMessageCode.INVALID_FORMAT
            MegaChatMessage.INVALID_TYPE -> ChatMessageCode.INVALID_TYPE
            else -> ChatMessageCode.UNKNOWN
        }
    }

    private fun MegaChatMessage.toUserHandles(): List<Long> =
        (0 until usersCount).map { getUserHandle(it) }

    private fun MegaChatMessage.toUserNames(): List<String> =
        (0 until usersCount).map { getUserName(it) }

    private fun MegaChatMessage.toUserEmails(): List<String> =
        (0 until usersCount).map { getUserEmail(it) }

    private fun Int.toChatMessageTermCode(): ChatMessageTermCode = when (this) {
        MegaChatMessage.END_CALL_REASON_ENDED -> ChatMessageTermCode.ENDED
        MegaChatMessage.END_CALL_REASON_REJECTED -> ChatMessageTermCode.REJECTED
        MegaChatMessage.END_CALL_REASON_NO_ANSWER -> ChatMessageTermCode.NO_ANSWER
        MegaChatMessage.END_CALL_REASON_FAILED -> ChatMessageTermCode.FAILED
        MegaChatMessage.END_CALL_REASON_CANCELLED -> ChatMessageTermCode.CANCELLED
        MegaChatMessage.END_CALL_REASON_BY_MODERATOR -> ChatMessageTermCode.BY_MODERATOR
        else -> ChatMessageTermCode.FAILED
    }

    private fun Int.toChatMessageChanges() =
        msgChanges.filter { (it.key and this) != 0 }.values.toList()

    private val msgChanges = mapOf(
        MegaChatMessage.CHANGE_TYPE_STATUS to ChatMessageChange.STATUS,
        MegaChatMessage.CHANGE_TYPE_CONTENT to ChatMessageChange.CONTENT,
        MegaChatMessage.CHANGE_TYPE_ACCESS to ChatMessageChange.ACCESS,
        MegaChatMessage.CHANGE_TYPE_TIMESTAMP to ChatMessageChange.TIMESTAMP
    )
}
