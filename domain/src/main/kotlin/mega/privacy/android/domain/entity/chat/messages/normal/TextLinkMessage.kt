package mega.privacy.android.domain.entity.chat.messages.normal

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.chat.LinkDetail
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction

/**
 * Contact link message
 * @property links all links in the message
 * @property content Link
 */
@Serializable
data class TextLinkMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val reactions: List<Reaction>,
    val links: List<LinkDetail>,
    val content: String,
) : NormalMessage