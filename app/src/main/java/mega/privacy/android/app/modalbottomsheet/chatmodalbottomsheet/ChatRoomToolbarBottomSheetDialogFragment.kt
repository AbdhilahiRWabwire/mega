package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import mega.privacy.android.app.R
import mega.privacy.android.app.adapters.FileStorageAdapter
import mega.privacy.android.app.databinding.BottomSheetChatRoomToolbarBinding
import mega.privacy.android.app.interfaces.ChatRoomToolbarBottomSheetDialogActionListener
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop
import mega.privacy.android.app.lollipop.megachat.FileGalleryItem
import mega.privacy.android.app.utils.*
import java.util.*

/**
 * Bottom Sheet Dialog which shows the chat options
 */
class ChatRoomToolbarBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetChatRoomToolbarBinding

    private var downloadLocationDefaultPath: String? = null
    private var mPhotoUris: ArrayList<FileGalleryItem>? = null
    private lateinit var adapter: FileStorageAdapter

    lateinit var mainLinearLayout :LinearLayout
    private lateinit var listener: ChatRoomToolbarBottomSheetDialogActionListener


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetChatRoomToolbarBinding.inflate(layoutInflater, container, false)
        downloadLocationDefaultPath = FileUtil.getDownloadLocation()
        mainLinearLayout = binding.linearLayout

        mPhotoUris = ArrayList<FileGalleryItem>()

        checkPermissionsDialog()

        return binding.root
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog ?: return
        BottomSheetBehavior.from(dialog.findViewById(R.id.design_bottom_sheet)).state =
            BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupButtons()
        //setupListView()
        //setupListAdapter()

        super.onViewCreated(view, savedInstanceState)
    }

    /**
     * Setup option buttons according to the current MegaUser.
     *
     * @param megaUser  MegaUser to be shown
     */
    private fun setupButtons() {
        val chatActivity = requireActivity() as ChatActivityLollipop

        binding.optionGallery.setOnClickListener{
            chatActivity.sendFromFileSystem()
            dismiss()

        }

        binding.optionFile.setOnClickListener{
            chatActivity.sendFromCloud()
            dismiss()

        }

        binding.optionVoice.setOnClickListener{
            chatActivity.optionCall(false)
            dismiss()

        }

        binding.optionVideo.setOnClickListener{
            chatActivity.optionCall(true)
            dismiss()

        }

        binding.optionScan.setOnClickListener{
            chatActivity.scanDocument()
            dismiss()

        }

        binding.optionGif.setOnClickListener{
            chatActivity.sendGif()
            dismiss()

        }

        binding.optionLocation.setOnClickListener{
            chatActivity.sendLocation()
            dismiss()

        }

        binding.optionContact.setOnClickListener{
            chatActivity.chooseContactsDialog()
            dismiss()

        }
    }

    /**
     * Show node permission dialog to ask for Node permissions.
     */
    private fun checkPermissionsDialog() {
        val chatActivity = requireActivity() as ChatActivityLollipop

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasStoragePermission = ContextCompat.checkSelfPermission(
                chatActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasStoragePermission) {
                ActivityCompat.requestPermissions(
                    chatActivity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    Constants.REQUEST_READ_STORAGE
                )
            } else {
               // uploadGallery()
            }
        }

    }

    private fun setupListView() {
        val mLayoutManager: RecyclerView.LayoutManager =
            GridLayoutManager(context, 1, GridLayoutManager.HORIZONTAL, false)

        binding.recyclerViewGallery.clipToPadding = false
        binding.recyclerViewGallery.setHasFixedSize(true)
        binding.recyclerViewGallery.setItemAnimator(Util.noChangeRecyclerViewItemAnimator())
        binding.recyclerViewGallery.setLayoutManager(mLayoutManager)
    }


    private fun setupListAdapter() {
        context?.let {
            adapter = FileStorageAdapter(it, mPhotoUris)

        }
        adapter.setHasStableIds(true)
        binding.recyclerViewGallery.adapter = adapter
    }

    fun uploadGallery() {
        setNodes(mPhotoUris!!)
        //FetchDeviceGalleryTask(context).execute()
        checkAdapterItems(false)
    }

    fun setNodes(photosUrisReceived: ArrayList<FileGalleryItem>) {
        this.mPhotoUris = photosUrisReceived
        adapter.setNodes(mPhotoUris)
        checkAdapterItems(true)
    }

    private fun checkAdapterItems(fileLoaded: Boolean) {
        if (adapter.itemCount == 0) {
            binding.recyclerViewGallery.visibility = View.GONE
            if (fileLoaded) {
                binding.emptyGallery.setText(R.string.file_storage_empty_folder)
            }
            binding.emptyGallery.visibility = View.VISIBLE
            return
        }

        binding.recyclerViewGallery.visibility = View.VISIBLE
        binding.emptyGallery.visibility = View.GONE
    }

    fun updateFiles(files: List<FileGalleryItem>?){
        if (files!!.isNotEmpty()) {
            mPhotoUris!!.clear()
            mPhotoUris!!.addAll(files)
            adapter.notifyDataSetChanged()
        }

        checkAdapterItems(true)
    }
}