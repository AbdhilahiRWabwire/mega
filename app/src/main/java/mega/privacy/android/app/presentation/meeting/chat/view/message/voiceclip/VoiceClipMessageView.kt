package mega.privacy.android.app.presentation.meeting.chat.view.message.voiceclip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.core.ui.controls.chat.messages.CoreVoiceClipMessageView
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage


@Composable
fun VoiceClipMessageView(
    message: VoiceClipMessage,
    chatId: Long,
    modifier: Modifier = Modifier,
    viewModel: VoiceClipMessageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.getUiStateFlow(message.msgId).collectAsStateWithLifecycle()
    LaunchedEffect(message.msgId) {
        viewModel.addVoiceClip(
            message = message,
            chatId = chatId
        )
    }

    CoreVoiceClipMessageView(
        isMe = message.isMine,
        timestamp = uiState.timestamp,
        modifier = modifier,
        isError = uiState.isError,
        loadProgress = uiState.loadProgress?.floatValue,
        playProgress = uiState.playProgress?.floatValue,
        isPlaying = uiState.isPlaying,
        onPlayClicked = { viewModel.onPlayOrPauseClicked(message.msgId) },
        interactionEnabled = true,
    )
}