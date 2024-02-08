package mega.privacy.android.data.database.entity.chat

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Meta typed message request
 *
 * @property typedMessageEntity Typed message request entity
 * @property nodeList List of nodes
 * @property richPreviewEntity Rich preview entity
 * @property geolocationEntity Chat geolocation entity
 * @property giphyEntity Giphy entity
 * @property pendingMessageEntity Pending message entity
 */
data class MetaTypedMessageEntity(
    @Embedded val typedMessageEntity: TypedMessageEntity,
    @Relation(
        parentColumn = "msgId",
        entityColumn = "messageId",
        entity = ChatNodeEntity::class
    )
    val nodeList: List<ChatNodeEntity>,
    @Relation(
        parentColumn = "msgId",
        entityColumn = "messageId",
        entity = RichPreviewEntity::class
    )
    val richPreviewEntity: RichPreviewEntity?,
    @Relation(
        parentColumn = "msgId",
        entityColumn = "messageId",
        entity = ChatGeolocationEntity::class
    )
    val geolocationEntity: ChatGeolocationEntity?,
    @Relation(
        parentColumn = "msgId",
        entityColumn = "messageId",
        entity = GiphyEntity::class
    )
    val giphyEntity: GiphyEntity?,
    @Relation(
        parentColumn = "pendingMessageId",
        entityColumn = "pendingMessageId",
        entity = PendingMessageEntity::class
    )
    val pendingMessageEntity: PendingMessageEntity?
)