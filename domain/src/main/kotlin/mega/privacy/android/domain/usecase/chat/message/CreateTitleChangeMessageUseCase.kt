package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.messages.management.TitleChangeMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import javax.inject.Inject


internal class CreateTitleChangeMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override fun invoke(request: CreateTypedMessageInfo) = with(request) {
        TitleChangeMessage(
            chatId = chatId,
            msgId = msgId,
            time = timestamp,
            isMine = isMine,
            userHandle = userHandle,
            content = content.orEmpty(),
            shouldShowAvatar = shouldShowAvatar,
            shouldShowTime = shouldShowTime,
            reactions = reactions,
        )
    }
}