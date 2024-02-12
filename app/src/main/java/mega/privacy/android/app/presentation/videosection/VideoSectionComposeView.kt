package mega.privacy.android.app.presentation.videosection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.presentation.videosection.model.UIVideo
import mega.privacy.android.app.presentation.videosection.model.UIVideoPlaylist
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTab

internal const val videoSectionRoute = "videoSection/video_section"

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun VideoSectionComposeView(
    videoSectionViewModel: VideoSectionViewModel,
    onClick: (item: UIVideo, index: Int) -> Unit,
    onSortOrderClick: () -> Unit = {},
    onMenuClick: (UIVideo) -> Unit = {},
    onLongClick: (item: UIVideo, index: Int) -> Unit = { _, _ -> },
    onPlaylistItemClick: (item: UIVideoPlaylist, index: Int) -> Unit = { _, _ -> },
    onPlaylistItemMenuClick: (UIVideoPlaylist) -> Unit = { _ -> },
    onPlaylistItemLongClick: (UIVideoPlaylist, index: Int) -> Unit = { _, _ -> },
) {
    val uiState by videoSectionViewModel.state.collectAsStateWithLifecycle()
    val tabState by videoSectionViewModel.tabState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val allLazyListState = rememberLazyListState()
    val playlistsLazyListState = rememberLazyListState()

    val pagerState = rememberPagerState(
        initialPage = tabState.selectedTab.ordinal,
        initialPageOffsetFraction = 0f
    ) {
        tabState.tabs.size
    }

    LaunchedEffect(Unit) {
        videoSectionViewModel.updateCurrentVideoPlaylist(null)
    }

    LaunchedEffect(pagerState.currentPage) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            videoSectionViewModel.onTabSelected(selectTab = tabState.tabs[page])
            val tab = VideoSectionTab.entries[page]
            pagerState.scrollToPage(tab.ordinal)
        }
    }

    VideoSectionBodyView(
        pagerState = pagerState,
        tabs = tabState.tabs,
        allVideoView = {
            AllVideosView(
                items = uiState.allVideos,
                progressBarShowing = uiState.progressBarShowing,
                searchMode = uiState.searchMode,
                scrollToTop = uiState.scrollToTop,
                lazyListState = allLazyListState,
                sortOrder = stringResource(
                    id = SortByHeaderViewModel.orderNameMap[uiState.sortOrder]
                        ?: R.string.sortby_name
                ),
                modifier = Modifier,
                onSortOrderClick = onSortOrderClick,
                onClick = onClick,
                onLongClick = onLongClick,
                onMenuClick = onMenuClick
            )
        },
        playlistsView = {
            VideoPlaylistsView(
                items = uiState.videoPlaylists,
                progressBarShowing = uiState.isPlaylistProgressBarShown,
                searchMode = uiState.searchMode,
                scrollToTop = uiState.scrollToTop,
                lazyListState = playlistsLazyListState,
                sortOrder = stringResource(
                    id = SortByHeaderViewModel.orderNameMap[uiState.sortOrder]
                        ?: R.string.sortby_name
                ),
                modifier = Modifier,
                onSortOrderClick = onSortOrderClick,
                onClick = onPlaylistItemClick,
                onLongClick = onPlaylistItemLongClick,
                onMenuClick = onPlaylistItemMenuClick
            )
        },
        selectedTab = tabState.selectedTab,
        allLazyListState = allLazyListState,
        playlistsLazyListState = playlistsLazyListState,
        onTabSelected = { tab ->
            videoSectionViewModel.onTabSelected(selectTab = tab)
            coroutineScope.launch {
                pagerState.scrollToPage(tab.ordinal)
            }
        }
    )
}