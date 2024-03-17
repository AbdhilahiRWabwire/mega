package mega.privacy.android.domain.usecase.chat.message


import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import mega.privacy.android.domain.entity.node.FileNode
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

internal class CreateVoiceClipMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override suspend fun invoke(request: CreateTypedMessageInfo): TypedMessage = with(request) {
        val fileNode = nodeList.firstOrNull() as? FileNode
        return VoiceClipMessage(
            chatId = chatId,
            msgId = messageId,
            time = timestamp,
            isDeletable = isDeletable,
            isEditable = isEditable,
            isMine = isMine,
            userHandle = userHandle,
            status = status,
            name = fileNode?.name.orEmpty(),
            size = fileNode?.size ?: 0L,
            duration = (fileNode?.type as? AudioFileTypeInfo)?.duration ?: 0.seconds,
            shouldShowAvatar = shouldShowAvatar,
            reactions = reactions,
            content = content,
        )
    }
}
