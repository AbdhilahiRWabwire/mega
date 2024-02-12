package mega.privacy.android.app.presentation.search

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.node.NodeActionsViewModel
import mega.privacy.android.app.presentation.node.NodeOptionsBottomSheetViewModel
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Search nav host controller
 *
 * @param viewModel Search activity view model
 * @param handleClick Function to handle click
 * @param navigateToLink Function to navigate to link
 * @param showSortOrderBottomSheet Function to show sort order bottom sheet
 * @param trackAnalytics Function to track analytics
 * @param nodeActionHandler Node bottom sheet action handler
 * @param modifier Modifier
 */
@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
internal fun SearchNavHostController(
    viewModel: SearchActivityViewModel,
    nodeOptionsBottomSheetViewModel: NodeOptionsBottomSheetViewModel,
    handleClick: (TypedNode?) -> Unit,
    navigateToLink: (String) -> Unit,
    showSortOrderBottomSheet: () -> Unit,
    trackAnalytics: (SearchFilter?) -> Unit,
    onBackPressed: () -> Unit,
    nodeActionHandler: NodeActionHandler,
    navHostController: NavHostController,
    bottomSheetNavigator: BottomSheetNavigator,
    modifier: Modifier = Modifier,
    nodeActionsViewModel: NodeActionsViewModel,
) {
    ModalBottomSheetLayout(
        modifier = modifier.navigationBarsPadding(),
        bottomSheetNavigator = bottomSheetNavigator,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        NavHost(
            modifier = modifier.navigationBarsPadding(),
            navController = navHostController,
            startDestination = searchRoute
        ) {
            searchNavGraph(
                handleClick = handleClick,
                navigateToLink = navigateToLink,
                showSortOrderBottomSheet = showSortOrderBottomSheet,
                trackAnalytics = trackAnalytics,
                navHostController = navHostController,
                searchActivityViewModel = viewModel,
                nodeActionHandler = nodeActionHandler,
                onBackPressed = onBackPressed,
                nodeOptionsBottomSheetViewModel = nodeOptionsBottomSheetViewModel,
                nodeActionsViewModel = nodeActionsViewModel
            )
        }
    }
}