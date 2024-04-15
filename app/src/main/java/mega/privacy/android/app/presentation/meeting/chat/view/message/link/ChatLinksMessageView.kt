package mega.privacy.android.app.presentation.meeting.chat.view.message.link

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.message.normal.ChatMessageTextViewModel
import mega.privacy.android.core.ui.controls.chat.messages.ChatBubble
import mega.privacy.android.core.ui.controls.chat.messages.MessageText
import mega.privacy.android.core.ui.controls.chat.messages.completeURLProtocol
import mega.privacy.android.domain.entity.chat.messages.normal.TextLinkMessage

/**
 * Chat links message view
 *
 * @param message
 * @param modifier
 * @param linkViews
 */
@Composable
fun ChatLinksMessageView(
    message: TextLinkMessage,
    contentLinks: List<LinkContent>,
    linkViews: @Composable () -> Unit,
    interactionEnabled: Boolean,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    viewModel: ChatMessageTextViewModel = hiltViewModel(),
) {
    with(message) {
        var links by rememberSaveable { mutableStateOf(emptyList<String>()) }
        LaunchedEffect(Unit) {
            links = viewModel.getLinks(content)
        }

        ChatBubble(
            isMe = isMine,
            modifier = modifier,
            subContent = linkViews,
            content = {
                val uriHandler = LocalUriHandler.current
                val context = LocalContext.current

                MessageText(
                    message = content,
                    isEdited = isEdited,
                    links = links,
                    interactionEnabled = interactionEnabled,
                    onLinkClicked = { link ->
                        contentLinks.firstOrNull { it.link.contains(link) }?.onClick(context)
                            ?: uriHandler.openUri(link.completeURLProtocol())
                    },
                    onLongClick = onLongClick,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            },
        )
    }
}