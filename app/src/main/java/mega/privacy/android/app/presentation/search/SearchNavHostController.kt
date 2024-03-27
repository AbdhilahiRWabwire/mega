package mega.privacy.android.app.presentation.search

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.node.NodeActionsViewModel
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.core.ui.controls.sheets.MegaBottomSheetLayout
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper

/**
 * Search nav host controller
 *
 * @param viewModel Search activity view model
 * @param handleClick Function to handle click
 * @param navigateToLink Function to navigate to link
 * @param showSortOrderBottomSheet Function to show sort order bottom sheet
 * @param trackAnalytics Function to track analytics
 * @param onBackPressed
 * @param nodeActionHandler Node bottom sheet action handler
 * @param navHostController
 * @param bottomSheetNavigator
 * @param nodeActionsViewModel
 * @param listToStringWithDelimitersMapper
 * @param modifier Modifier
 */
@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
internal fun SearchNavHostController(
    viewModel: SearchActivityViewModel,
    navigateToLink: (String) -> Unit,
    showSortOrderBottomSheet: () -> Unit,
    trackAnalytics: (SearchFilter?) -> Unit,
    onBackPressed: () -> Unit,
    nodeActionHandler: NodeActionHandler,
    navHostController: NavHostController,
    bottomSheetNavigator: BottomSheetNavigator,
    nodeActionsViewModel: NodeActionsViewModel,
    listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper,
    handleClick: (TypedNode?) -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaBottomSheetLayout(
        modifier = modifier.navigationBarsPadding(),
        bottomSheetNavigator = bottomSheetNavigator,
    ) {
        NavHost(
            modifier = modifier.navigationBarsPadding(),
            navController = navHostController,
            startDestination = searchRoute
        ) {
            searchNavGraph(
                navigateToLink = navigateToLink,
                showSortOrderBottomSheet = showSortOrderBottomSheet,
                trackAnalytics = trackAnalytics,
                navHostController = navHostController,
                searchActivityViewModel = viewModel,
                nodeActionHandler = nodeActionHandler,
                onBackPressed = onBackPressed,
                nodeActionsViewModel = nodeActionsViewModel,
                handleClick = handleClick,
                listToStringWithDelimitersMapper = listToStringWithDelimitersMapper
            )
        }
    }
}