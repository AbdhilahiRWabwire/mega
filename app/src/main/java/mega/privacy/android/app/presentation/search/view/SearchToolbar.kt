package mega.privacy.android.app.presentation.search.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.node.view.ToolbarMenuItem
import mega.privacy.android.app.presentation.node.view.toolbar.NodeToolbarViewModel
import mega.privacy.android.app.presentation.search.SearchActivity
import mega.privacy.android.core.ui.controls.appbar.SelectModeAppBar
import mega.privacy.android.core.ui.model.MenuActionWithClick
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.legacy.core.ui.controls.appbar.ExpandedSearchAppBar
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Search toolbar used in search activity
 *
 * @param searchQuery
 * @param updateSearchQuery
 * @param selectedNodes
 * @param totalCount
 * @param toolbarViewModel
 * @param onBackPressed

 */
@Composable
fun SearchToolBar(
    searchQuery: String,
    updateSearchQuery: (String) -> Unit,
    selectedNodes: Set<TypedNode>,
    totalCount: Int,
    onBackPressed: () -> Unit,
    navHostController: NavHostController,
    nodeActionHandler: NodeActionHandler,
    clearSelection: () -> Unit,
    toolbarViewModel: NodeToolbarViewModel = hiltViewModel(),
) {
    LaunchedEffect(key1 = selectedNodes.size) {
        toolbarViewModel.updateToolbarState(
            selectedNodes = selectedNodes,
            resultCount = totalCount,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )
    }
    val toolbarState by toolbarViewModel.state.collectAsStateWithLifecycle()
    SearchToolbarBody(
        searchQuery = searchQuery,
        updateSearchQuery = updateSearchQuery,
        selectedNodes = selectedNodes,
        menuActions = toolbarState.toolbarMenuItems,
        onBackPressed = onBackPressed,
        navHostController = navHostController,
        handler = nodeActionHandler,
        clearSelection = clearSelection,
    )
}

@Composable
private fun SearchToolbarBody(
    searchQuery: String,
    menuActions: List<ToolbarMenuItem>,
    updateSearchQuery: (String) -> Unit,
    selectedNodes: Set<TypedNode>,
    onBackPressed: () -> Unit,
    navHostController: NavHostController,
    handler: NodeActionHandler,
    clearSelection: () -> Unit,
) {
    if (selectedNodes.isNotEmpty()) {
        val actions = menuActions.map {
            MenuActionWithClick(
                menuAction = it.action,
                onClick = it.control(clearSelection, handler::handleAction, navHostController)
            )
        }
        SelectModeAppBar(
            title = "${selectedNodes.size}",
            actions = actions,
            onNavigationPressed = { onBackPressed() }
        )
    } else {
        ExpandedSearchAppBar(
            text = searchQuery,
            hintId = R.string.hint_action_search,
            onSearchTextChange = { updateSearchQuery(it) },
            onCloseClicked = { onBackPressed() },
            elevation = false
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewSearchToolbarBody() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        SearchToolbarBody(
            searchQuery = "searchQuery",
            menuActions = emptyList(),
            updateSearchQuery = {},
            selectedNodes = emptySet(),
            onBackPressed = {},
            navHostController = NavHostController(LocalContext.current),
            handler = NodeActionHandler(
                LocalContext.current as SearchActivity,
                hiltViewModel(),
            ),
            clearSelection = {}
        )
    }
}