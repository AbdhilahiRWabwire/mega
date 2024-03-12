package mega.privacy.android.app.presentation.meeting.chat.view.actions

import mega.privacy.android.icon.pack.R.drawable as IconPack
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.message.error.SendErrorViewModel
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

internal class RetryMessageAction() : MessageAction(
    text = R.string.message_option_retry,
    icon = IconPack.ic_menu_retry,
    testTag = "action_retry",
) {
    override fun shouldDisplayFor(messages: Set<TypedMessage>) =
        messages.all { it.status == ChatMessageStatus.SENDING_MANUAL }

    override val appliesToSendError = true

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        val sendErrorViewModel: SendErrorViewModel = viewModel()
        sendErrorViewModel.retry(messages)
        onHandled()
    }
}
