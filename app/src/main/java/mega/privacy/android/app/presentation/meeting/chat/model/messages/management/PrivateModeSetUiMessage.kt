package mega.privacy.android.app.presentation.meeting.chat.model.messages.management

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.PrivateModeSetMessageView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.management.PrivateModeSetMessage


/**
 * Private mode set ui message
 *
 */
data class PrivateModeSetUiMessage(
    override val message: PrivateModeSetMessage,
    override val reactions: List<UIReaction>,
) : ManagementUiChatMessage() {

    override val contentComposable: @Composable () -> Unit = {
        PrivateModeSetMessageView(
            message = message,
            modifier = Modifier.padding(start = 32.dp)
        )
    }
}
