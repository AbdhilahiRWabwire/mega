package mega.privacy.android.app.presentation.search

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.node.NodeActionsViewModel
import mega.privacy.android.app.presentation.node.NodeOptionsBottomSheetViewModel
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.app.presentation.search.model.navigation.removeNodeLinkDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.cannotOpenFileDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.cannotVerifyUserNavigation
import mega.privacy.android.app.presentation.search.navigation.changeLabelBottomSheetNavigation
import mega.privacy.android.app.presentation.search.navigation.changeNodeExtensionDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.foreignNodeDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.leaveFolderShareDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.moveToRubbishOrDeleteNavigation
import mega.privacy.android.app.presentation.search.navigation.nodeBottomSheetNavigation
import mega.privacy.android.app.presentation.search.navigation.overQuotaDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.removeShareFolderDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.renameDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.shareFolderAccessDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.shareFolderDialogNavigation
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper


/**
 * Navigation graph for Search
 *
 * @param trackAnalytics Function to track analytics
 * @param showSortOrderBottomSheet Function to show sort order bottom sheet
 * @param navigateToLink Function to navigate to link
 * @param handleClick Function to handle click
 * @param navHostController Navigation controller
 * @param nodeActionHandler Node bottom sheet action handler
 * @param searchActivityViewModel Search activity view model
 * @param onBackPressed OnBackPressed
 * @param nodeActionsViewModel
 * @param listToStringWithDelimitersMapper
 */
internal fun NavGraphBuilder.searchNavGraph(
    trackAnalytics: (SearchFilter?) -> Unit,
    showSortOrderBottomSheet: () -> Unit,
    navigateToLink: (String) -> Unit,
    handleClick: (TypedNode?) -> Unit,
    navHostController: NavHostController,
    nodeActionHandler: NodeActionHandler,
    searchActivityViewModel: SearchActivityViewModel,
    nodeOptionsBottomSheetViewModel: NodeOptionsBottomSheetViewModel,
    onBackPressed: () -> Unit,
    nodeActionsViewModel: NodeActionsViewModel,
    listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper
) {
    composable(searchRoute) {
        SearchScreen(
            trackAnalytics = trackAnalytics,
            handleClick = handleClick,
            navigateToLink = navigateToLink,
            showSortOrderBottomSheet = showSortOrderBottomSheet,
            navHostController = navHostController,
            searchActivityViewModel = searchActivityViewModel,
            onBackPressed = onBackPressed,
            nodeActionHandler = nodeActionHandler,
        )
    }
    moveToRubbishOrDeleteNavigation(
        navHostController = navHostController,
        listToStringWithDelimitersMapper = listToStringWithDelimitersMapper
    )
    renameDialogNavigation(
        navHostController = navHostController,
    )
    nodeBottomSheetNavigation(
        nodeActionHandler = nodeActionHandler,
        navHostController = navHostController,
        nodeOptionsBottomSheetViewModel = nodeOptionsBottomSheetViewModel
    )
    changeLabelBottomSheetNavigation(
        navHostController = navHostController,
        nodeOptionsBottomSheetViewModel = nodeOptionsBottomSheetViewModel
    )
    changeNodeExtensionDialogNavigation(
        navHostController = navHostController,
        nodeOptionsBottomSheetViewModel = nodeOptionsBottomSheetViewModel
    )
    cannotVerifyUserNavigation(navHostController = navHostController)
    removeNodeLinkDialogNavigation(
        navHostController = navHostController,
    )
    shareFolderDialogNavigation(
        navHostController = navHostController,
        nodeActionHandler = nodeActionHandler,
        stringWithDelimitersMapper = listToStringWithDelimitersMapper
    )
    removeShareFolderDialogNavigation(
        navHostController = navHostController,
        searchActivityViewModel = searchActivityViewModel,
        nodeOptionsBottomSheetViewModel = nodeOptionsBottomSheetViewModel
    )
    leaveFolderShareDialogNavigation(
        navHostController = navHostController,
        searchActivityViewModel = searchActivityViewModel,
        nodeOptionsBottomSheetViewModel = nodeOptionsBottomSheetViewModel
    )
    overQuotaDialogNavigation(navHostController = navHostController)
    foreignNodeDialogNavigation(navHostController = navHostController)
    shareFolderAccessDialogNavigation(
        navHostController = navHostController,
        listToStringWithDelimitersMapper = listToStringWithDelimitersMapper
    )
    cannotOpenFileDialogNavigation(
        navHostController = navHostController,
        nodeActionsViewModel = nodeActionsViewModel,
    )
}

/**
 * Route for Search
 */
internal const val searchRoute = "search/main"
internal const val isFromToolbar = "isFromToolbar"
internal const val nodeListHandle = "nodeListHandle"