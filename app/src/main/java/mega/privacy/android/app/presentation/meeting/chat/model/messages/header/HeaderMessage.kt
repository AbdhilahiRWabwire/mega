package mega.privacy.android.app.presentation.meeting.chat.model.messages.header

import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction

/**
 * Header message
 */
abstract class HeaderMessage() : UiChatMessage {
    override val timeSent: Long? = null
    override val id = -1L
    override val displayAsMine = false
    override val shouldDisplayForwardIcon = false
    override val userHandle = -1L
    override val showTime = false
    override val reactions = emptyList<UIReaction>()
    override val isSelectable = false
    override val message = null
    override var isSelected: Boolean
        get() = false
        set(_) {}
}