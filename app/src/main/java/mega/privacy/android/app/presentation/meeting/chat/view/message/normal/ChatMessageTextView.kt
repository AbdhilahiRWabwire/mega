package mega.privacy.android.app.presentation.meeting.chat.view.message.normal

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.meeting.chat.model.MessageListViewModel
import mega.privacy.android.core.ui.controls.chat.messages.ChatBubble
import mega.privacy.android.core.ui.controls.chat.messages.MessageText
import mega.privacy.android.core.ui.controls.chat.messages.completeURLProtocol
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Text view for chat message
 *
 * @param message Text message
 * @param modifier Modifier
 */
@Composable
fun ChatMessageTextView(
    message: TextMessage,
    interactionEnabled: Boolean,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatMessageTextViewModel = hiltViewModel(),
) {
    val richLinkConfig by viewModel.richLinkConfig.collectAsStateWithLifecycle()
    val parentViewModel = hiltViewModel<MessageListViewModel>()
    val lastedMessageId by parentViewModel.latestMessageId
    val askedEnableRichLink by parentViewModel.askedEnableRichLink

    with(message) {
        var links by rememberSaveable { mutableStateOf(emptyList<String>()) }
        LaunchedEffect(Unit) {
            links = viewModel.getLinks(content)
        }

        ChatMessageTextView(
            text = content,
            isMe = isMine,
            counterNotNowRichLinkWarning = richLinkConfig.counterNotNowRichLinkWarning,
            shouldShowWarning = lastedMessageId == msgId
                    && isMine
                    && richLinkConfig.isShowRichLinkWarning
                    && hasOtherLink
                    && !askedEnableRichLink,
            modifier = modifier,
            onAskedEnableRichLink = parentViewModel::onAskedEnableRichLink,
            enableRichLinkPreview = viewModel::enableRichLinkPreview,
            setRichLinkWarningCounter = viewModel::setRichLinkWarningCounter,
            interactionEnabled = interactionEnabled,
            onLongClick = onLongClick,
            isEdited = isEdited,
            links = links,
        )
    }
}

/**
 * Text view for chat message
 */
@Composable
fun ChatMessageTextView(
    text: String,
    isMe: Boolean,
    shouldShowWarning: Boolean,
    isEdited: Boolean,
    counterNotNowRichLinkWarning: Int,
    modifier: Modifier = Modifier,
    onAskedEnableRichLink: () -> Unit = {},
    enableRichLinkPreview: (Boolean) -> Unit = {},
    setRichLinkWarningCounter: (Int) -> Unit = {},
    interactionEnabled: Boolean = true,
    onLongClick: () -> Unit = {},
    links: List<String> = emptyList(),
) {
    ChatBubble(modifier = modifier, isMe = isMe, subContent = {
        if (shouldShowWarning) {
            EnableRichLinkView(
                alwaysAllowClick = {
                    enableRichLinkPreview(true)
                    onAskedEnableRichLink()
                },
                notNowClick = {
                    setRichLinkWarningCounter(
                        counterNotNowRichLinkWarning
                            .inc()
                            .coerceAtLeast(1)
                    )
                    onAskedEnableRichLink()
                },
                neverClick = {
                    enableRichLinkPreview(false)
                    onAskedEnableRichLink()
                },
                denyNeverClick = {
                    setRichLinkWarningCounter(
                        counterNotNowRichLinkWarning
                            .inc()
                            .coerceAtLeast(1)
                    )
                    onAskedEnableRichLink()
                },
                isShowNeverButton = counterNotNowRichLinkWarning >= 3,
            )
        }
    }) {
        val uriHandler = LocalUriHandler.current

        MessageText(
            message = text,
            isEdited = isEdited,
            links = links,
            interactionEnabled = interactionEnabled,
            onLinkClicked = { link -> uriHandler.openUri(link.completeURLProtocol()) },
            onLongClick = onLongClick,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MeChatMessageTextPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChatMessageTextView(
            text = "Hello World",
            isMe = true,
            shouldShowWarning = true,
            counterNotNowRichLinkWarning = 3,
            isEdited = false,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun OtherChatMessageTextPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChatMessageTextView(
            text = "Hello World",
            isMe = true,
            shouldShowWarning = true,
            counterNotNowRichLinkWarning = 3,
            isEdited = false,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun EditedChatMessageTextPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChatMessageTextView(
            text = "Hello World",
            isMe = true,
            shouldShowWarning = true,
            counterNotNowRichLinkWarning = 3,
            isEdited = true,
        )
    }
}