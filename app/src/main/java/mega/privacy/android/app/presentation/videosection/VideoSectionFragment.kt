package mega.privacy.android.app.presentation.videosection

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.FragmentVideoSectionBinding
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.HomepageSearchable
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.mediaplayer.miniplayer.MiniAudioPlayerController
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.mapper.GetOptionsForToolbarMapper
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTab
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.presentation.videosection.view.VideoSectionFeatureScreen
import mega.privacy.android.app.presentation.videosection.view.playlist.videoPlaylistDetailRoute
import mega.privacy.android.app.presentation.videosection.view.videoSectionRoute
import mega.privacy.android.app.utils.Constants.AUTHORITY_STRING_FILE_PROVIDER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_POSITION
import mega.privacy.android.app.utils.Constants.ORDER_CLOUD
import mega.privacy.android.app.utils.Constants.ORDER_VIDEO_PLAYLIST
import mega.privacy.android.app.utils.Constants.SEARCH_BY_ADAPTER
import mega.privacy.android.app.utils.Constants.VIDEO_BROWSE_ADAPTER
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.callManager
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.theme.MegaAppTheme
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * The Fragment for video section
 */
@AndroidEntryPoint
class VideoSectionFragment : Fragment(), HomepageSearchable {

    private val videoSectionViewModel by viewModels<VideoSectionViewModel>()
    private val sortByHeaderViewModel: SortByHeaderViewModel by activityViewModels()

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * Mapper to get options for Action Bar
     */
    @Inject
    lateinit var getOptionsForToolbarMapper: GetOptionsForToolbarMapper

    private var _binding: FragmentVideoSectionBinding? = null
    private val binding get() = _binding!!

    private var actionMode: ActionMode? = null

    private val playlistMenuMoreProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.fragment_playlist_menu_more, menu)
            menu.findItem(R.id.menu_playlist_more).setOnMenuItemClickListener {
                videoSectionViewModel.setShouldShowMoreVideoPlaylistOptions(true)
                true
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean = true
    }

    private val videoSelectedActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    result.data?.getStringArrayListExtra(
                        VideoSelectedActivity.INTENT_KEY_VIDEO_SELECTED
                    )?.let { items ->
                        videoSectionViewModel.state.value.currentVideoPlaylist?.let { currentPlaylist ->
                            videoSectionViewModel.addVideosToPlaylist(
                                currentPlaylist.id,
                                items.map { NodeId(it.toLong()) })
                        }

                    }
                }
            }
        }

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentVideoSectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initVideoSectionComposeView()
        setupMiniAudioPlayer()

        sortByHeaderViewModel.orderChangeEvent.observe(
            viewLifecycleOwner, EventObserver { videoSectionViewModel.refreshWhenOrderChanged() }
        )

        viewLifecycleOwner.collectFlow(
            videoSectionViewModel.state.map { it.isPendingRefresh }.distinctUntilChanged()
        ) { isPendingRefresh ->
            if (isPendingRefresh) {
                with(videoSectionViewModel) {
                    refreshNodes()
                    markHandledPendingRefresh()
                }
            }
        }

        viewLifecycleOwner.collectFlow(
            videoSectionViewModel.state.map { it.allVideos }.distinctUntilChanged()
        ) { list ->
            if (!videoSectionViewModel.state.value.searchMode && list.isNotEmpty()) {
                callManager {
                    it.invalidateOptionsMenu()
                }
            }
        }

        viewLifecycleOwner.collectFlow(
            videoSectionViewModel.state.map { it.searchMode }.distinctUntilChanged()
        ) { isSearchMode ->
            if (!isSearchMode) {
                (activity as? ManagerActivity)?.closeSearchView()
            }
        }

        viewLifecycleOwner.collectFlow(
            videoSectionViewModel.state.map { it.currentDestinationRoute }.distinctUntilChanged(),
            minActiveState = Lifecycle.State.CREATED
        ) { route ->
            route?.let { updateToolbarWhenDestinationChanged(it) }
        }

        viewLifecycleOwner.collectFlow(
            videoSectionViewModel.state.map { it.actionMode }.distinctUntilChanged()
        ) { isActionMode ->
            if (!isActionMode) {
                actionMode?.finish()
            }
        }

        viewLifecycleOwner.collectFlow(
            videoSectionViewModel.state.map { it.isVideoPlaylistCreatedSuccessfully }
                .distinctUntilChanged()
        ) { isSuccess ->
            if (isSuccess) {
                navigateToVideoSelectedActivity()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            merge(
                videoSectionViewModel.state.map { it.selectedVideoHandles }.distinctUntilChanged(),
                videoSectionViewModel.state.map { it.selectedVideoPlaylistHandles }
                    .distinctUntilChanged(),
                videoSectionViewModel.state.map { it.selectedVideoElementIDs }
                    .distinctUntilChanged()
            ).collectLatest { list ->
                updateActionModeTitle(count = list.size)
            }
        }
    }

    private fun initVideoSectionComposeView() {
        binding.videoSectionComposeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val uiState by videoSectionViewModel.state.collectAsStateWithLifecycle()
                MegaAppTheme(isDark = themeMode.isDarkMode()) {
                    VideoSectionFeatureScreen(
                        onSortOrderClick = { showSortByPanel() },
                        videoSectionViewModel = videoSectionViewModel,
                        onClick = { item, index ->
                            if (uiState.isInSelection) {
                                videoSectionViewModel.onItemClicked(item, index)
                            } else {
                                openVideoFile(
                                    activity = requireActivity(),
                                    item = item,
                                    index = index
                                )
                            }
                        },
                        onLongClick = { item, index ->
                            activateActionMode()
                            videoSectionViewModel.onItemClicked(item, index)
                        },
                        onMenuClick = { item ->
                            showOptionsMenuForItem(item)
                        },
                        onAddElementsClicked = {
                            navigateToVideoSelectedActivity()
                        },
                        onPlaylistDetailItemClick = { item, index ->
                            if (uiState.isInSelection) {
                                videoSectionViewModel.onVideoItemOfPlaylistClicked(item, index)
                            } else {
                                openVideoFile(
                                    activity = requireActivity(),
                                    item = item,
                                    index = index
                                )
                            }
                        },
                        onPlaylistItemLongClick = { item, index ->
                            activateVideoPlaylistActionMode(ACTION_TYPE_VIDEO_PLAYLIST)
                            videoSectionViewModel.onVideoPlaylistItemClicked(item, index)
                        },
                        onPlaylistDetailItemLongClick = { item, index ->
                            activateVideoPlaylistActionMode(ACTION_TYPE_VIDEO_PLAYLIST_DETAIL)
                            videoSectionViewModel.onVideoItemOfPlaylistClicked(item, index)
                        },
                        onActionModeFinished = { actionMode?.finish() },
                        onPlayAllClicked = {
                            uiState.currentVideoPlaylist?.videos?.firstOrNull()?.let {
                                openVideoFile(activity = requireActivity(), item = it, index = 0)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun navigateToVideoSelectedActivity() {
        videoSelectedActivityLauncher.launch(
            Intent(
                requireActivity(),
                VideoSelectedActivity::class.java
            )
        )
    }

    private fun showSortByPanel() {
        val currentSelectTab = videoSectionViewModel.tabState.value.selectedTab
        (requireActivity() as ManagerActivity).showNewSortByPanel(
            if (currentSelectTab == VideoSectionTab.All) ORDER_CLOUD else ORDER_VIDEO_PLAYLIST
        )
    }

    private fun openVideoFile(
        activity: Activity,
        item: VideoUIEntity,
        index: Int,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val nodeHandle = item.id.longValue
            val nodeName = item.name
            val intent = getIntent(item = item, index = index)

            activity.startActivity(
                videoSectionViewModel.isLocalFile(nodeHandle)?.let { localPath ->
                    File(localPath).let { mediaFile ->
                        runCatching {
                            FileProvider.getUriForFile(
                                activity,
                                AUTHORITY_STRING_FILE_PROVIDER,
                                mediaFile
                            )
                        }.onFailure {
                            Uri.fromFile(mediaFile)
                        }.map { mediaFileUri ->
                            intent.setDataAndType(
                                mediaFileUri,
                                MimeTypeList.typeForName(nodeName).type
                            )
                            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                    }
                    intent
                } ?: videoSectionViewModel.updateIntent(
                    handle = nodeHandle,
                    name = nodeName,
                    intent = intent
                )
            )
        }
    }

    private fun getIntent(
        item: VideoUIEntity,
        index: Int,
    ) = Util.getMediaIntent(activity, item.name).apply {
        val state = videoSectionViewModel.state.value
        putExtra(INTENT_EXTRA_KEY_POSITION, index)
        putExtra(INTENT_EXTRA_KEY_HANDLE, item.id.longValue)
        putExtra(INTENT_EXTRA_KEY_FILE_NAME, item.name)
        when (state.currentDestinationRoute) {
            videoSectionRoute ->
                if (state.searchMode) {
                    putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, SEARCH_BY_ADAPTER)
                    putExtra(
                        INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH,
                        state.allVideos.map { it.id.longValue }.toLongArray()
                    )
                } else {
                    putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, VIDEO_BROWSE_ADAPTER)
                }

            videoPlaylistDetailRoute -> {
                putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, SEARCH_BY_ADAPTER)
                state.currentVideoPlaylist?.videos?.map { it.id.longValue }?.let {
                    putExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH, it.toLongArray())
                }
            }
        }
        putExtra(
            INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
            sortByHeaderViewModel.cloudSortOrder.value
        )
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    private fun activateActionMode() {
        if (actionMode == null) {
            actionMode =
                (requireActivity() as? AppCompatActivity)?.startSupportActionMode(
                    VideoSectionActionModeCallback(
                        managerActivity = requireActivity() as ManagerActivity,
                        childFragmentManager = childFragmentManager,
                        videoSectionViewModel = videoSectionViewModel,
                        getOptionsForToolbarMapper = getOptionsForToolbarMapper
                    ) {
                        disableSelectMode()
                    }
                )
            videoSectionViewModel.setActionMode(true)
        }
    }

    private fun activateVideoPlaylistActionMode(actionType: Int) {
        if (actionMode == null) {
            actionMode =
                (requireActivity() as? AppCompatActivity)?.startSupportActionMode(
                    VideoPlaylistActionMode(
                        managerActivity = requireActivity() as ManagerActivity,
                        videoSectionViewModel = videoSectionViewModel,
                        actionType = actionType
                    ) {
                        disableSelectMode()
                    }
                )
            videoSectionViewModel.setActionMode(true)
        }
    }

    private fun updateActionModeTitle(count: Int) {
        if (count == 0) actionMode?.finish()
        actionMode?.title = count.toString()

        runCatching {
            actionMode?.invalidate()
        }.onFailure {
            Timber.e(it, "Invalidate error")
        }
    }

    private fun showOptionsMenuForItem(item: VideoUIEntity) {
        (requireActivity() as ManagerActivity).showNodeOptionsPanel(
            nodeId = item.id,
            mode = NodeOptionsBottomSheetDialogFragment.CLOUD_DRIVE_MODE
        )
    }

    private fun updateToolbarWhenDestinationChanged(route: String) {
        (activity as? ManagerActivity)?.let { managerActivity ->
            when (route) {
                videoSectionRoute -> {
                    managerActivity.setToolbarTitle(getString(R.string.sortby_type_video_first))
                    managerActivity.removeMenuProvider(playlistMenuMoreProvider)
                }

                videoPlaylistDetailRoute -> {
                    managerActivity.addMenuProvider(playlistMenuMoreProvider)
                    managerActivity.setToolbarTitle("")
                }
            }
            managerActivity.invalidateOptionsMenu()
        }
    }

    private fun disableSelectMode() {
        actionMode = null
        videoSectionViewModel.clearAllSelectedVideos()
        videoSectionViewModel.clearAllSelectedVideoPlaylists()
        videoSectionViewModel.clearAllSelectedVideosOfPlaylist()
        videoSectionViewModel.setActionMode(false)
    }

    private fun setupMiniAudioPlayer() {
        val audioPlayerController = MiniAudioPlayerController(binding.miniAudioPlayer).apply {
            shouldVisible = true
        }
        lifecycle.addObserver(audioPlayerController)
    }

    /**
     * onDestroyView
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Should show search menu
     *
     * @return true if should show search menu, false otherwise
     */
    override fun shouldShowSearchMenu(): Boolean = videoSectionViewModel.shouldShowSearchMenu()

    /**
     * Search ready
     */
    override fun searchReady() {
        videoSectionViewModel.searchReady()
    }

    /**
     * Search query
     *
     * @param query query string
     */
    override fun searchQuery(query: String) {
        videoSectionViewModel.searchQuery(query)
    }

    /**
     * Exit search
     */
    override fun exitSearch() {
        videoSectionViewModel.exitSearch()
    }

    companion object {
        /**
         * The action type for video playlist
         */
        const val ACTION_TYPE_VIDEO_PLAYLIST = 10

        /**
         * The action type for video playlist detail
         */
        const val ACTION_TYPE_VIDEO_PLAYLIST_DETAIL = 11
    }
}