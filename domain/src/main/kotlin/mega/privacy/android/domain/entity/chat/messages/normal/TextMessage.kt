package mega.privacy.android.domain.entity.chat.messages.normal

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction

/**
 * Text message
 *
 * @param hasOtherLink Whether the message contains other links. (Not contact link, file link, folder link)
 */
@Serializable
data class TextMessage(
    override val chatId: Long,
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val reactions: List<Reaction>,
    override val content: String,
    val hasOtherLink: Boolean,
) : NormalMessage