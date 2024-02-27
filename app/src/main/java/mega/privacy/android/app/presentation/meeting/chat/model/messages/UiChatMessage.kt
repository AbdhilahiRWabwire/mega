package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * UI chat message
 *
 */
interface UiChatMessage {

    /**
     * Id
     */
    val id: Long

    /**
     * Message list item
     *
     * @param state
     */
    @Composable
    fun MessageListItem(
        state: UIMessageState,
        onLongClick: (TypedMessage) -> Unit,
        onMoreReactionsClicked: (Long) -> Unit,
        onReactionClicked: (Long, String, List<UIReaction>) -> Unit,
        onReactionLongClick: (String, List<UIReaction>) -> Unit,
        onForwardClicked: (TypedMessage) -> Unit,
        onSelectedChanged: (Boolean) -> Unit,
    )

    /**
     * Modifier
     */
    val modifier: Modifier
        get() = Modifier.fillMaxWidth()

    /**
     * Display as mine
     */
    val displayAsMine: Boolean

    /**
     * Can forward
     */
    val shouldDisplayForwardIcon: Boolean

    /**
     * Time sent
     */
    val timeSent: Long?

    /**
     * User handle
     */
    val userHandle: Long

    /**
     * Reactions
     */
    val reactions: List<UIReaction>

    /**
     * Is selectable
     */
    val isSelectable: Boolean

    /**
     * Message
     */
    val message: TypedMessage?

    /**
     * Key
     */
    fun key(): String = "$id"
}