package mega.privacy.android.data.facade.chat

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.chat.InMemoryChatDatabase
import mega.privacy.android.data.database.entity.chat.ChatGeolocationEntity
import mega.privacy.android.data.database.entity.chat.ChatNodeEntity
import mega.privacy.android.data.database.entity.chat.GiphyEntity
import mega.privacy.android.data.database.entity.chat.PendingMessageEntity
import mega.privacy.android.data.database.entity.chat.RichPreviewEntity
import mega.privacy.android.data.database.entity.chat.TypedMessageEntity
import mega.privacy.android.data.gateway.chat.ChatStorageGateway
import mega.privacy.android.domain.entity.chat.ChatMessageType
import javax.inject.Inject

/**
 * Chat storage facade
 *
 * Facade to encapsulate chat storage implementation
 *
 * @property database In memory chat database
 */
internal class ChatStorageFacade @Inject constructor(
    private val database: InMemoryChatDatabase,
) : ChatStorageGateway {

    /**
     * Get typed message request paging source
     *
     * @param chatId Chat ID
     * @return paging source
     */
    override fun getTypedMessageRequestPagingSource(chatId: Long) =
        database.typedMessageDao().getAllAsPagingSource(chatId)

    /**
     * Store messages
     *
     * @param messages Messages to store
     */
    override suspend fun storeMessages(
        messages: List<TypedMessageEntity>,
        richPreviews: List<RichPreviewEntity>,
        giphys: List<GiphyEntity>,
        geolocations: List<ChatGeolocationEntity>,
        chatNodes: List<ChatNodeEntity>,
    ) {
        with(database) {
            withTransaction {
                typedMessageDao().deleteStaleMessagesByTempIds(messages.map { it.tempId }
                    .filterNot { it == -1L })
                typedMessageDao().insertAll(messages)
                val metaDao = chatMessageMetaDao()
                richPreviews.takeUnless { it.isEmpty() }
                    ?.let { metaDao.insertRichPreviews(it) }
                giphys.takeUnless { it.isEmpty() }?.let { metaDao.insertGiphys(it) }
                geolocations.takeUnless { it.isEmpty() }
                    ?.let { metaDao.insertGeolocations(it) }
                chatNodes.takeUnless { it.isEmpty() }?.let { chatNodeDao().insertChatNodes(it) }
            }
        }
    }

    /**
     * Clear chat messages
     *
     * @param chatId Chat ID
     */
    override suspend fun clearChatMessages(chatId: Long) {
        with(database) {
            withTransaction {
                val messagesToDelete = typedMessageDao().getMsgIdsByChatId(chatId)
                typedMessageDao().deleteMessagesByChatId(chatId)

                val metaDao = chatMessageMetaDao()
                metaDao.deleteRichPreviewsByMessageId(messagesToDelete)
                metaDao.deleteGiphysByMessageId(messagesToDelete)
                metaDao.deleteGeolocationsByMessageId(messagesToDelete)
                chatNodeDao().deleteChatNodesByMessageId(messagesToDelete)
            }
        }
    }

    /**
     * Get next message
     *
     * @param chatId Chat ID
     * @param timestamp Timestamp
     * @return next message
     */
    override suspend fun getNextMessage(chatId: Long, timestamp: Long) =
        database.typedMessageDao().getMessageWithNextGreatestTimestamp(chatId, timestamp)


    override suspend fun storePendingMessage(
        pendingMessageEntity: PendingMessageEntity,
    ) = database.pendingMessageDao().insert(pendingMessageEntity)

    override suspend fun updatePendingMessage(pendingMessage: PendingMessageEntity) {
        database.pendingMessageDao().insert(pendingMessage)
    }

    override suspend fun deletePendingMessage(pendingMessageId: Long) {
        database.pendingMessageDao().delete(pendingMessageId)
    }

    override fun fetchPendingMessages(chatId: Long): Flow<List<PendingMessageEntity>> =
        database.pendingMessageDao().fetchPendingMessagesForChat(chatId)

    override suspend fun getPendingMessage(pendingMessageId: Long): PendingMessageEntity? =
        database.pendingMessageDao().get(pendingMessageId)

    override suspend fun getMessageIdsByType(chatId: Long, type: ChatMessageType): List<Long> =
        database.typedMessageDao().getMessageIdsByType(chatId, type)
}