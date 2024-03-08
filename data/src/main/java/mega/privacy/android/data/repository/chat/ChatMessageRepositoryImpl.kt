package mega.privacy.android.data.repository.chat

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.cache.Cache
import mega.privacy.android.data.database.converter.TypedMessageEntityConverters
import mega.privacy.android.data.extensions.getChatRequestListener
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.chat.ChatStorageGateway
import mega.privacy.android.data.mapper.StringListMapper
import mega.privacy.android.data.mapper.chat.ChatMessageMapper
import mega.privacy.android.data.mapper.chat.messages.PendingMessageEntityMapper
import mega.privacy.android.data.mapper.chat.messages.PendingMessageMapper
import mega.privacy.android.data.mapper.chat.paging.TypedMessagePagingSourceMapper
import mega.privacy.android.data.mapper.handles.HandleListMapper
import mega.privacy.android.data.mapper.handles.MegaHandleListMapper
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.messages.pending.SavePendingMessageRequest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageRequest
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

internal class ChatMessageRepositoryImpl @Inject constructor(
    private val megaChatApiGateway: MegaChatApiGateway,
    private val megaApiGateway: MegaApiGateway,
    private val chatStorageGateway: ChatStorageGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringListMapper: StringListMapper,
    private val handleListMapper: HandleListMapper,
    private val chatMessageMapper: ChatMessageMapper,
    private val megaHandleListMapper: MegaHandleListMapper,
    private val pendingMessageEntityMapper: PendingMessageEntityMapper,
    private val pendingMessageMapper: PendingMessageMapper,
    private val typedMessageEntityConverters: TypedMessageEntityConverters,
    private val originalPathCache: Cache<Map<NodeId, String>>,
    private val typedMessagePagingSourceMapper: TypedMessagePagingSourceMapper,
) : ChatMessageRepository {
    override suspend fun setMessageSeen(chatId: Long, messageId: Long) = withContext(ioDispatcher) {
        megaChatApiGateway.setMessageSeen(chatId, messageId)
    }

    override suspend fun getLastMessageSeenId(chatId: Long): Long = withContext(ioDispatcher) {
        megaChatApiGateway.getLastMessageSeenId(chatId)
    }

    override suspend fun addReaction(chatId: Long, msgId: Long, reaction: String) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getChatRequestListener("addReaction") {}
                megaChatApiGateway.addReaction(chatId, msgId, reaction, listener)
                continuation.invokeOnCancellation {
                    megaChatApiGateway.removeRequestListener(listener)
                }
            }
        }

    override suspend fun deleteReaction(chatId: Long, msgId: Long, reaction: String) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getChatRequestListener("deleteReaction") {}
                megaChatApiGateway.delReaction(chatId, msgId, reaction, listener)
                continuation.invokeOnCancellation {
                    megaChatApiGateway.removeRequestListener(listener)
                }
            }
        }

    override suspend fun getMessageReactions(chatId: Long, msgId: Long) =
        withContext(ioDispatcher) {
            stringListMapper(megaChatApiGateway.getMessageReactions(chatId, msgId))
        }

    override suspend fun getMessageReactionCount(chatId: Long, msgId: Long, reaction: String) =
        withContext(ioDispatcher) {
            megaChatApiGateway.getMessageReactionCount(chatId, msgId, reaction)
        }

    override suspend fun getReactionUsers(chatId: Long, msgId: Long, reaction: String) =
        withContext(ioDispatcher) {
            handleListMapper(megaChatApiGateway.getReactionUsers(chatId, msgId, reaction))
        }

    override suspend fun sendGiphy(
        chatId: Long,
        srcMp4: String?,
        srcWebp: String?,
        sizeMp4: Long,
        sizeWebp: Long,
        width: Int,
        height: Int,
        title: String?,
    ) = withContext(ioDispatcher) {
        chatMessageMapper(
            megaChatApiGateway.sendGiphy(
                chatId,
                srcMp4,
                srcWebp,
                sizeMp4,
                sizeWebp,
                width,
                height,
                title
            )
        )
    }

    override suspend fun attachContact(chatId: Long, contactEmail: String): ChatMessage? =
        withContext(ioDispatcher) {
            megaApiGateway.getContact(contactEmail)?.let { user ->
                megaHandleListMapper(listOf(user.handle))?.let {
                    chatMessageMapper(megaChatApiGateway.attachContacts(chatId, it))
                }
            }
        }

    override suspend fun savePendingMessage(savePendingMessageRequest: SavePendingMessageRequest): PendingMessage {
        return withContext(ioDispatcher) {
            val pendingMessage = pendingMessageEntityMapper(savePendingMessageRequest)
            val id = chatStorageGateway.storePendingMessage(pendingMessage)

            PendingMessage(
                id = id,
                chatId = savePendingMessageRequest.chatId,
                type = savePendingMessageRequest.type,
                uploadTimestamp = savePendingMessageRequest.uploadTimestamp,
                state = savePendingMessageRequest.state.value,
                tempIdKarere = savePendingMessageRequest.tempIdKarere,
                videoDownSampled = savePendingMessageRequest.videoDownSampled,
                filePath = savePendingMessageRequest.filePath,
                nodeHandle = savePendingMessageRequest.nodeHandle,
                fingerprint = savePendingMessageRequest.fingerprint,
                name = savePendingMessageRequest.name,
                transferTag = savePendingMessageRequest.transferTag,
            )
        }
    }

    override suspend fun updatePendingMessage(
        updatePendingMessageRequest: UpdatePendingMessageRequest,
    ) {
        return withContext(ioDispatcher) {
            chatStorageGateway.updatePendingMessage(updatePendingMessageRequest)
        }
    }

    override fun monitorPendingMessages(chatId: Long) =
        chatStorageGateway.fetchPendingMessages(chatId)
            .map { list -> list.map { pendingMessageMapper(it) } }
            .flowOn(ioDispatcher)

    override suspend fun forwardContact(
        sourceChatId: Long,
        msgId: Long,
        targetChatId: Long,
    ): ChatMessage? =
        withContext(ioDispatcher) {
            megaChatApiGateway.forwardContact(sourceChatId, msgId, targetChatId)?.let {
                chatMessageMapper(it)
            }
        }

    override suspend fun attachNode(chatId: Long, nodeHandle: Long): Long? =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getChatRequestListener("attachNode") {
                    it.megaChatMessage.tempId
                }
                megaChatApiGateway.attachNode(chatId, nodeHandle, listener)
                continuation.invokeOnCancellation {
                    megaChatApiGateway.removeRequestListener(listener)
                }
            }
        }

    override suspend fun attachVoiceMessage(chatId: Long, nodeHandle: Long): Long? =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getChatRequestListener("attachVoiceMessage") {
                    it.megaChatMessage.tempId
                }

                megaChatApiGateway.attachVoiceMessage(
                    chatId = chatId,
                    nodeHandle = nodeHandle,
                    listener = listener,
                )

                continuation.invokeOnCancellation {
                    megaChatApiGateway.removeRequestListener(listener)
                }
            }
        }

    override suspend fun getPendingMessage(pendingMessageId: Long): PendingMessage? =
        withContext(ioDispatcher) {
            chatStorageGateway.getPendingMessage(pendingMessageId)?.let {
                pendingMessageMapper(it)
            }
        }

    override suspend fun deletePendingMessage(pendingMessage: PendingMessage) {
        withContext(ioDispatcher) {
            chatStorageGateway.deletePendingMessage(pendingMessage.id)
        }
    }

    override suspend fun deletePendingMessageById(pendingMessageId: Long) {
        withContext(ioDispatcher) {
            chatStorageGateway.deletePendingMessage(pendingMessageId)
        }
    }

    override suspend fun getMessageIdsByType(chatId: Long, type: ChatMessageType): List<Long> =
        withContext(ioDispatcher) {
            chatStorageGateway.getMessageIdsByType(chatId, type)
        }

    override suspend fun getReactionsFromMessage(chatId: Long, msgId: Long): List<Reaction> =
        withContext(ioDispatcher) {
            chatStorageGateway.getMessageReactions(chatId, msgId)?.let { reactions ->
                typedMessageEntityConverters.convertToMessageReactionList(reactions)
            } ?: emptyList()
        }

    override suspend fun updateReactionsInMessage(
        chatId: Long,
        msgId: Long,
        reactions: List<Reaction>,
    ) {
        withContext(ioDispatcher) {
            val reactionsString =
                typedMessageEntityConverters.convertFromMessageReactionList(reactions)
            chatStorageGateway.updateMessageReactions(chatId, msgId, reactionsString)
        }
    }

    override suspend fun deleteMessage(chatId: Long, msgId: Long) = withContext(ioDispatcher) {
        megaChatApiGateway.deleteMessage(chatId, msgId)?.let { chatMessageMapper(it) }
    }

    override suspend fun revokeAttachmentMessage(chatId: Long, msgId: Long) =
        withContext(ioDispatcher) {
            megaChatApiGateway.revokeAttachmentMessage(chatId, msgId)?.let { chatMessageMapper(it) }
        }

    override suspend fun editMessage(chatId: Long, msgId: Long, msg: String) =
        withContext(ioDispatcher) {
            megaChatApiGateway.editMessage(chatId, msgId, msg)?.let { chatMessageMapper(it) }
        }

    override suspend fun editGeolocation(
        chatId: Long,
        msgId: Long,
        longitude: Float,
        latitude: Float,
        img: String,
    ) = withContext(ioDispatcher) {
        megaChatApiGateway.editGeolocation(chatId, msgId, longitude, latitude, img)
            ?.let { chatMessageMapper(it) }
    }

    override fun getCachedOriginalPathForNode(nodeId: NodeId) = originalPathCache.get()?.get(nodeId)

    override fun cacheOriginalPathForNode(nodeId: NodeId, path: String) {
        val updated = buildMap {
            originalPathCache.get()?.let {
                putAll(it)
            }
            put(nodeId, path)
        }
        originalPathCache.set(updated)
    }

    override fun getPagedMessages(chatId: Long) =
        typedMessagePagingSourceMapper(chatStorageGateway.getTypedMessageRequestPagingSource(chatId))


    override suspend fun truncateMessages(chatId: Long, truncateTimestamp: Long) {
        withContext(ioDispatcher) {
            chatStorageGateway.truncateMessages(
                chatId = chatId,
                truncateTimestamp = truncateTimestamp
            )
        }
    }

    override suspend fun clearChatPendingMessages(chatId: Long) = withContext(ioDispatcher) {
        chatStorageGateway.clearChatPendingMessages(chatId)
    }
}