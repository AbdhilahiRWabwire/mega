package mega.privacy.android.app.presentation.meeting.chat.view.actions

import android.content.Context
import androidx.compose.runtime.Composable
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Message action
 */
interface MessageAction {
    /**
     * Applies to
     *
     * @param messages
     * @return
     */
    fun appliesTo(messages: List<TypedMessage>): Boolean

    /**
     * In column
     *
     * @param messages
     * @param context
     * @param hideBottomSheet
     * @return
     */
    fun bottomSheetMenuItem(
        messages: List<TypedMessage>,
        context: Context,
        hideBottomSheet: () -> Unit,
    ): @Composable () -> Unit
}
