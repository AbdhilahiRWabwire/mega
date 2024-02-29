package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.NodeAttachmentMessageView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Node attachment Ui message
 *
 * @param message [NodeAttachmentMessageView]
 */
data class NodeAttachmentUiMessage(
    override val message: NodeAttachmentMessage,
    override val reactions: List<UIReaction>,
) : AvatarMessage() {

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun RowScope.ContentComposable(onLongClick: (TypedMessage) -> Unit) {
        NodeAttachmentMessageView(
            message = message, chatId = message.chatId,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = { onLongClick(message) }
            ))
    }

    override val showTime = message.shouldShowTime
    override val showAvatar = message.shouldShowAvatar
    override val displayAsMine = message.isMine
    override val shouldDisplayForwardIcon = true
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
}