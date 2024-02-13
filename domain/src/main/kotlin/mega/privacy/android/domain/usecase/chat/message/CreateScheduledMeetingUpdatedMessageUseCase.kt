package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.messages.management.ScheduledMeetingUpdatedMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import javax.inject.Inject


internal class CreateScheduledMeetingUpdatedMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override fun invoke(request: CreateTypedMessageInfo) = with(request) {
        ScheduledMeetingUpdatedMessage(
            chatId = chatId,
            msgId = msgId,
            time = timestamp,
            isMine = isMine,
            userHandle = userHandle,
            shouldShowAvatar = shouldShowAvatar,
            shouldShowTime = shouldShowTime,
            reactions = reactions,
        )
    }
}