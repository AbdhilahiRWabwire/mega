package mega.privacy.android.domain.entity.chat.messages.normal

import kotlinx.serialization.Polymorphic
import mega.privacy.android.domain.entity.chat.messages.UserMessage

/**
 * Call message
 *
 * @property content Message content
 */
@Polymorphic
interface NormalMessage : UserMessage