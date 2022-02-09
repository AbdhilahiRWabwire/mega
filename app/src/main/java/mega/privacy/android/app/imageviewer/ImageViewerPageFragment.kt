package mega.privacy.android.app.imageviewer

import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.drawable.ScalingUtils.ScaleType
import com.facebook.imagepipeline.common.Priority
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.memory.BasePool
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.PageImageViewerBinding
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.ContextUtils.getScreenSize
import mega.privacy.android.app.utils.ExtraUtils.extra
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.view.MultiTapGestureListener

/**
 * Image Viewer page that shows an individual image within a list of image items
 */
@AndroidEntryPoint
class ImageViewerPageFragment : Fragment() {

    companion object {
        /**
         * Main method to create a ImageViewerPageFragment.
         *
         * @param nodeHandle    Image node to show information from
         * @return              ImageBottomSheetDialogFragment to be shown
         */
        fun newInstance(nodeHandle: Long): ImageViewerPageFragment =
            ImageViewerPageFragment().apply {
                arguments = Bundle().apply {
                    putLong(INTENT_EXTRA_KEY_HANDLE, nodeHandle)
                }
            }
    }

    private lateinit var binding: PageImageViewerBinding

    private var hasScreenBeenRotated = false
    private var hasZoomBeenTriggered = false
    private val viewModel by activityViewModels<ImageViewerViewModel>()
    private val nodeHandle: Long? by extra(INTENT_EXTRA_KEY_HANDLE)
    private val controllerListener by lazy { buildImageControllerListener() }
    private val screenSize: Size by lazy { requireContext().getScreenSize() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireNotNull(nodeHandle)
        hasScreenBeenRotated = savedInstanceState != null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PageImageViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        if (!hasScreenBeenRotated) {
            viewModel.loadSingleImage(nodeHandle!!, fullSize = false, highPriority = true)
        }
    }

    override fun onPause() {
        if (activity?.isChangingConfigurations != true) {
            viewModel.stopImageLoading(nodeHandle!!, aggressive = false)
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (activity?.isFinishing == true) {
            viewModel.stopImageLoading(nodeHandle!!, aggressive = true)
        }
        super.onDestroy()
    }

    private fun setupView() {
        binding.image.apply {
            setZoomingEnabled(true)
            setIsLongpressEnabled(true)
            setAllowTouchInterceptionWhileZoomed(false)
            setTapListener(
                MultiTapGestureListener(
                    this,
                    onSingleTapCallback = viewModel::switchToolbar,
                    onZoomCallback = {
                        if (!hasZoomBeenTriggered) {
                            hasZoomBeenTriggered = true
                            viewModel.loadSingleImage(nodeHandle!!, fullSize = true, highPriority = true)
                        }
                    }
                )
            )
        }
    }

    private fun setupObservers() {
        viewModel.onImage(nodeHandle!!).observe(viewLifecycleOwner, ::showItem)
        if (!hasScreenBeenRotated) {
            viewModel.loadSingleNode(nodeHandle!!)
            viewModel.loadSingleImage(nodeHandle!!, fullSize = false, highPriority = false)
        }
    }

    private fun showItem(imageItem: ImageItem?) {
        val imageResult = imageItem?.imageResult ?: return

        var mainImageUri: Uri? = null
        var lowImageUri: Uri? = null

        when {
            imageResult.isVideo -> {
                mainImageUri = imageResult.previewUri
                lowImageUri = imageResult.thumbnailUri
            }
            imageResult.fullSizeUri != null -> {
                if (lifecycle.currentState == Lifecycle.State.RESUMED
                    || (imageResult.previewUri == null && imageResult.thumbnailUri == null)
                ) {
                    mainImageUri = imageResult.fullSizeUri
                    lowImageUri = imageResult.previewUri ?: imageResult.thumbnailUri
                } else {
                    mainImageUri = imageResult.previewUri
                    lowImageUri = imageResult.thumbnailUri
                }
            }
            imageResult.previewUri != null -> {
                mainImageUri = imageResult.previewUri
                lowImageUri = imageResult.thumbnailUri
            }
            imageResult.thumbnailUri != null -> {
                mainImageUri = imageResult.thumbnailUri
            }
        }

        if (mainImageUri == null && lowImageUri == null) return
        val mainImageRequest = mainImageUri?.toImageRequest()
        val lowImageRequest = lowImageUri?.toImageRequest()

        val newController = Fresco.newDraweeControllerBuilder()
            .setLowResImageRequest(lowImageRequest)
            .setImageRequest(mainImageRequest)
            .build()

        if (binding.image.controller?.isSameImageRequest(newController) != true) {
            binding.image.controller = Fresco.newDraweeControllerBuilder()
                .setOldController(binding.image.controller)
                .setControllerListener(controllerListener)
                .setAutoPlayAnimations(true)
                .setLowResImageRequest(lowImageRequest)
                .setImageRequest(mainImageRequest)
                .build()
        } else if (imageItem.imageResult.isFullyLoaded) {
            binding.image.post {
                if (imageItem.imageResult.isVideo) {
                    showVideoButton()
                }
                binding.progress.hide()
            }
        }
    }

    private fun buildImageControllerListener() = object : BaseControllerListener<ImageInfo>() {
        override fun onFinalImageSet(
            id: String?,
            imageInfo: ImageInfo?,
            animatable: Animatable?
        ) {
            val imageItem = viewModel.getImageItem(nodeHandle!!)
            if (imageItem?.imageResult?.isFullyLoaded == true) {
                binding.image.post {
                    if (imageItem.imageResult.isVideo) {
                        showVideoButton()
                    }
                    binding.progress.hide()
                }
            }
        }

        override fun onFailure(id: String, throwable: Throwable) {
            logError(throwable.stackTraceToString())
            if (throwable is BasePool.PoolSizeViolationException) activity?.onLowMemory()

            val imageItem = viewModel.getImageItem(nodeHandle!!)
            binding.image.hierarchy.setFailureImage(R.drawable.ic_error, ScaleType.FIT_CENTER)
            binding.image.controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(imageItem?.imageResult?.previewUri?.toImageRequest())
                .build()

            if (imageItem?.imageResult?.isFullyLoaded == true) {
                binding.image.post {
                    if (imageItem.imageResult.isVideo) {
                        showVideoButton()
                    }
                    binding.progress.hide()
                }
            }
        }
    }

    private fun showVideoButton() {
        if (binding.btnVideo.isVisible && viewModel.isToolbarShown()) return

        viewModel.switchToolbar(true)
        binding.btnVideo.setOnClickListener { launchVideoScreen() }
        binding.btnVideo.isVisible = true
        binding.image.apply {
            setAllowTouchInterceptionWhileZoomed(true)
            setZoomingEnabled(false)
            setTapListener(object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent?): Boolean {
                    launchVideoScreen()
                    return true
                }
            })
            setOnClickListener { launchVideoScreen() }
        }
    }

    private fun launchVideoScreen() {
        val imageItem = viewModel.getImageItem(nodeHandle!!) ?: return
        (activity as? ImageViewerActivity?)?.launchVideoScreen(imageItem)
    }

    private fun Uri.toImageRequest(): ImageRequest? =
        ImageRequestBuilder.newBuilderWithSource(this)
            .setRotationOptions(RotationOptions.autoRotate())
            .setResizeOptions(ResizeOptions.forDimensions(screenSize.width, screenSize.height))
            .setRequestPriority(
                if (lifecycle.currentState == Lifecycle.State.RESUMED)
                    Priority.HIGH
                else
                    Priority.LOW
            )
            .build()
}
