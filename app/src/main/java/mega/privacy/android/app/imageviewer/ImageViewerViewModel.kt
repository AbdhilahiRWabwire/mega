package mega.privacy.android.app.imageviewer

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.facebook.drawee.backends.pipeline.Fresco
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.getLink.useCase.ExportNodeUseCase
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.imageviewer.data.ImageResult
import mega.privacy.android.app.imageviewer.usecase.GetImageHandlesUseCase
import mega.privacy.android.app.imageviewer.usecase.GetImageUseCase
import mega.privacy.android.app.usecase.CancelTransferUseCase
import mega.privacy.android.app.usecase.GetGlobalChangesUseCase
import mega.privacy.android.app.usecase.GetGlobalChangesUseCase.Result
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.usecase.LoggedInUseCase
import mega.privacy.android.app.usecase.data.MegaNodeItem
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.MegaNodeUtil.isValidForImageViewer
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Main ViewModel to handle all logic related to the ImageViewer.
 * This is shared between ImageViewerActivity behaving as the main container and
 * each individual ImageViewerPageFragment representing a single image within the ViewPager.
 *
 * @property getImageUseCase        Needed to retrieve each individual image based on a node.
 * @property getImageHandlesUseCase Needed to retrieve node handles given sent params.
 * @property getNodeUseCase         Needed to retrieve each individual node based on a node handle,
 *                                  as well as each individual node action required by the menu.
 * @property exportNodeUseCase      Needed to export image node on demand.
 * @property cancelTransferUseCase  Needed to cancel current full image transfer if needed.
 */
@HiltViewModel
class ImageViewerViewModel @Inject constructor(
    private val getImageUseCase: GetImageUseCase,
    private val getImageHandlesUseCase: GetImageHandlesUseCase,
    private val getGlobalChangesUseCase: GetGlobalChangesUseCase,
    private val getNodeUseCase: GetNodeUseCase,
    private val exportNodeUseCase: ExportNodeUseCase,
    private val cancelTransferUseCase: CancelTransferUseCase,
    private val loggedInUseCase: LoggedInUseCase,
) : BaseRxViewModel() {

    private val images = MutableLiveData<List<ImageItem>?>()
    private val currentPosition = MutableLiveData<Int>()
    private val showToolbar = MutableLiveData<Boolean>()
    private val snackbarMessage = SingleLiveEvent<String>()
    private val nodesComposite = CompositeDisposable()
    private val imagesComposite = CompositeDisposable()
    private var isUserLoggedIn = false

    init {
        checkIfUserIsLoggedIn()
        subscribeToNodeChanges()
    }

    override fun onCleared() {
        nodesComposite.clear()
        imagesComposite.clear()
        Fresco.getImagePipeline()?.clearMemoryCaches()
        super.onCleared()
    }

    fun onImagesHandle(): LiveData<List<Long>?> =
        images.map { items -> items?.map(ImageItem::handle) }

    fun onImage(nodeHandle: Long): LiveData<ImageItem?> =
        images.map { items -> items?.firstOrNull { it.handle == nodeHandle } }

    fun onCurrentPosition(): LiveData<Pair<Int, Int>> =
        currentPosition.map { position -> Pair(position, images.value?.size ?: 0) }

    fun onCurrentImageItem(): LiveData<ImageItem?> =
        currentPosition.map { images.value?.getOrNull(it) }

    fun getCurrentImageItem(): ImageItem? =
        currentPosition.value?.let { images.value?.getOrNull(it) }

    fun getImageItem(nodeHandle: Long): ImageItem? =
        images.value?.find { it.handle == nodeHandle }

    fun onSnackbarMessage(): LiveData<String> = snackbarMessage

    fun onShowToolbar(): LiveData<Boolean> = showToolbar

    fun isToolbarShown(): Boolean = showToolbar.value ?: false

    fun retrieveSingleImage(nodeHandle: Long, isOffline: Boolean = false) {
        getImageHandlesUseCase.get(nodeHandles = longArrayOf(nodeHandle), isOffline = isOffline)
            .subscribeAndUpdateImages()
    }

    fun retrieveSingleImage(nodeFileLink: String) {
        getImageHandlesUseCase.get(nodeFileLinks = listOf(nodeFileLink))
            .subscribeAndUpdateImages()
    }

    fun retrieveFileImage(imageUri: Uri, showNearbyFiles: Boolean? = false, currentNodeHandle: Long? = null) {
        getImageHandlesUseCase.get(imageFileUri = imageUri, showNearbyFiles = showNearbyFiles)
            .subscribeAndUpdateImages(currentNodeHandle)
    }

    fun retrieveImagesFromParent(
        parentNodeHandle: Long,
        childOrder: Int? = null,
        currentNodeHandle: Long? = null
    ) {
        getImageHandlesUseCase.get(parentNodeHandle = parentNodeHandle, sortOrder = childOrder)
            .subscribeAndUpdateImages(currentNodeHandle)
    }

    fun retrieveImages(
        nodeHandles: LongArray,
        currentNodeHandle: Long? = null,
        isOffline: Boolean = false
    ) {
        getImageHandlesUseCase.get(nodeHandles = nodeHandles, isOffline = isOffline)
            .subscribeAndUpdateImages(currentNodeHandle)
    }

    fun retrieveChatImages(
        chatRoomId: Long,
        messageIds: LongArray,
        currentNodeHandle: Long? = null
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
     * @param nodeHandle    Image node handle to be loaded.
     */
    fun loadSingleNode(nodeHandle: Long) {
        val imageItem = images.value?.find { it.handle == nodeHandle }
        val subscription = when {
            imageItem == null ->
                return
            imageItem.nodePublicLink?.isNotBlank() == true ->
                getNodeUseCase.getNodeItem(imageItem.nodePublicLink)
            imageItem.chatMessageId != null && imageItem.chatRoomId != null ->
                getNodeUseCase.getNodeItem(imageItem.chatRoomId, imageItem.chatMessageId)
            imageItem.isOffline ->
                getNodeUseCase.getOfflineNodeItem(imageItem.handle)
            imageItem.handle != INVALID_HANDLE ->
                getNodeUseCase.getNodeItem(nodeHandle)
            imageItem.nodeItem?.node != null ->
                getNodeUseCase.getNodeItem(imageItem.nodeItem.node)
            else ->
                return // Image file uri with no handle
        }

        subscription
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .retry(1)
            .subscribeBy(
                onSuccess = { nodeItem ->
                    updateItemIfNeeded(nodeHandle, nodeItem = nodeItem)
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    /**
     * Main method to request an ImageResult given a previously loaded Node handle.
     * This will update the current Image on the main "images" list if it's newer.
     * You must be observing the requested Image to get the updated result.
     *
     * @param nodeHandle    Image node handle to be loaded.
     * @param fullSize      Flag to request full size image despite data/size requirements.
     * @param highPriority  Flag to request image with high priority.
     */
    fun loadSingleImage(nodeHandle: Long, fullSize: Boolean, highPriority: Boolean) {
        val imageItem = images.value?.find { it.handle == nodeHandle }
        val fullSizeRequired = fullSize || images.value?.size == 1

        val subscription = when {
            imageItem == null
                    || (imageItem.imageResult?.isFullyLoaded == true
                    && imageItem.imageResult.fullSizeUri != null
                    && imageItem.imageResult.previewUri != null) ->
                return
            imageItem.nodePublicLink?.isNotBlank() == true ->
                getImageUseCase.get(imageItem.nodePublicLink, fullSizeRequired, highPriority)
            imageItem.chatMessageId != null && imageItem.chatRoomId != null ->
                getImageUseCase.get(imageItem.chatRoomId, imageItem.chatMessageId, fullSizeRequired, highPriority)
            imageItem.isOffline ->
                getImageUseCase.getOffline(imageItem.handle)
            imageItem.handle != INVALID_HANDLE ->
                getImageUseCase.get(nodeHandle, fullSizeRequired, highPriority)
            imageItem.nodeItem?.node != null ->
                getImageUseCase.get(imageItem.nodeItem.node, fullSizeRequired, highPriority)
            else ->
                return // Image file uri with no handle
        }

        subscription
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .retry(1)
            .subscribeBy(
                onNext = { imageResult ->
                    updateItemIfNeeded(nodeHandle, imageResult = imageResult)
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    /**
     * Update a specific ImageItem from the Images list with the provided
     * MegaNodeItem or ImageResult
     *
     * @param nodeHandle    Item node handle to be updated
     * @param nodeItem      MegaNodeItem to be updated with
     * @param imageResult   ImageResult to be updated with
     */
    private fun updateItemIfNeeded(
        nodeHandle: Long,
        nodeItem: MegaNodeItem? = null,
        imageResult: ImageResult? = null
    ) {
        if (nodeItem == null && imageResult == null) return

        val items = images.value?.toMutableList()
        if (!items.isNullOrEmpty()) {
            val index = items.indexOfFirst { it.handle == nodeHandle }
            if (index != INVALID_POSITION) {
                val currentItem = items[index]
                if (nodeItem != null) {
                    items[index] = currentItem.copy(
                        nodeItem = nodeItem
                    )
                }
                if (imageResult != null && imageResult != currentItem.imageResult) {
                    items[index] = currentItem.copy(
                        imageResult = imageResult
                    )
                }

                images.value = items.toList()
                if (index == currentPosition.value) {
                    updateCurrentPosition(index, true)
                }
            } else {
                logWarning("Node $nodeHandle not found")
            }
        } else {
            logWarning("Images are null or empty")
            images.value = null
        }
    }

    /**
     * Subscribe to latest node changes to update existing ones.
     */
    @Suppress("SENSELESS_COMPARISON")
    private fun subscribeToNodeChanges() {
        getGlobalChangesUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .filter { change -> change is Result.OnNodesUpdate }
            .subscribeBy(
                onNext = { change ->
                    val items = images.value?.toMutableList()
                    if (items.isNullOrEmpty()) {
                        logWarning("Images are null or empty")
                        return@subscribeBy
                    }

                    val dirtyNodeHandles = mutableListOf<Long>()
                    (change as Result.OnNodesUpdate).nodes?.forEach { changedNode ->
                        val currentIndex = items.indexOfFirst { it.handle == changedNode.handle }
                        when {
                            changedNode.hasChanged(MegaNode.CHANGE_TYPE_NEW) -> {
                                val hasSameParent = (changedNode.parentHandle != null
                                        && changedNode.parentHandle == items.firstOrNull()?.nodeItem?.node?.parentHandle)
                                if (hasSameParent && changedNode.isValidForImageViewer()) {
                                    items.add(ImageItem(changedNode.handle))
                                    dirtyNodeHandles.add(changedNode.handle)
                                }
                            }
                            changedNode.hasChanged(MegaNode.CHANGE_TYPE_PARENT) -> {
                                if (currentIndex != INVALID_POSITION) {
                                    val hasSameParent = (changedNode.parentHandle != null
                                            && changedNode.parentHandle == items.firstOrNull()?.nodeItem?.node?.parentHandle)
                                    if (!hasSameParent) {
                                        items.removeAt(currentIndex)
                                    }
                                }
                            }
                            changedNode.hasChanged(MegaNode.CHANGE_TYPE_REMOVED) -> {
                                if (currentIndex != INVALID_POSITION) {
                                    items.removeAt(currentIndex)
                                }
                            }
                            currentIndex != INVALID_POSITION -> {
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
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    fun markNodeAsFavorite(nodeHandle: Long, isFavorite: Boolean) {
        getNodeUseCase.markAsFavorite(nodeHandle, isFavorite)
            .subscribeAndComplete()
    }

    fun switchNodeOfflineAvailability(
        nodeItem: MegaNodeItem,
        activity: Activity
    ) {
        getNodeUseCase.setNodeAvailableOffline(
            node = nodeItem.node,
            setOffline = !nodeItem.isAvailableOffline,
            isFromIncomingShares = nodeItem.isFromIncoming,
            isFromInbox = nodeItem.isFromInbox,
            activity = activity
        ).subscribeAndComplete {
            loadSingleNode(nodeItem.handle)
        }
    }

    fun removeOfflineNode(nodeHandle: Long, activity: Activity) {
        getNodeUseCase.removeOfflineNode(nodeHandle, activity)
            .subscribeAndComplete {
                val currentIndex = images.value?.indexOfFirst { it.handle == nodeHandle } ?: INVALID_POSITION
                if (currentIndex != INVALID_POSITION) {
                    val items = images.value!!.toMutableList().apply {
                        removeAt(currentIndex)
                    }
                    images.value = items.toList()
                    calculateNewPosition(items)
                }
            }
    }

    /**
     * Calculate new ViewPager position based on a new list of items
     *
     * @param newItems  New ImageItems to calculate new position from
     */
    private fun calculateNewPosition(newItems: List<ImageItem>) {
        val items = images.value?.toMutableList()
        val newPosition =
            if (items.isNullOrEmpty()) {
                0
            } else {
                val currentPositionNewIndex = newItems.indexOfFirst { it.handle == getCurrentImageItem()?.handle }
                val currentItemPosition = currentPosition.value ?: 0
                when {
                    currentPositionNewIndex != INVALID_POSITION ->
                        currentPositionNewIndex
                    currentItemPosition >= items.size ->
                        items.size - 1
                    currentItemPosition == 0 ->
                        currentItemPosition + 1
                    else ->
                        currentItemPosition
                }
            }

        updateCurrentPosition(newPosition, true)
    }

    fun removeLink(nodeHandle: Long) {
        exportNodeUseCase.disableExport(nodeHandle)
            .subscribeAndComplete {
                snackbarMessage.value = getQuantityString(R.plurals.context_link_removal_success, 1)
            }
    }

    fun shareNode(node: MegaNode): LiveData<String?> {
        val result = MutableLiveData<String?>()
        exportNodeUseCase.export(node)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { link ->
                    result.value = link
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                    result.value = null
                }
            )
            .addTo(composite)
        return result
    }

    fun copyNode(nodeHandle: Long, newParentHandle: Long) {
        getNodeUseCase.copyNode(
            node = getExistingNode(nodeHandle),
            nodeHandle = nodeHandle,
            toParentHandle = newParentHandle
        ).subscribeAndComplete {
            snackbarMessage.value = getString(R.string.context_correctly_copied)
        }
    }

    fun moveNode(nodeHandle: Long, newParentHandle: Long) {
        getNodeUseCase.moveNode(nodeHandle, newParentHandle)
            .subscribeAndComplete {
                snackbarMessage.value = getString(R.string.context_correctly_moved)
            }
    }

    fun moveNodeToRubbishBin(nodeHandle: Long) {
        getNodeUseCase.moveToRubbishBin(nodeHandle)
            .subscribeAndComplete {
                snackbarMessage.value = getString(R.string.context_correctly_moved_to_rubbish)
            }
    }

    fun removeNode(nodeHandle: Long) {
        getNodeUseCase.removeNode(nodeHandle)
            .subscribeAndComplete {
                snackbarMessage.value = getString(R.string.context_correctly_removed)
            }
    }

    fun stopImageLoading(nodeHandle: Long, aggressive: Boolean) {
        images.value?.find { nodeHandle == it.handle }?.imageResult?.let { imageResult ->
            imageResult.fullSizeUri?.let { fullSizeImageUri ->
                Fresco.getImagePipeline()?.evictFromMemoryCache(fullSizeImageUri)
            }
            if (aggressive) {
                imageResult.transferTag?.let { tag ->
                    cancelTransferUseCase.cancel(tag)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(
                            onError = { error ->
                                logError(error.stackTraceToString())
                            }
                        )
                }
            }
        }
    }

    fun updateCurrentPosition(position: Int, forceUpdate: Boolean) {
        if (forceUpdate || position != currentPosition.value) {
            currentPosition.value = position
        }
    }

    fun switchToolbar(show: Boolean? = null) {
        showToolbar.value = show ?: showToolbar.value?.not() ?: true
    }

    private fun getExistingNode(nodeHandle: Long): MegaNode? =
        images.value?.find { it.handle == nodeHandle }?.nodeItem?.node

    /**
     * Check if current user is logged in
     */
    private fun checkIfUserIsLoggedIn() {
        loggedInUseCase.isUserLoggedIn()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { isLoggedIn ->
                    isUserLoggedIn = isLoggedIn
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
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
                    images.value = items.toList()

                    val position = items.indexOfFirst { it.handle == currentNodeHandle }
                    if (position != INVALID_POSITION) {
                        updateCurrentPosition(position, true)
                    } else {
                        updateCurrentPosition(0, true)
                    }
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                    images.value = null
                }
            )
            .addTo(composite)
    }

    private fun Completable.subscribeAndComplete(completeAction: (() -> Unit)? = null) {
        subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    completeAction?.invoke()
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }
}
