package mega.privacy.android.app.presentation.imagepreview

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import mega.privacy.android.app.presentation.imagepreview.fetcher.ImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.menu.ImagePreviewMenuOptions
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewState
import mega.privacy.android.app.utils.MegaNodeUtil.getInfoText
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.imageviewer.GetImageUseCase
import mega.privacy.android.domain.usecase.node.AddImageTypeUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ImagePreviewViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val imageNodeFetchers: Map<@JvmSuppressWildcards ImagePreviewFetcherSource, @JvmSuppressWildcards ImageNodeFetcher>,
    private val imagePreviewMenuOptionsMap: Map<@JvmSuppressWildcards ImagePreviewMenuSource, @JvmSuppressWildcards ImagePreviewMenuOptions>,
    private val addImageTypeUseCase: AddImageTypeUseCase,
    private val getImageUseCase: GetImageUseCase,
    private val areTransfersPausedUseCase: AreTransfersPausedUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val imagePreviewFetcherSource: ImagePreviewFetcherSource
        get() = savedStateHandle[IMAGE_NODE_FETCHER_SOURCE] ?: ImagePreviewFetcherSource.TIMELINE

    private val params: Bundle
        get() = savedStateHandle[FETCHER_PARAMS] ?: Bundle()

    private val currentImageNodeIdValue: Long
        get() = savedStateHandle[PARAMS_CURRENT_IMAGE_NODE_ID_VALUE] ?: 0L

    private val imagePreviewMenuSource: ImagePreviewMenuSource
        get() = savedStateHandle[IMAGE_PREVIEW_MENU_OPTIONS] ?: ImagePreviewMenuSource.TIMELINE

    private val _state = MutableStateFlow(
        ImagePreviewState(
            currentImageNodeId = NodeId(currentImageNodeIdValue),
        )
    )

    val state: StateFlow<ImagePreviewState> = _state

    init {
        monitorImageNodes()
    }

    private fun monitorImageNodes() {
        val imageFetcher = imageNodeFetchers[imagePreviewFetcherSource] ?: return
        imageFetcher.monitorImageNodes(params)
            .catch { Timber.e(it) }
            .mapLatest { imageNodes ->
                _state.update {
                    it.copy(
                        showSlideshowOption = shouldShowSlideshowOption(imageNodes),
                        imageNodes = imageNodes,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun shouldShowSlideshowOption(imageNodes: List<ImageNode>): Boolean =
        withContext(defaultDispatcher) {
            imageNodes.count { it.type !is VideoFileTypeInfo } > 1
        }

    fun isSlideshowOptionVisible(imageNode: ImageNode): Boolean =
        imagePreviewMenuOptionsMap[imagePreviewMenuSource]
            ?.isSlideshowOptionVisible(imageNode)
            ?: false


    fun isGetLinkOptionVisible(imageNode: ImageNode): Boolean =
        imagePreviewMenuOptionsMap[imagePreviewMenuSource]
            ?.isGetLinkOptionVisible(imageNode)
            ?: false


    fun isSaveToDeviceOptionVisible(imageNode: ImageNode): Boolean =
        imagePreviewMenuOptionsMap[imagePreviewMenuSource]
            ?.isSaveToDeviceOptionVisible(imageNode)
            ?: false


    fun isForwardOptionVisible(imageNode: ImageNode): Boolean =
        imagePreviewMenuOptionsMap[imagePreviewMenuSource]
            ?.isForwardOptionVisible(imageNode)
            ?: false

    fun isSendToOptionVisible(imageNode: ImageNode): Boolean =
        imagePreviewMenuOptionsMap[imagePreviewMenuSource]
            ?.isSendToOptionVisible(imageNode)
            ?: false

    suspend fun monitorImageResult(imageNode: ImageNode): Flow<ImageResult> {
        val typedNode = addImageTypeUseCase(imageNode)
        return getImageUseCase(
            node = typedNode,
            fullSize = true,
            highPriority = true,
            resetDownloads = {},
        ).catch { Timber.e("Failed to load image: $it") }
    }

    fun switchFullScreenMode() {
        val inFullScreenMode = _state.value.inFullScreenMode
        _state.update {
            it.copy(
                inFullScreenMode = !inFullScreenMode,
            )
        }
    }

    fun setCurrentImageNodeId(nodeId: NodeId) {
        _state.update {
            it.copy(
                currentImageNodeId = nodeId,
            )
        }
    }

    fun setTransferMessage(message: String) {
        _state.update {
            it.copy(
                transferMessage = message,
            )
        }
    }

    fun getInfoText(
        imageNode: ImageNode,
        context: Context,
    ): String {
        val megaNode = MegaNode.unserialize(imageNode.serializedData)
        return megaNode.getInfoText(context)
    }

    /**
     * Check if transfers are paused.
     */
    suspend fun executeTransfer(transferMessage: String, transferAction: () -> Unit) {
        if (areTransfersPausedUseCase()) {
            setTransferMessage(transferMessage)
        } else {
            transferAction()
        }
    }

    companion object {
        const val IMAGE_NODE_FETCHER_SOURCE = "image_node_fetcher_source"
        const val IMAGE_PREVIEW_MENU_OPTIONS = "image_preview_menu_options"
        const val FETCHER_PARAMS = "fetcher_params"
        const val PARAMS_CURRENT_IMAGE_NODE_ID_VALUE = "currentImageNodeIdValue"
    }
}