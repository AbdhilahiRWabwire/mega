package mega.privacy.android.app.presentation.meeting.chat.view

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.extension.isJoined
import mega.privacy.android.app.presentation.meeting.chat.extension.isStarted
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.shared.original.core.ui.controls.chat.ChatMeetingButton
import mega.privacy.android.domain.entity.ChatRoomPermission

@Composable
fun TopCallButton(
    uiState: ChatUiState,
    onStartOrJoinMeeting: () -> Unit = {},
) = with(uiState) {
    if (schedIsPending && isActive && !isArchived) {
        val modifier = Modifier
            .padding(top = 16.dp)

        if (callInThisChat?.status?.isStarted != true && myPermission != ChatRoomPermission.ReadOnly) {
            ChatMeetingButton(
                modifier = modifier,
                text = stringResource(id = R.string.meetings_chat_room_start_scheduled_meeting_option),
                onClick = onStartOrJoinMeeting,
            )
        } else if (callInThisChat != null && callInThisChat.status?.isJoined != true) {
            ChatMeetingButton(
                modifier = modifier,
                text = stringResource(id = R.string.meetings_chat_room_join_scheduled_meeting_option),
                onClick = onStartOrJoinMeeting,
            )
        }
    }
}
