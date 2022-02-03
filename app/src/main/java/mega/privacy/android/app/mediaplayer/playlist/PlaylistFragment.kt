package mega.privacy.android.app.mediaplayer.playlist

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.util.RepeatModeUtil
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentAudioPlaylistBinding
import mega.privacy.android.app.mediaplayer.MediaMegaPlayer
import mega.privacy.android.app.mediaplayer.MediaPlayerActivity
import mega.privacy.android.app.mediaplayer.service.*
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.autoCleared

/**
 * Playlist fragment for displaying the playlist of audios or videos
 */
class PlaylistFragment : Fragment(), PlaylistItemOperation, DragStartListener {
    private var binding by autoCleared<FragmentAudioPlaylistBinding>()

    private var playerService: MediaPlayerService? = null

    private lateinit var adapter: PlaylistAdapter
    private lateinit var listLayoutManager: LinearLayoutManager

    private var playlistObserved = false

    private var itemTouchHelper: ItemTouchHelper? = null
    private var actionMode: ActionMode? = null
    private var playlistActionBarCallback: PlaylistActionBarCallback? = null
    private var isActionMode = false

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
        }

        /**
         * Called after a successful bind with our AudioPlayerService.
         */
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MediaPlayerServiceBinder) {
                playerService = service.service

                if (service.service.viewModel.audioPlayer) {
                    setupPlayerView(service.service.player)
                } else {
                    binding.playerView.isVisible = false

                    val layoutParams = binding.playlist.layoutParams as FrameLayout.LayoutParams
                    layoutParams.bottomMargin = 0
                    binding.playlist.layoutParams = layoutParams
                }
                tryObservePlaylist()
                // Initial item touch helper after the service is connected.
                setupItemTouchHelper()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAudioPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MediaPlayerActivity).showToolbar(false)
        context?.let {
            adapter = PlaylistAdapter(
                it,
                this,
                (requireActivity() as MediaPlayerActivity).isAudioPlayer(),
                dragStartListener = this
            )
        }
        binding.playlist.setHasFixedSize(true)
        listLayoutManager = LinearLayoutManager(requireContext())
        binding.playlist.layoutManager = listLayoutManager
        binding.playlist.adapter = adapter

        binding.playlist.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                (requireActivity() as MediaPlayerActivity).setupToolbarColors(
                    recyclerView.canScrollVertically(-1)
                )
            }
        })
        tryObservePlaylist()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.select -> {
                // Activate the action mode for selecting the tracks
                playerService?.viewModel?.setActionMode(true)
            }
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val isAudioPlayer = MediaPlayerActivity.isAudioPlayer(requireActivity().intent)
        val playerServiceIntent = Intent(
            requireContext(),
            if (isAudioPlayer) AudioPlayerService::class.java else VideoPlayerService::class.java
        )
        playerServiceIntent.putExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, false)
        requireContext().bindService(playerServiceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playlistObserved = false
    }

    override fun onDestroy() {
        super.onDestroy()
        playerService = null
        requireContext().unbindService(connection)
    }

    /**
     * Initial the item touch helper and attach to recycle view.
     */
    private fun setupItemTouchHelper(){
        playerService?.viewModel?.run {
            val itemTouchCallBack = PlaylistItemTouchCallBack(adapter, this)
            itemTouchHelper = ItemTouchHelper(itemTouchCallBack)
        }
        itemTouchHelper?.attachToRecyclerView(binding.playlist)
    }

    /**
     * Observe playlist LiveData when view is created and service is connected.
     */
    private fun tryObservePlaylist() {
        val service = playerService
        if (!playlistObserved && service != null && view != null) {
            playlistObserved = true
            service.viewModel.run {
                playlist.observe(viewLifecycleOwner) {
                    adapter.paused = service.viewModel.paused

                    adapter.submitList(it.first) {
                        if (it.second != -1) {
                            listLayoutManager.scrollToPositionWithOffset(it.second, 0)
                        }
                        if (!isVideoPlayer() && it.first.isNotEmpty()) {
                            // Trigger the visibility update of the pause icon of the
                            // playing (paused) audio.
                            adapter.notifyItemChanged(it.second + 1)
                        }
                    }
                }
                playlistTitle.observe(viewLifecycleOwner) {
                    (requireActivity() as MediaPlayerActivity).setToolbarTitle(it)
                }
                itemsSelectedCount.observe(viewLifecycleOwner) {
                    actionMode?.run {
                        // Set title according to the number of selected tracks
                        if (it <= 0) {
                            title = resources.getString(R.string.title_select_tracks)
                            menu.findItem(R.id.remove).isVisible = false
                        } else {
                            title = it.toString()
                            menu.findItem(R.id.remove).isVisible = true
                        }
                    }
                }
                isActionMode.observe(viewLifecycleOwner) {
                    if (it) {
                        activateActionMode()
                        itemTouchHelper?.attachToRecyclerView(null)
                    } else {
                        actionMode?.finish()
                        itemTouchHelper?.attachToRecyclerView(binding.playlist)
                    }
                    this@PlaylistFragment.isActionMode = it
                }
            }
        }
    }

    /**
     * Activated the action mode
     */
    private fun activateActionMode(){
        if (playlistActionBarCallback == null) {
            playerService?.viewModel?.run {
                playlistActionBarCallback = PlaylistActionBarCallback(this)
            }
        }
        playlistActionBarCallback?.run {
            actionMode = (context as AppCompatActivity).startSupportActionMode(this)
            actionMode?.title = resources.getString(R.string.title_select_tracks)
        }
    }

    /**
     * Setup PlayerView
     * @param player MediaMegaPlayer
     */
    private fun setupPlayerView(player: MediaMegaPlayer) {
        binding.playerView.run {
            this.player = player
            useController = true
            controllerShowTimeoutMs = 0
            controllerHideOnTouch = false
            setShowShuffleButton(true)
            setRepeatToggleModes(
                RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE or RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL
            )
            showController()
        }
    }

    override fun onItemClick(
        view: View,
        item: PlaylistItem,
        holder: PlaylistViewHolder,
        position: Int
    ) {
        playerService?.run {
            if (viewModel.isActionMode.value == true) {
                viewModel.itemSelected(item.nodeHandle)
                adapter.startAnimation(holder, position)
            } else {
                if (view.id == R.id.transfers_list_option_reorder) {
                    return
                }
                if (!CallUtil.participatingInACall()) {
                   seekTo(viewModel.getIndexFromPlaylistItems(item))
                }
                (requireActivity() as MediaPlayerActivity).closeSearch()

                if (isVideoPlayer()) {
                    (requireActivity() as MediaPlayerActivity).onBackPressed()
                }
            }
        }
    }

    private fun isVideoPlayer() = playerService?.viewModel?.audioPlayer == false

    override fun onDragStarted(holder: PlaylistViewHolder) {
        if (!isActionMode) {
            itemTouchHelper?.startDrag(holder)
        }
    }
}
