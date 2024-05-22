package mega.privacy.android.app.imageviewer

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.facebook.drawee.backends.pipeline.Fresco
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.imageviewer.data.ImageAdapterItem
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.imageviewer.slideshow.ImageSlideshowState
import mega.privacy.android.app.imageviewer.slideshow.ImageSlideshowState.NEXT
import mega.privacy.android.app.imageviewer.slideshow.ImageSlideshowState.STARTED
import mega.privacy.android.app.imageviewer.slideshow.ImageSlideshowState.STOPPED
import mega.privacy.android.app.imageviewer.usecase.GetImageHandlesUseCase
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.usecase.GetGlobalChangesUseCase
import mega.privacy.android.app.usecase.GetGlobalChangesUseCase.Result
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.usecase.chat.DeleteChatMessageUseCase
import mega.privacy.android.app.usecase.data.MegaNodeItem
import mega.privacy.android.app.usecase.exception.MegaException
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.usecase.exception.ResourceAlreadyExistsMegaException
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.MegaNodeUtil.getInfoText
import mega.privacy.android.app.utils.MegaNodeUtil.isValidForImageViewer
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.app.utils.notifyObserver
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.IsUserLoggedIn
import mega.privacy.android.domain.usecase.filenode.DeleteNodeByHandleUseCase
import mega.privacy.android.domain.usecase.filenode.MoveNodeToRubbishBinUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageByAlbumImportNodeUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageByNodeHandleUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageByNodePublicLinkUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageByOfflineNodeHandleUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageForChatMessageUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageFromFileUseCase
import mega.privacy.android.domain.usecase.node.CopyChatNodeUseCase
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.node.DisableExportUseCase
import mega.privacy.android.domain.usecase.node.ExportNodeUseCase
import mega.privacy.android.domain.usecase.node.MoveNodeUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetNumPendingDownloadsNonBackgroundUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.ResetTotalDownloadsUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Main ViewModel to handle all logic related to the ImageViewer.
 * This is shared between ImageViewerActivity behaving as the main container and
 * each individual ImageViewerPageFragment representing a single image within the ViewPager.
 *
 * @property getImageByNodeHandleUseCase            Needed to retrieve each individual image based on a node handle
 * @property getImageByNodePublicLinkUseCase        Needed to retrieve each individual image based on a node public link
 * @property getImageForChatMessageUseCase          Needed to retrieve each individual image based on chat details
 * @property getImageByOfflineNodeHandleUseCase     Needed to retrieve each individual image based on offline node handle
 * @property getImageFromFileUseCase                Needed to retrieve each individual image based on file Uri
 * @property getNumPendingDownloadsNonBackgroundUseCase    UseCase to get number of pending downloads that are not background transfers
 * @property resetTotalDownloadsUseCase                    UseCase to reset total downloads
 * @property getImageHandlesUseCase     Needed to retrieve node handles given sent params
 * @property getGlobalChangesUseCase    Use case required to get node changes
 * @property getNodeUseCase             Needed to retrieve each individual node based on a node handle,
 *                                      as well as each individual node action required by the menu
 * @property copyNodeUseCase            Needed to copy image node on demand
 * @property moveNodeUseCase            Needed to move image node on demand
 * @property removeNodeUseCase          Needed to remove image node on demand
 * @property cancelTransferUseCase      Needed to cancel current full image transfer if needed
 * @property isUserLoggedInUseCase      UseCase required to check when the user is already logged in
 * @property deleteChatMessageUseCase   UseCase required to delete current chat node message
 * @property areTransfersPausedUseCase         UseCase required to check if transfers are paused
 * @property copyNodeUseCase            UseCase required to copy nodes
 * @property moveNodeUseCase            UseCase required to move nodes
 * @property removeNodeUseCase          UseCase required to remove nodes
 * @property checkNameCollision         UseCase required to check name collisions
 * @property moveNodeToRubbishBinUseCase  UseCase to move node to rubbish bin
 */
@HiltViewModel
class ImageViewerViewModel @Inject constructor(
    private val getImageByNodeHandleUseCase: GetImageByNodeHandleUseCase,
    private val getImageByNodePublicLinkUseCase: GetImageByNodePublicLinkUseCase,
    private val getImageForChatMessageUseCase: GetImageForChatMessageUseCase,
    private val getImageByOfflineNodeHandleUseCase: GetImageByOfflineNodeHandleUseCase,
    private val getImageFromFileUseCase: GetImageFromFileUseCase,
    private val getNumPendingDownloadsNonBackgroundUseCase: GetNumPendingDownloadsNonBackgroundUseCase,
    private val resetTotalDownloadsUseCase: ResetTotalDownloadsUseCase,
    private val getImageHandlesUseCase: GetImageHandlesUseCase,
    private val getGlobalChangesUseCase: GetGlobalChangesUseCase,
    private val getNodeUseCase: GetNodeUseCase,
    private val exportNodeUseCase: ExportNodeUseCase,
    private val disableExportUseCase: DisableExportUseCase,
    private val cancelTransferByTagUseCase: CancelTransferByTagUseCase,
    private val isUserLoggedInUseCase: IsUserLoggedIn,
    private val deleteChatMessageUseCase: DeleteChatMessageUseCase,
    private val areTransfersPausedUseCase: AreTransfersPausedUseCase,
    private val copyNodeUseCase: CopyNodeUseCase,
    private val moveNodeUseCase: MoveNodeUseCase,
    private val deleteNodeByHandleUseCase: DeleteNodeByHandleUseCase,
    private val checkNameCollision: CheckNameCollision,
    private val copyChatNodeUseCase: CopyChatNodeUseCase,
    private val checkNameCollisionUseCase: CheckNameCollisionUseCase,
    private val moveNodeToRubbishBinUseCase: MoveNodeToRubbishBinUseCase,
    private val getImageByAlbumImportNodeUseCase: GetImageByAlbumImportNodeUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val composite = CompositeDisposable()

    companion object {
        private const val SLIDESHOW_DELAY = 4L
    }

    val images = MutableLiveData<List<ImageItem>?>()
    private val currentImageId = MutableLiveData<Long?>()
    private val showToolbar = MutableLiveData(ToolbarState())
    private val snackBarMessage = SingleLiveEvent<String>()
    private val actionBarMessage = SingleLiveEvent<Int>()
    private val copyMoveException = SingleLiveEvent<Throwable>()
    private val collision = SingleLiveEvent<NameCollision>()
    private val slideShowState = MutableLiveData(STOPPED)
    private val timerComposite = CompositeDisposable()

    private var isUserLoggedIn = false

    init {
        checkIfUserIsLoggedIn()
        subscribeToNodeChanges()
    }

    override fun onCleared() {
        composite.clear()
        timerComposite.dispose()
        Fresco.getImagePipeline()?.clearMemoryCaches()
        super.onCleared()
    }

    /**
     * Get an updated LiveData of adapter images filtered
     *
     * @param filterVideos  Flag to filter videos and only get images
     * @return              LiveData
     */
    fun onAdapterImages(filterVideos: Boolean): LiveData<List<ImageAdapterItem>?> =
        images.map { items ->
            if (filterVideos) {
                items?.filter { it.imageResult?.isVideo != true }
                    ?.map { ImageAdapterItem(it.id, it.hashCode()) }
            } else {
                items?.map { ImageAdapterItem(it.id, it.hashCode()) }
            }
        }

    /**
     * Get an updated LiveData of a specific Image
     *
     * @param itemId    Item id to find the specific Image
     * @return          LiveData
     */
    fun onImage(itemId: Long?): LiveData<ImageItem?> =
        images.map { items -> items?.firstOrNull { it.id == itemId } }

    /**
     * Get the amount of images
     *
     * @param filterVideos  Flag to filter videos and only count images
     * @return              Number of image items
     */
    fun getImagesSize(filterVideos: Boolean): Int =
        if (filterVideos) {
            images.value?.count { it.imageResult?.isVideo != true } ?: 0
        } else {
            images.value?.size ?: 0
        }

    /**
     * Get current position
     *
     * @param filterVideos  Flag to filter videos and get images position only
     * @return              Current position
     */
    fun getCurrentPosition(filterVideos: Boolean): Int =
        if (filterVideos) {
            images.value?.filter { it.imageResult?.isVideo != true }
                ?.indexOfFirst { it.id == currentImageId.value } ?: 0
        } else {
            images.value?.indexOfFirst { it.id == currentImageId.value } ?: 0
        }

    fun getCurrentImageItem(): ImageItem? =
        currentImageId.value?.let { imageId -> images.value?.find { it.id == imageId } }

    fun onCurrentImageItem(): LiveData<ImageItem?> =
        currentImageId.switchMap(::onImage)

    fun getImageItem(itemId: Long): ImageItem? =
        images.value?.find { it.id == itemId }

    fun onSnackBarMessage(): SingleLiveEvent<String> = snackBarMessage

    fun onActionBarMessage(): SingleLiveEvent<Int> = actionBarMessage

    fun onCopyMoveException(): LiveData<Throwable> = copyMoveException

    fun onCollision(): LiveData<NameCollision> = collision

    fun onSlideshowState(): LiveData<ImageSlideshowState> = slideShowState

    fun onShowToolbar(): LiveData<ToolbarState> = showToolbar

    fun isToolbarShown(): Boolean = showToolbar.value?.show ?: false

    fun showToolbar(show: Boolean, enableTransparency: Boolean = false) {
        showToolbar.value = ToolbarState(
            show = show,
            enableTransparency = enableTransparency
        )
    }

    fun retrieveSingleImage(nodeHandle: Long, isOffline: Boolean = false) {
        getImageHandlesUseCase.get(nodeHandles = longArrayOf(nodeHandle), isOffline = isOffline)
            .subscribeAndUpdateImages(nodeHandle)
    }

    fun retrieveSingleImage(nodeFileLink: String) {
        getImageHandlesUseCase.get(nodeFileLinks = listOf(nodeFileLink))
            .subscribeAndUpdateImages()
    }

    fun retrieveFileImage(imageUri: Uri, showNearbyFiles: Boolean? = false, itemId: Long? = null) {
        getImageHandlesUseCase.get(imageFileUri = imageUri, showNearbyFiles = showNearbyFiles)
            .subscribeAndUpdateImages(itemId)
    }

    fun retrieveImagesFromParent(
        parentNodeHandle: Long,
        childOrder: SortOrder? = null,
        currentNodeHandle: Long? = null,
    ) {
        getImageHandlesUseCase.get(parentNodeHandle = parentNodeHandle, sortOrder = childOrder)
            .subscribeAndUpdateImages(currentNodeHandle)
    }

    fun retrieveImagesFromTimeline(
        currentNodeHandle: Long? = null,
    ) {
        getImageHandlesUseCase.get(isTimeline = true)
            .subscribeAndUpdateImages(currentNodeHandle)
    }

    fun retrieveImagesFromAlbumSharing(
        currentNodeHandle: Long? = null,
    ) {
        getImageHandlesUseCase.get(isAlbumSharing = true)
            .subscribeAndUpdateImages(currentNodeHandle)
    }

    fun retrieveImages(
        nodeHandles: LongArray,
        currentNodeHandle: Long? = null,
        isOffline: Boolean = false,
    ) {
        getImageHandlesUseCase.get(nodeHandles = nodeHandles, isOffline = isOffline)
            .subscribeAndUpdateImages(currentNodeHandle)
    }

    fun retrieveChatImages(
        chatRoomId: Long,
        messageIds: LongArray,
        currentNodeHandle: Long? = null,
    ) {
        getImageHandlesUseCase.get(chatRoomId = chatRoomId, chatMessageIds = messageIds)
            .subscribeAndUpdateImages(currentNodeHandle)
    }

    fun isUserLoggedIn(): Boolean = isUserLoggedIn

    /**
     * Main method to request a MegaNodeItem given a previously loaded Node handle.
     * This will update the current Node on the main "images" list if it's newer.
     * You must be observing the requested Image to get the updated result.
     *
     * @param itemId    Item to be loaded.
     */
    fun loadSingleNode(itemId: Long) {
        val imageItem = images.value?.find { it.id == itemId } ?: run {
            Timber.w("Null item id: $itemId")
            return
        }

        val subscription = when (imageItem) {
            is ImageItem.PublicNode ->
                getNodeUseCase.getNodeItem(imageItem.nodePublicLink)

            is ImageItem.ChatNode ->
                getNodeUseCase.getNodeItem(imageItem.chatRoomId, imageItem.chatMessageId)

            is ImageItem.OfflineNode ->
                getNodeUseCase.getOfflineNodeItem(imageItem.handle)

            is ImageItem.Node ->
                getNodeUseCase.getNodeItem(imageItem.handle)

            is ImageItem.AlbumImportNode ->
                getNodeUseCase.getAlbumSharingNodeItem(imageItem.handle)

            is ImageItem.File,
            -> {
                // do nothing
                return
            }
        }

        subscription
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .retry(1)
            .subscribeBy(
                onSuccess = { nodeItem ->
                    updateItemIfNeeded(itemId, nodeItem = nodeItem)
                },
                onError = { error ->
                    Timber.e(error)
                    if (itemId == getCurrentImageItem()?.id && error is MegaException) {
                        snackBarMessage.value = error.getTranslatedErrorString()
                    }
                }
            )
            .addTo(composite)
    }

    /**
     * Main method to request an ImageResult given a previously loaded Node handle.
     * This will update the current Image on the main "images" list if it's newer.
     * You must be observing the requested Image to get the updated result.
     *
     * @param itemId        Item to be loaded.
     * @param fullSize      Flag to request full size image despite data/size requirements.
     */
    fun loadSingleImage(itemId: Long, fullSize: Boolean) {
        val imageItem = images.value?.find { it.id == itemId } ?: run {
            Timber.w("Null item id: $itemId")
            return
        }

        if (imageItem.imageResult?.isFullyLoaded == true
            && imageItem.imageResult?.fullSizeUri != null
            && imageItem.imageResult?.previewUri != null
        ) return // Already downloaded

        val highPriority = itemId == getCurrentImageItem()?.id
        viewModelScope.launch {
            when (imageItem) {
                is ImageItem.PublicNode -> getImageByNodePublicLinkUseCase(
                    imageItem.nodePublicLink,
                    fullSize,
                    highPriority
                ) { resetTotalDownloadsIfNeeded() }.catch {
                    onLoadSingleImageFailure(itemId, it)
                }.collectLatest {
                    onLoadSingleImageSuccess(itemId, it)
                }

                is ImageItem.ChatNode -> getImageForChatMessageUseCase(
                    imageItem.chatRoomId,
                    imageItem.chatMessageId, fullSize, highPriority
                ) { resetTotalDownloadsIfNeeded() }.catch {
                    onLoadSingleImageFailure(itemId, it)
                }.collectLatest {
                    onLoadSingleImageSuccess(itemId, it)
                }

                is ImageItem.OfflineNode -> runCatching {
                    getImageByOfflineNodeHandleUseCase(imageItem.handle)
                }.onSuccess {
                    onLoadSingleImageSuccess(itemId, it)
                }.onFailure {
                    onLoadSingleImageFailure(itemId, it)
                }

                is ImageItem.Node ->
                    getImageByNodeHandleUseCase(
                        imageItem.handle,
                        fullSize, highPriority
                    ) { resetTotalDownloadsIfNeeded() }.catch {
                        onLoadSingleImageFailure(itemId, it)
                    }.collectLatest {
                        onLoadSingleImageSuccess(itemId, it)
                    }


                is ImageItem.AlbumImportNode ->
                    getImageByAlbumImportNodeUseCase(
                        imageItem.handle,
                        fullSize, highPriority
                    ) { resetTotalDownloadsIfNeeded() }.catch {
                        onLoadSingleImageFailure(itemId, it)
                    }.collectLatest {
                        onLoadSingleImageSuccess(itemId, it)
                    }

                is ImageItem.File,
                -> runCatching {
                    getImageFromFileUseCase(imageItem.fileUri.toFile())
                }.onSuccess {
                    onLoadSingleImageSuccess(itemId, it)
                }.onFailure {
                    onLoadSingleImageFailure(itemId, it)
                }
            }
        }
    }

    private fun resetTotalDownloadsIfNeeded() {
        viewModelScope.launch {
            if (getNumPendingDownloadsNonBackgroundUseCase() == 0) {
                resetTotalDownloadsUseCase()
            }
        }
    }

    private fun onLoadSingleImageSuccess(
        itemId: Long,
        imageResult: ImageResult,
    ) {
        updateItemIfNeeded(itemId, imageResult = imageResult)
    }

    private fun onLoadSingleImageFailure(itemId: Long, error: Throwable) {
        Timber.e(error)
        if (itemId == getCurrentImageItem()?.id
            && error is MegaException && error !is ResourceAlreadyExistsMegaException
        ) {
            snackBarMessage.value = error.getTranslatedErrorString()
        }
    }

    /**
     * Update a specific ImageItem from the Images list with the provided
     * MegaNodeItem or ImageResult
     *
     * @param itemId        Item to be updated
     * @param nodeItem      MegaNodeItem to be updated with
     * @param imageResult   ImageResult to be updated with
     */
    private fun updateItemIfNeeded(
        itemId: Long,
        nodeItem: MegaNodeItem? = null,
        imageResult: ImageResult? = null,
    ) {
        if (nodeItem == null && imageResult == null) return

        val items = images.value?.toMutableList()
        if (!items.isNullOrEmpty()) {
            val index = items.indexOfFirst { it.id == itemId }
            if (index != INVALID_POSITION) {
                val currentItem = items[index]
                if (nodeItem != null && nodeItem != currentItem.nodeItem) {
                    items[index] = currentItem.copy(
                        nodeItem = nodeItem,
                        context = context,
                    )
                }
                if (imageResult != null && imageResult != currentItem.imageResult) {
                    items[index] = currentItem.copy(
                        imageResult = imageResult,
                        context = context,
                    )
                }
                images.value = items.toList()
            } else {
                Timber.w("Node $itemId not found")
            }
        } else {
            Timber.w("Images are null or empty")
            images.value = null
        }
    }

    /**
     * Subscribe to latest node changes to update existing ones.
     */
    @Suppress("SENSELESS_COMPARISON")
    private fun subscribeToNodeChanges() {
        getGlobalChangesUseCase()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .filter { change -> change is Result.OnNodesUpdate }
            .subscribeBy(
                onNext = { change ->
                    val items = images.value?.toMutableList() ?: run {
                        Timber.w("Images are null or empty")
                        return@subscribeBy
                    }

                    val dirtyNodeHandles = mutableListOf<Long>()
                    (change as Result.OnNodesUpdate).nodes?.forEach { changedNode ->
                        val currentIndex =
                            items.indexOfFirst { changedNode.handle == it.getNodeHandle() }
                        when {
                            currentIndex == INVALID_POSITION -> {
                                return@subscribeBy // Not found
                            }

                            changedNode.hasChanged(MegaNode.CHANGE_TYPE_NEW.toLong()) -> {
                                val hasSameParent = (changedNode.parentHandle != null
                                        && changedNode.parentHandle == items.firstOrNull()?.nodeItem?.node?.parentHandle)
                                if (hasSameParent && changedNode.isValidForImageViewer()) {
                                    items.add(
                                        ImageItem.Node(
                                            id = changedNode.handle,
                                            handle = changedNode.handle,
                                            name = changedNode.name,
                                            infoText = changedNode.getInfoText(context)
                                        )
                                    )
                                    dirtyNodeHandles.add(changedNode.handle)
                                }
                            }

                            changedNode.hasChanged(MegaNode.CHANGE_TYPE_PARENT.toLong()) -> {
                                if (currentIndex != INVALID_POSITION) {
                                    val hasSameParent = (changedNode.parentHandle != null
                                            && changedNode.parentHandle == items.firstOrNull()?.nodeItem?.node?.parentHandle)
                                    if (!hasSameParent) {
                                        items.removeAt(currentIndex)
                                    }
                                }
                            }

                            changedNode.hasChanged(MegaNode.CHANGE_TYPE_REMOVED.toLong()) -> {
                                if (currentIndex != INVALID_POSITION) {
                                    items.removeAt(currentIndex)
                                }
                            }

                            else -> {
                                dirtyNodeHandles.add(changedNode.handle)
                            }
                        }
                    }

                    if (dirtyNodeHandles.isNotEmpty() || items.size != images.value?.size) {
                        images.value = items.toList()
                        dirtyNodeHandles.forEach(::loadSingleNode)
                        calculateNewPosition(items)
                    }
                },
                onError = Timber::e
            )
            .addTo(composite)
    }

    fun markNodeAsFavorite(nodeHandle: Long, isFavorite: Boolean) {
        getNodeUseCase.markAsFavorite(nodeHandle, isFavorite)
            .subscribeAndComplete()
    }

    fun switchNodeOfflineAvailabilityToFalse(
        nodeItem: MegaNodeItem,
        activity: Activity,
    ) {
        getNodeUseCase.removeNodeAvailableOffline(
            node = nodeItem.node,
            activity = activity
        ).subscribeAndComplete {
            loadSingleNode(nodeItem.handle)
        }
    }

    /**
     * Remove ImageItem from main list given an Index.
     *
     * @param index    Node Handle to be removed from the list
     */
    private fun removeImageItemAt(index: Int) {
        if (index != INVALID_POSITION) {
            val items = images.value!!.toMutableList().apply {
                removeAt(index)
            }
            images.value = items.toList()
            calculateNewPosition(items)
        }
    }

    /**
     * Calculate new ViewPager position based on a new list of items
     *
     * @param newItems  New ImageItems to calculate new position from
     */
    private fun calculateNewPosition(newItems: List<ImageItem>) {
        val existingItems = images.value?.toMutableList()
        val existingImageId = currentImageId.value
        if (existingItems.isNullOrEmpty() || existingImageId == null) {
            currentImageId.value = null
        } else if (existingItems.size == newItems.size) {
            return // Nothing to update
        } else {
            val currentItemPosition = images.value?.indexOfFirst { it.id == existingImageId }
            val newCurrentItemPosition = newItems.indexOfFirst { it.id == existingImageId }
            when {
                currentItemPosition == newCurrentItemPosition ->
                    return // Nothing to update
                newCurrentItemPosition != INVALID_POSITION ->
                    currentImageId.notifyObserver()

                newCurrentItemPosition >= existingItems.size ->
                    currentImageId.value = newItems.last().id

                currentItemPosition == 0 ->
                    currentImageId.value = newItems.first().id
            }
        }
    }

    fun showTransfersAction() {
        actionBarMessage.value = R.string.resume_paused_transfers_text
    }

    fun removeOfflineNode(nodeHandle: Long, activity: Activity) {
        getNodeUseCase.removeOfflineNode(nodeHandle, activity)
            .subscribeAndComplete {
                val index = images.value?.indexOfFirst { nodeHandle == it.getNodeHandle() }
                    ?: INVALID_POSITION
                removeImageItemAt(index)
            }
    }

    fun removeLink(nodeHandle: Long) {
        viewModelScope.launch {
            runCatching {
                disableExportUseCase(NodeId(nodeHandle))
            }.onSuccess {
                snackBarMessage.value =
                    context.resources.getQuantityString(R.plurals.context_link_removal_success, 1)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    fun removeChatMessage(nodeHandle: Long) {
        val imageItem =
            images.value?.firstOrNull { nodeHandle == it.getNodeHandle() } as? ImageItem.ChatNode
                ?: return
        deleteChatMessageUseCase.delete(imageItem.chatRoomId, imageItem.chatMessageId)
            .subscribeAndComplete {
                val index = images.value?.indexOfFirst { it.id == imageItem.id } ?: INVALID_POSITION
                removeImageItemAt(index)

                snackBarMessage.value = context.getString(R.string.context_correctly_removed)
            }
    }

    val result = MutableLiveData<String?>()
    fun exportNode(node: MegaNode): LiveData<String?> {
        viewModelScope.launch {
            runCatching { exportNodeUseCase(NodeId(node.handle)) }.onSuccess { link ->
                result.value = link
            }.onFailure { error ->
                Timber.e(error)
                result.value = null
            }
        }
        return result
    }

    /**
     * Imports a node if there is no name collision.
     *
     * @param newParentHandle   Parent handle in which the node will be copied.
     */
    fun importNode(newParentHandle: Long) = viewModelScope.launch {
        val importNodeItem = images.value?.find { it.id == currentImageId.value }
        val importNode = importNodeItem?.nodeItem?.node
            ?: return@launch
        if (importNodeItem is ImageItem.ChatNode) {
            runCatching {
                checkNameCollisionUseCase.check(
                    node = importNode,
                    parentHandle = newParentHandle,
                    type = NameCollisionType.COPY,
                )
            }.onSuccess { collisionResult ->
                collision.value = collisionResult
            }.onFailure { throwable ->
                when (throwable) {
                    is MegaNodeException.ChildDoesNotExistsException -> {
                        copyChatNode(importNodeItem, NodeId(newParentHandle))
                    }

                    else -> Timber.e(throwable)
                }
            }
        } else {
            copyNode(importNode.handle, newParentHandle)
        }
    }

    /**
     * Copies a chat node
     * @param nodeItem Node to copy
     * @param newParentNodeId Parent handle in which the node will be copied.
     */
    private fun copyChatNode(nodeItem: ImageItem.ChatNode, newParentNodeId: NodeId) {
        viewModelScope.launch {
            runCatching {
                copyChatNodeUseCase(
                    chatId = nodeItem.chatRoomId,
                    messageId = nodeItem.chatMessageId,
                    newNodeParent = newParentNodeId,
                )
            }.onSuccess {
                snackBarMessage.value =
                    context.getString(R.string.context_correctly_copied)
            }.onFailure {
                Timber.e(it, "The chat node is not copied")
                copyMoveException.value = it
            }
        }
    }

    /**
     * Copies a node if there is no name collision.
     *
     * @param nodeHandle        Node handle to copy.
     * @param newParentHandle   Parent handle in which the node will be copied.
     */
    fun copyNode(nodeHandle: Long, newParentHandle: Long) {
        viewModelScope.launch {
            checkForNameCollision(
                nodeHandle = nodeHandle,
                newParentHandle = newParentHandle,
                type = NameCollisionType.COPY
            ) {
                runCatching {
                    copyNodeUseCase(
                        nodeToCopy = NodeId(nodeHandle),
                        newNodeParent = NodeId(newParentHandle),
                        newNodeName = null,
                    )
                }.onSuccess {
                    snackBarMessage.value = context.getString(R.string.context_correctly_copied)
                }.onFailure {
                    copyMoveException.value = it
                    Timber.e(it, "The node is not copied")
                }
            }
        }
    }

    /**
     * Moves a node if there is no name collision.
     *
     * @param nodeHandle        Node handle to move.
     * @param newParentHandle   Parent handle in which the node will be moved.
     */
    fun moveNode(nodeHandle: Long, newParentHandle: Long) {
        viewModelScope.launch {
            checkForNameCollision(
                nodeHandle = nodeHandle,
                newParentHandle = newParentHandle,
                type = NameCollisionType.MOVE
            ) {
                viewModelScope.launch {
                    runCatching {
                        moveNodeUseCase(
                            nodeToMove = NodeId(nodeHandle),
                            newNodeParent = NodeId(newParentHandle),
                        )
                    }.onSuccess {
                        snackBarMessage.value = context.getString(R.string.context_correctly_moved)
                    }.onFailure {
                        Timber.d("Move node failure $it")
                        copyMoveException.value = it
                    }
                }
            }
        }
    }

    /**
     * Checks if there is a name collision before proceeding with the action.
     *
     * @param nodeHandle        Handle of the node to check the name collision.
     * @param newParentHandle   Handle of the parent folder in which the action will be performed.
     * @param completeAction    Action to complete after checking the name collision.
     */
    private suspend fun checkForNameCollision(
        nodeHandle: Long,
        newParentHandle: Long,
        type: NameCollisionType,
        completeAction: suspend (() -> Unit),
    ) {
        runCatching {
            checkNameCollision(
                nodeHandle = NodeId(nodeHandle),
                parentHandle = NodeId(newParentHandle),
                type = type,
            )
        }.onSuccess {
            collision.value = it
        }.onFailure {
            when (it) {
                is MegaNodeException.ChildDoesNotExistsException -> completeAction.invoke()
                is MegaNodeException.ParentDoesNotExistException -> {
                    snackBarMessage.value =
                        context.getString(R.string.general_error)
                }

                else -> Timber.e(it)
            }
        }
    }

    /**
     * Move node to rubbish bin
     */
    fun moveNodeToRubbishBin(nodeHandle: Long) {
        viewModelScope.launch {
            runCatching { moveNodeToRubbishBinUseCase(NodeId(nodeHandle)) }
                .onSuccess {
                    snackBarMessage.value =
                        context.getString(R.string.context_correctly_moved_to_rubbish)
                }.onFailure {
                    Timber.e("Move to rubbish bin failed $it")
                }
        }
    }

    fun removeNode(nodeHandle: Long) {
        viewModelScope.launch {
            runCatching { deleteNodeByHandleUseCase(NodeId(nodeHandle)) }
                .onSuccess {
                    snackBarMessage.value = context.getString(R.string.context_correctly_removed)
                }.onFailure {
                    Timber.d(it)
                }
        }
    }

    fun stopImageLoading(itemId: Long) {
        images.value?.find { itemId == it.id }?.imageResult?.let { imageResult ->
            imageResult.transferTag?.let { tag ->
                viewModelScope.launch {
                    runCatching {
                        cancelTransferByTagUseCase(tag)
                    }.onFailure {
                        Timber.e(it)
                    }
                }
            }
            imageResult.fullSizeUri?.let { fullSizeImageUri ->
                Fresco.getImagePipeline()?.evictFromMemoryCache(fullSizeImageUri.toUri())
            }
        }
    }

    fun onLowMemory() {
        getCurrentImageItem()?.imageResult?.fullSizeUri?.toUri()?.lastPathSegment?.let { fileName ->
            Fresco.getImagePipeline()?.bitmapMemoryCache?.removeAll {
                !it.uriString.contains(fileName)
            }
        }
    }

    /**
     * Update current image being shown based on the current position
     *
     * @param position      Position of the current image
     * @param filterVideos  Flag to filter videos and count images only
     */
    fun updateCurrentImage(position: Int, filterVideos: Boolean) {
        if (filterVideos) {
            currentImageId.value = images.value
                ?.filter { it.imageResult?.isVideo != true }
                ?.getOrNull(position)
                ?.id
        } else {
            currentImageId.value = images.value
                ?.getOrNull(position)
                ?.id
        }
    }

    /**
     * Check if transfers are paused.
     */
    fun executeTransfer(transferAction: () -> Unit) {
        viewModelScope.launch {
            if (areTransfersPausedUseCase()) {
                showTransfersAction()
            } else {
                transferAction()
            }
        }
    }

    /**
     * Check if current user is logged in
     */
    private fun checkIfUserIsLoggedIn() {
        viewModelScope.launch {
            isUserLoggedIn = isUserLoggedInUseCase()
        }
    }

    /**
     * Reused Extension Function to subscribe to a Single<List<ImageItem>>.
     *
     * @param currentNodeHandle Node handle to be shown on first load.
     */
    private fun Single<List<ImageItem>>.subscribeAndUpdateImages(currentNodeHandle: Long? = null) {
        subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { items ->
                    items.find {
                        currentNodeHandle == it.getNodeHandle() || currentNodeHandle == it.id
                    }?.let {
                        currentImageId.value = it.id
                    }
                    images.value = items.toList()
                },
                onError = { error ->
                    Timber.e(error)
                    images.value = null
                }
            )
            .addTo(composite)
    }

    private fun Completable.subscribeAndComplete(
        addToComposite: Boolean = false,
        completeAction: (() -> Unit)? = null,
        errorAction: ((Throwable) -> Unit)? = null,
    ) {
        subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    completeAction?.invoke()
                },
                onError = { error ->
                    errorAction?.invoke(error)
                    Timber.e(error)
                }
            ).also {
                if (addToComposite) it.addTo(composite)
            }
    }

    /**
     * Start slideshow
     */
    fun startSlideshow() {
        stopSlideshow()
        Observable.interval(SLIDESHOW_DELAY, SLIDESHOW_DELAY, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { slideShowState.value = STARTED }
            .subscribeBy(
                onNext = { slideShowState.value = NEXT },
                onComplete = { slideShowState.value = STOPPED },
                onError = { error ->
                    Timber.e(error)
                    slideShowState.value = STOPPED
                }
            )
            .addTo(timerComposite)
    }

    /**
     * Stop slideshow
     */
    fun stopSlideshow() {
        timerComposite.clear()
        slideShowState.value = STOPPED
    }
}

/**
 * A toolbar state class to handle extra toolbar logic
 *
 * @param show handle toolbar visibility
 * @param enableTransparency handle when should show black background
 */
data class ToolbarState(
    val show: Boolean = true,
    val enableTransparency: Boolean = false,
)
