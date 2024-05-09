package mega.privacy.android.app.presentation.node.model.menuaction

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Send to chat menu action
 */
class SendMenuAction @Inject constructor() : MenuActionWithIcon {

    @Composable
    override fun getDescription() = stringResource(id = R.string.context_send)

    @Composable
    override fun getIconPainter() =
        painterResource(id = iconPackR.drawable.ic_send_horizontal_medium_regular_outline)

    override val orderInCategory = 180

    override val testTag: String = "menu_action:send"
}