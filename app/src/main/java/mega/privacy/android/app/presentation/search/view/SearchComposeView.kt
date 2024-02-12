package mega.privacy.android.app.presentation.search.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.search.SearchActivity
import mega.privacy.android.app.presentation.search.model.SearchActivityState
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.app.presentation.view.NodesView
import mega.privacy.android.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyViewForSearch

/**
 * View for Search compose
 * @param state [SearchActivityState]
 * @param sortOrder String
 * @param onItemClick item click listener
 * @param onLongClick item long click listener
 * @param onMenuClick item menu click listener
 * @param onSortOrderClick Change sort order click listener
 * @param onChangeViewTypeClick change view type click listener
 * @param onLinkClicked link click listener for item
 * @param onDisputeTakeDownClicked dispute take-down click listener
 */
@Composable
fun SearchComposeView(
    state: SearchActivityState,
    sortOrder: String,
    onItemClick: (NodeUIItem<TypedNode>) -> Unit,
    onLongClick: (NodeUIItem<TypedNode>) -> Unit,
    onMenuClick: (NodeUIItem<TypedNode>) -> Unit,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    onLinkClicked: (String) -> Unit,
    onDisputeTakeDownClicked: (String) -> Unit,
    onErrorShown: () -> Unit,
    updateFilter: (SearchFilter) -> Unit,
    trackAnalytics: (SearchFilter) -> Unit,
    updateSearchQuery: (String) -> Unit,
    onBackPressed: () -> Unit,
    navHostController: NavHostController,
    modifier: Modifier = Modifier,
    nodeActionHandler: NodeActionHandler,
) {
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val scaffoldState = rememberScaffoldState()
    val snackBarHostState = remember { SnackbarHostState() }

    state.errorMessageId?.let {
        val errorMessage = stringResource(id = it)
        LaunchedEffect(key1 = scaffoldState.snackbarHostState) {
            val s = scaffoldState.snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Long
            )
            if (s == SnackbarResult.Dismissed) {
                onErrorShown()
            }

        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            SearchToolBar(
                searchQuery = state.searchQuery,
                updateSearchQuery = updateSearchQuery,
                onBackPressed = onBackPressed,
                selectedNodes = state.selectedNodes,
                totalCount = state.searchItemList.size,
                navHostController = navHostController,
                nodeActionHandler = nodeActionHandler,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState) { data ->
                MegaSnackbar(snackbarData = data)
            }
        }
    ) { padding ->
        Column {
            if (state.nodeSourceType == NodeSourceType.CLOUD_DRIVE) {
                SearchFilterChipsView(
                    filters = state.filters,
                    selectedFilter = state.selectedFilter,
                    updateFilter = {
                        trackAnalytics(it)
                        updateFilter(it)
                    }
                )
            }
            if (state.isSearching) {
                LoadingStateView(
                    isList = state.currentViewType == ViewType.LIST,
                    modifier = Modifier
                )
            } else {
                if (state.searchItemList.isNotEmpty()) {
                    NodesView(
                        nodeUIItems = state.searchItemList,
                        onMenuClick = onMenuClick,
                        onItemClicked = onItemClick,
                        onLongClick = onLongClick,
                        sortOrder = sortOrder,
                        isListView = state.currentViewType == ViewType.LIST,
                        onSortOrderClick = onSortOrderClick,
                        onChangeViewTypeClick = onChangeViewTypeClick,
                        onLinkClicked = onLinkClicked,
                        onDisputeTakeDownClicked = onDisputeTakeDownClicked,
                        listState = listState,
                        gridState = gridState,
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    LegacyMegaEmptyViewForSearch(
                        imagePainter = painterResource(
                            id = state.emptyState?.first ?: R.drawable.ic_empty_search
                        ),
                        text = state.emptyState?.second
                            ?: stringResource(id = R.string.search_empty_screen_no_results)
                    )
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewSearchComposeView() {
    SearchComposeView(
        state = SearchActivityState(),
        sortOrder = SortOrder.ORDER_NONE.toString(),
        onItemClick = {},
        onLongClick = {},
        onMenuClick = {},
        onSortOrderClick = { },
        onChangeViewTypeClick = { },
        onLinkClicked = {},
        onDisputeTakeDownClicked = {},
        onErrorShown = {},
        updateFilter = {},
        trackAnalytics = {},
        updateSearchQuery = {},
        onBackPressed = {},
        navHostController = NavHostController(LocalContext.current),
        modifier = Modifier,
        nodeActionHandler = NodeActionHandler(
            LocalContext.current as SearchActivity,
            hiltViewModel()
        )
    )
}
