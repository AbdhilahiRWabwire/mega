package mega.privacy.android.domain.usecase.chat.message.paging

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import mega.privacy.android.domain.usecase.chat.message.reactions.GetReactionsUseCase
import mega.privacy.android.domain.usecase.node.DoesNodeExistUseCase
import javax.inject.Inject

/**
 * Create save message request use case
 */
class CreateSaveMessageRequestUseCase @Inject constructor(
    private val getReactionsUseCase: GetReactionsUseCase,
    private val doesNodeExistUseCase: DoesNodeExistUseCase,
) {

    /**
     * Invoke
     *
     * @param chatMessages List of [ChatMessage]
     * @param currentUserHandle Current user handle
     * @return List of [CreateTypedMessageRequest]
     */
    suspend operator fun invoke(
        chatId: Long,
        chatMessages: List<ChatMessage>,
        currentUserHandle: Long,
        nextMessageUserHandle: Long?,
    ): List<CreateTypedMessageRequest> {
        return chatMessages
            .sortedBy { it.timestamp }
            .map { chatMessage ->
                val isMine = chatMessage.userHandle == currentUserHandle
                val shouldShowAvatar = shouldShowAvatar(
                    current = chatMessage,
                    nextUserHandle = getNextMessageUserHandle(
                        chatMessages,
                        chatMessages.indexOf(chatMessage),
                        nextMessageUserHandle
                    ),
                    currentIsMine = isMine
                )
                val reactions = if (chatMessage.hasConfirmedReactions) {
                    getReactionsUseCase(chatId, chatMessage.messageId, currentUserHandle)
                } else {
                    emptyList()
                }
                val exists = chatMessage.nodeList.firstOrNull()?.let {
                    if (isMine) doesNodeExistUseCase(it.id) else true
                } ?: true

                CreateTypedMessageRequest(
                    chatMessage = chatMessage,
                    chatId = chatId,
                    isMine = isMine,
                    shouldShowAvatar = shouldShowAvatar,
                    reactions = reactions,
                    exists = exists
                )
            }
    }

    private fun getNextMessageUserHandle(
        typedMessages: List<ChatMessage>,
        index: Int,
        nextMessageUserHandle: Long?,
    ) = typedMessages.getOrNull(index + 1)?.userHandle ?: nextMessageUserHandle

    private fun shouldShowAvatar(
        current: ChatMessage,
        nextUserHandle: Long?,
        currentIsMine: Boolean,
    ) = !currentIsMine && !current.hasSameSender(nextUserHandle)

    private fun ChatMessage.hasSameSender(
        other: Long?,
    ) = userHandle == other
}