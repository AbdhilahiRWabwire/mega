package mega.privacy.android.app.presentation.node.model.menuaction

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Share folder menu action
 */
class ManageShareFolderMenuAction @Inject constructor() : MenuActionWithIcon {

    @Composable
    override fun getDescription() = stringResource(id = R.string.manage_share)

    @Composable
    override fun getIconPainter() = painterResource(id = iconPackR.drawable.ic_gear_six_medium_regular_outline)

    override val orderInCategory = 190

    override val testTag: String = "menu_action:share_folder"
}