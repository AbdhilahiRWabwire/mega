package mega.privacy.android.domain.entity.chat.messages.pending

import mega.privacy.android.domain.entity.chat.PendingMessageState

/**
 * Update pending message request interface
 * @property pendingMessageId
 */
sealed interface UpdatePendingMessageRequest {
    val pendingMessageId: Long
}

/**
 * Entity to update the state of a pending message
 * @property pendingMessageId
 * @property state
 */
data class UpdatePendingMessageStateRequest(
    override val pendingMessageId: Long,
    val state: PendingMessageState,
) : UpdatePendingMessageRequest


/**
 * Entity to update the state and node handle of a pending message
 * @property pendingMessageId
 * @property state
 * @property nodeHandle
 */
data class UpdatePendingMessageStateAndNodeHandleRequest(
    override val pendingMessageId: Long,
    val state: PendingMessageState,
    val nodeHandle: Long,
) : UpdatePendingMessageRequest

/**
 * Entity to update the tag and state of a pending message
 * @property pendingMessageId
 * @property transferTag
 * @property state
 */
data class UpdatePendingMessageTransferTagRequest(
    override val pendingMessageId: Long,
    val transferTag: Int,
    val state: PendingMessageState,
) : UpdatePendingMessageRequest
