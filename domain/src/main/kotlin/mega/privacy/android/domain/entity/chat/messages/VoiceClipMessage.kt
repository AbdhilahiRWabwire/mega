package mega.privacy.android.domain.entity.chat.messages

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import kotlin.time.Duration

/**
 * Voice clip message
 *
 * @property status Message status
 * @property name name of the voice clip
 * @property size size of the voice clip
 * @property duration duration of the voice clip in milliseconds
 */
@Serializable
data class VoiceClipMessage(
    override val chatId: Long,
    override val msgId: Long,
    override val time: Long,
    override val isDeletable: Boolean,
    override val isEditable: Boolean,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val reactions: List<Reaction>,
    override val status: ChatMessageStatus,
    val name: String,
    val size: Long,
    val duration: Duration,
) : TypedMessage
