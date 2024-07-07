package mega.privacy.android.app.modalbottomsheet

import mega.privacy.android.icon.pack.R as IconPackR
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.OfflineFileInfoActivity
import mega.privacy.android.app.databinding.BottomSheetOfflineItemBinding
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.parcelable
import mega.privacy.android.app.presentation.offline.adapter.OfflineNodeListener
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.Util
import timber.log.Timber

@Deprecated("Use OfflineOptionsBottomSheetDialogFragment")
internal class LegacyOfflineOptionsBottomSheetDialogFragment : BaseBottomSheetDialogFragment(),
    View.OnClickListener {
    private val nodeOffline: MegaOffline by lazy(LazyThreadSafetyMode.NONE) {
        requireNotNull(requireArguments().parcelable(EXTRA_OFFLINE))
    }
    private var _binding: BottomSheetOfflineItemBinding? = null
    val binding: BottomSheetOfflineItemBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        contentView = BottomSheetOfflineItemBinding.inflate(inflater, container, false).also {
            _binding = it
            itemsLayout = it.itemsLayout
        }.root
        return contentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.optionDownload.setOnClickListener(this)
        binding.optionProperties.setOnClickListener(this)
        binding.optionShare.setOnClickListener(this)
        binding.optionDeleteOffline.setOnClickListener(this)
        binding.optionOpenWith.setOnClickListener(this)
        binding.offlineNameText.maxWidth = Util.scaleWidthPx(200, resources.displayMetrics)
        binding.offlineInfoText.maxWidth = Util.scaleWidthPx(200, resources.displayMetrics)
        binding.optionProperties.setText(R.string.general_info)
        binding.optionOpenWith.isGone = nodeOffline.isFolder
        binding.separatorOpen.isGone = nodeOffline.isFolder
        binding.offlineNameText.text = nodeOffline.name
        Timber.d("Set node info")
        val file = OfflineUtils.getOfflineFile(requireContext(), nodeOffline)
        if (!FileUtil.isFileAvailable(file)) return
        if (file.isDirectory) {
            binding.offlineInfoText.text =
                FileUtil.getFileFolderInfo(file, requireContext())
        } else {
            binding.offlineInfoText.text = FileUtil.getFileInfo(file, requireContext())
        }
        if (file.isFile) {
            if (typeForName(nodeOffline.name).isImage) {
                if (file.exists()) {
                    val params =
                        binding.offlineThumbnail.layoutParams as RelativeLayout.LayoutParams
                    params.width = Util.dp2px(Constants.THUMB_SIZE_DP.toFloat())
                    params.height = params.width
                    val margin = Util.dp2px(Constants.THUMB_MARGIN_DP.toFloat())
                    params.setMargins(margin, margin, margin, margin)
                    binding.offlineThumbnail.layoutParams = params
                    binding.offlineThumbnail.setImageURI(Uri.fromFile(file))
                } else {
                    binding.offlineThumbnail.hierarchy.setPlaceholderImage(
                        typeForName(
                            nodeOffline.name
                        ).iconResourceId
                    )
                }
            } else {
                binding.offlineThumbnail.setImageResource(
                    typeForName(
                        nodeOffline.name
                    ).iconResourceId
                )
            }
        } else {
            binding.offlineThumbnail.setImageResource(IconPackR.drawable.ic_folder_medium_solid)
        }
        if (nodeOffline.isFolder && !Util.isOnline(requireContext())) {
            binding.optionShare.visibility = View.GONE
            binding.separatorShare.visibility = View.GONE
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.option_delete_offline -> {
                if (parentFragment is OfflineNodeListener) {
                    (parentFragment as OfflineNodeListener).showConfirmationRemoveOfflineNode(
                        nodeOffline
                    )
                }
            }

            R.id.option_open_with -> {
                OfflineUtils.openWithOffline(
                    requireContext(),
                    nodeOffline.handle.toLong()
                )
            }

            R.id.option_share -> {
                OfflineUtils.shareOfflineNode(
                    requireContext(),
                    nodeOffline.handle.toLong()
                )
            }

            R.id.option_download -> {
                (requireActivity() as ManagerActivity).saveOfflineNodesToDevice(listOf(nodeOffline))
            }

            R.id.option_properties -> {
                val offlineIntent = Intent(requireContext(), OfflineFileInfoActivity::class.java)
                offlineIntent.putExtra(Constants.HANDLE, nodeOffline.handle)
                startActivity(offlineIntent)
            }
        }
        setStateBottomSheetBehaviorHidden()
    }

    companion object {
        private const val EXTRA_OFFLINE = "offline"

        fun newInstance(offline: MegaOffline) = LegacyOfflineOptionsBottomSheetDialogFragment().apply {
            arguments = bundleOf(EXTRA_OFFLINE to offline)
        }
    }
}