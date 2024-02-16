package mega.privacy.android.data.database.entity.chat

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.chat.messages.meta.ChatGeolocationInfo

/**
 * Entity to store a geolocation message.
 *
 * @property messageId Message ID.
 * @property longitude Longitude.
 * @property latitude Latitude.
 * @property image Image.
 */
@Entity(tableName = "chat_geolocation")
@Serializable
data class ChatGeolocationEntity(
    @PrimaryKey val messageId: Long,
    override val longitude: Float,
    override val latitude: Float,
    override val image: String?,
) : ChatGeolocationInfo