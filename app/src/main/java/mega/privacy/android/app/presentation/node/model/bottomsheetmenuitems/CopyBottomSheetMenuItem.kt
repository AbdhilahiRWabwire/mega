package mega.privacy.android.app.presentation.node.model.bottomsheetmenuitems

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.node.model.menuaction.CopyMenuAction
import mega.privacy.android.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Info bottom sheet menu action
 *
 * @param menuAction [CopyMenuAction]
 */
class CopyBottomSheetMenuItem @Inject constructor(
    override val menuAction: CopyMenuAction,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override fun shouldDisplay() = true
    override fun menuAction(selectedNode: TypedNode): @Composable ((MenuAction) -> Unit) -> Unit = {
        MenuActionListTile(
            text = menuAction.getDescription(),
            icon = menuAction.getIconPainter(),
            addSeparator = false,
            isDestructive = false,
            onActionClicked = { it(menuAction) }
        )
    }
}