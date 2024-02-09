package mega.privacy.android.data.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import mega.privacy.android.data.database.entity.chat.MetaTypedMessageEntity
import mega.privacy.android.data.database.entity.chat.TypedMessageEntity

/**
 * Typed message request dao
 */
@Dao
interface TypedMessageDao {

    /**
     * Get all as paging source
     *
     * @param chatId
     * @return paging source
     */
    @Transaction
    @Query("SELECT * FROM typed_messages WHERE chatId = :chatId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllAsPagingSource(chatId: Long): PagingSource<Int, MetaTypedMessageEntity>

    /**
     * Insert all
     *
     * @param messages
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<TypedMessageEntity>)

    /**
     * Delete messages by temp id
     *
     * @param tempIds
     */
    @Query("DELETE FROM typed_messages WHERE tempId IN (:tempIds) AND msgId = tempId")
    suspend fun deleteStaleMessagesByTempIds(tempIds: List<Long>)

    /**
     * Delete messages by chat id
     *
     * @param chatId
     */
    @Query("DELETE FROM typed_messages WHERE chatId = :chatId")
    suspend fun deleteMessagesByChatId(chatId: Long)

    /**
     * Delete messages by chat id
     *
     * @param chatId
     */
    @Query("SELECT msgId FROM typed_messages WHERE chatId = :chatId")
    suspend fun getMsgIdsByChatId(chatId: Long): List<Long>

    /**
     * Get message with next greatest timestamp
     *
     * @param chatId
     * @param timestamp
     * @return message
     */
    @Query("SELECT * FROM typed_messages WHERE chatId = :chatId AND timestamp > :timestamp ORDER BY timestamp ASC LIMIT 1")
    suspend fun getMessageWithNextGreatestTimestamp(
        chatId: Long,
        timestamp: Long,
    ): TypedMessageEntity?

}