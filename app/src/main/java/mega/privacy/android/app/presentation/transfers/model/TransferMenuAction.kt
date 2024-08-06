package mega.privacy.android.app.presentation.transfers.model

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionString

/**
 * Transfer menu action.
 * All the actions which may be available in normal mode should be defined here.
 */
sealed interface TransferMenuAction : MenuAction {

    /**
     * Resume transfers
     */
    class Resume : MenuActionString(
        iconRes = iconPackR.drawable.ic_play_medium_regular_outline,
        descriptionRes = R.string.action_play,
        testTag = TEST_TAG_RESUME_ACTION,
    ), TransferMenuAction {
        override val orderInCategory = 100
    }

    /**
     * Pause transfers
     */
    class Pause : MenuActionString(
        iconRes = iconPackR.drawable.ic_pause_medium_regular_outline,
        descriptionRes = R.string.action_pause,
        testTag = TEST_TAG_PAUSE_ACTION,
    ), TransferMenuAction {
        override val orderInCategory = 100
    }

    companion object {
        /**
         * Test Tag resume transfers Action
         */
        const val TEST_TAG_RESUME_ACTION = "transfers_view:action_resume_transfers"

        /**
         * Test Tag pause transfers Action
         */
        const val TEST_TAG_PAUSE_ACTION = "transfers_view:action_pause_transfers"
    }
}