package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessageCode
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.messages.invalid.FormatInvalidMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.InvalidMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.SignatureInvalidMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.UnrecognizableInvalidMessage
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import javax.inject.Inject

/**
 * Create invalid message use case.
 */
class CreateInvalidMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override suspend fun invoke(request: CreateTypedMessageInfo) =
        with(request) {
            val constructor: (
                Long,
                Long,
                Long,
                Boolean,
                Boolean,
                Boolean,
                Long,
                Boolean,
                Boolean,
                List<Reaction>,
                ChatMessageStatus,
            ) -> InvalidMessage = when {
                type == ChatMessageType.INVALID -> {
                    ::UnrecognizableInvalidMessage
                }

                code == ChatMessageCode.INVALID_FORMAT -> {
                    ::FormatInvalidMessage
                }

                code == ChatMessageCode.INVALID_SIGNATURE -> {
                    ::SignatureInvalidMessage
                }

                else -> {
                    ::UnrecognizableInvalidMessage
                }
            }

            constructor(
                chatId,
                messageId,
                timestamp,
                isDeletable,
                isEditable,
                isMine,
                userHandle,
                shouldShowAvatar,
                shouldShowTime,
                reactions,
                status,
            )
        }
}