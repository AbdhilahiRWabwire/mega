package mega.privacy.android.app.usecase

import android.app.Activity
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.di.MegaApiFolder
import mega.privacy.android.app.errors.BusinessAccountOverdueMegaError
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.usecase.data.MegaNodeItem
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.MegaNodeUtil.getLastAvailableTime
import mega.privacy.android.app.utils.MegaNodeUtil.getRootParentNode
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import java.io.File
import javax.inject.Inject

/**
 * Main use case to retrieve Mega Node information.
 *
 * @property context            Context needed to get offline node files.
 * @property megaApi            Mega API needed to call node information.
 * @property megaApiFolder      Mega API folder needed to authorize node.
 * @property databaseHandler    Database Handler needed to retrieve offline nodes.
 */
class GetNodeUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    @MegaApiFolder private val megaApiFolder: MegaApiAndroid,
    private val getChatMessageUseCase: GetChatMessageUseCase,
    private val databaseHandler: DatabaseHandler
) {

    /**
     * Get a MegaNode given a Node Handle.
     *
     * @param nodeHandle    Mega node handle
     * @return              Single with Mega Node
     */
    fun get(nodeHandle: Long): Single<MegaNode> =
        Single.fromCallable { nodeHandle.getMegaNode() }

    /**
     * Get a MegaNodeItem given a Node Handle.
     *
     * @param nodeHandle    Mega node handle
     * @return              Single with Mega Node Item
     */
    fun getNodeItem(nodeHandle: Long): Single<MegaNodeItem> =
        get(nodeHandle).flatMap(::getNodeItem)

    /**
     * Get a MegaNodeItem given a Node public link.
     *
     * @param nodeFileLink  MegaNode public link
     * @return              Single with Mega Node Item
     */
    fun getNodeItem(nodeFileLink: String): Single<MegaNodeItem> =
        getPublicNode(nodeFileLink).flatMap(::getNodeItem)

    /**
     * Get a MegaNodeItem given a Node Chat Room Id and Chat Message Id.
     *
     * @param chatRoomId    Chat Message Room Id
     * @param chatMessageId Chat Message Id
     * @return              Single with Mega Node Item
     */
    fun getNodeItem(chatRoomId: Long, chatMessageId: Long): Single<MegaNodeItem> =
        getChatMessageUseCase.getChatNode(chatRoomId, chatMessageId).flatMap(::getNodeItem)

    /**
     * Get a MegaNodeItem given a Node.
     *
     * @param node  MegaNode
     * @return      Single with Mega Node Item
     */
    fun getNodeItem(node: MegaNode?): Single<MegaNodeItem> =
        Single.fromCallable {
            requireNotNull(node)

            val nodeSizeText = Util.getSizeString(node.size)
            val nodeDateText = TimeUtils.formatLongDateTime(node.getLastAvailableTime())
            val infoText = TextUtil.getFileInfo(nodeSizeText, nodeDateText)

            var hasReadAccess = false
            var hasReadWriteAccess = false
            var hasFullAccess = false
            var hasOwnerAccess = false
            when (megaApi.getAccess(node)) {
                MegaShare.ACCESS_READ -> hasReadAccess = true
                MegaShare.ACCESS_READWRITE -> {
                    hasReadAccess = true
                    hasReadWriteAccess = true
                }
                MegaShare.ACCESS_FULL -> {
                    hasReadAccess = true
                    hasReadWriteAccess = true
                    hasFullAccess = true
                }
                MegaShare.ACCESS_OWNER -> {
                    hasReadAccess = true
                    hasReadWriteAccess = true
                    hasFullAccess = true
                    hasOwnerAccess = true
                }
            }

            val isAvailableOffline = isNodeAvailableOffline(node.handle).blockingGetOrNull() ?: false
            val hasVersions = megaApi.hasVersions(node)

            val rootParentNode = megaApi.getRootParentNode(node)
            val isFromIncoming = rootParentNode.isInShare
            var isFromRubbishBin = false
            var isFromInbox = false
            var isFromRoot = false
            when (rootParentNode.handle) {
                megaApi.rootNode?.handle -> isFromRoot = true
                megaApi.inboxNode?.handle -> isFromInbox = true
                megaApi.rubbishNode?.handle -> isFromRubbishBin = true
            }

            MegaNodeItem(
                name = node.name,
                handle = node.handle,
                infoText = infoText,
                hasReadAccess = hasReadAccess,
                hasReadWriteAccess = hasReadWriteAccess,
                hasFullAccess = hasFullAccess,
                hasOwnerAccess = hasOwnerAccess,
                isFromIncoming = isFromIncoming,
                isFromRubbishBin = isFromRubbishBin,
                isFromInbox = isFromInbox,
                isFromRoot = isFromRoot,
                hasVersions = hasVersions,
                isAvailableOffline = isAvailableOffline,
                node = node
            )
        }

    /**
     * Get a MegaOffline node given a node handle
     *
     * @param nodeHandle    Node handle to be retrieved
     * @return              Single with the MegaOffline
     */
    fun getOfflineNode(nodeHandle: Long): Single<MegaOffline> =
        Single.fromCallable {
            val offlineNode = databaseHandler.offlineFiles.find { megaOffline ->
                nodeHandle == megaOffline.handle.toLongOrNull()
                        || nodeHandle == megaOffline.handleIncoming.toLongOrNull()
            }
            offlineNode ?: error("Offline node was not found")
        }

    /**
     * Get an offline MegaNodeItem given a node handle
     *
     * @param nodeHandle    Node handle to be retrieved
     * @return              Single with the MegaNodeItem
     */
    fun getOfflineNodeItem(nodeHandle: Long): Single<MegaNodeItem> =
        Single.fromCallable {
            val offlineNode = getOfflineNode(nodeHandle).blockingGetOrNull()
            if (offlineNode != null) {
                val file = OfflineUtils.getOfflineFile(context, offlineNode)
                if (file.exists()) {
                    val nodeSizeText = Util.getSizeString(offlineNode.getSize(context))
                    val nodeDateText = TimeUtils.formatLongDateTime(offlineNode.getModificationDate(context))
                    val infoText = TextUtil.getFileInfo(nodeSizeText, nodeDateText)

                    MegaNodeItem(
                        name = offlineNode.name,
                        handle = offlineNode.handle.toLong(),
                        infoText = infoText,
                        hasReadAccess = true,
                        hasReadWriteAccess = false,
                        hasFullAccess = false,
                        hasOwnerAccess = false,
                        isFromIncoming = offlineNode.origin == MegaOffline.INCOMING,
                        isFromRubbishBin = false,
                        isFromInbox = offlineNode.origin == MegaOffline.INBOX,
                        isFromRoot = false,
                        hasVersions = false,
                        isAvailableOffline = true,
                        node = null
                    )
                } else {
                    error("Offline file doesn't exist")
                }
            } else {
                error("Offline node was not found")
            }
        }

    /**
     * Get a MegaNode given a Node public link.
     *
     * @param nodeFileLink      Node public link
     * @return                  Single with Mega Node
     */
    fun getPublicNode(nodeFileLink: String): Single<MegaNode> =
        Single.create { emitter ->
            if (nodeFileLink.isBlank()) {
                emitter.onError(IllegalArgumentException("Invalid megaFileLink"))
                return@create
            }

            megaApi.getPublicNode(nodeFileLink, OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (emitter.isDisposed) return@OptionalMegaRequestListenerInterface

                    if (error.errorCode == MegaError.API_OK) {
                        if (!request.flag) {
                            emitter.onSuccess(request.publicMegaNode)
                        } else {
                            emitter.onError(IllegalArgumentException("Invalid key for public node"))
                        }
                    } else {
                        emitter.onError(error.toThrowable())
                    }
                }
            ))
        }

    /**
     * Mark node as favorite
     *
     * @param nodeHandle    Node handle to mark as favorite
     * @param isFavorite    Flag to mark/unmark as favorite
     * @return              Completable
     */
    fun markAsFavorite(nodeHandle: Long, isFavorite: Boolean): Completable =
        get(nodeHandle).flatMapCompletable { markAsFavorite(it, isFavorite) }

    /**
     * Mark node as favorite
     *
     * @param node          Node to mark as favorite
     * @param isFavorite    Flag to mark/unmark as favorite
     * @return              Completable
     */
    fun markAsFavorite(node: MegaNode?, isFavorite: Boolean): Completable =
        Completable.fromCallable {
            requireNotNull(node)
            megaApi.setNodeFavourite(node, isFavorite)
        }

    /**
     * Check if a node is available offline
     *
     * @param nodeHandle    Mega Node handle to check
     * @return              Single with true if it's available, false otherwise
     */
    fun isNodeAvailableOffline(nodeHandle: Long): Single<Boolean> =
        Single.fromCallable {
            databaseHandler.findByHandle(nodeHandle)?.let { offlineNode ->
                val offlineFile = OfflineUtils.getOfflineFile(context, offlineNode)
                val isFileAvailable = FileUtil.isFileAvailable(offlineFile)
                val isFileDownloadedLatest = nodeHandle.getMegaNode()?.let { node ->
                    FileUtil.isFileDownloadedLatest(offlineFile, node) && offlineFile.length() == node.size
                } ?: false
                return@fromCallable isFileAvailable && isFileDownloadedLatest
            }

            return@fromCallable false
        }

    /**
     * Set node as available offline given its Node Handle.
     *
     * @param nodeHandle            Node handle to set available offline
     * @param setOffline            Flag to set/unset available offline
     * @param activity              Activity context needed to create file
     * @param isFromIncomingShares  Flag indicating if node is from incoming shares.
     * @param isFromInbox           Flag indicating if node is from inbox.
     * @return                      Completable
     */
    fun setNodeAvailableOffline(
        nodeHandle: Long,
        setOffline: Boolean,
        isFromIncomingShares: Boolean = false,
        isFromInbox: Boolean = false,
        activity: Activity
    ): Completable =
        get(nodeHandle).flatMapCompletable {
            setNodeAvailableOffline(
                it,
                setOffline,
                isFromIncomingShares,
                isFromInbox,
                activity
            )
        }

    /**
     * Set node as available offline
     *
     * @param node                  Node to set available offline
     * @param setOffline            Flag to set/unset available offline
     * @param activity              Activity context needed to create file
     * @param isFromIncomingShares  Flag indicating if node is from incoming shares.
     * @param isFromInbox           Flag indicating if node is from inbox.
     * @return                      Completable
     */
    fun setNodeAvailableOffline(
        node: MegaNode?,
        setOffline: Boolean,
        isFromIncomingShares: Boolean = false,
        isFromInbox: Boolean = false,
        activity: Activity
    ): Completable =
        Completable.fromCallable {
            requireNotNull(node)
            val isAvailableOffline = isNodeAvailableOffline(node.handle).blockingGetOrNull() ?: false
            when {
                setOffline && !isAvailableOffline -> {
                    val from = when {
                        isFromIncomingShares -> Constants.FROM_INCOMING_SHARES
                        isFromInbox -> Constants.FROM_INBOX
                        else -> Constants.FROM_OTHERS
                    }

                    val offlineParent = OfflineUtils.getOfflineParentFile(activity, from, node, megaApi)
                    if (FileUtil.isFileAvailable(offlineParent)) {
                        val offlineFile = File(offlineParent, node.name)
                        if (FileUtil.isFileAvailable(offlineFile)) {
                            val offlineNode = databaseHandler.findByHandle(node.handle)
                            OfflineUtils.removeOffline(offlineNode, databaseHandler, activity)
                        }
                    }

                    OfflineUtils.saveOffline(offlineParent, node, activity)
                }
                !setOffline && isAvailableOffline -> {
                    removeOfflineNode(node.handle, activity).blockingAwait()
                }
            }
        }

    /**
     * Remove offline node
     *
     * @param nodeHandle    Node handle to be removed
     * @param activity      Activity context needed to remove file
     * @return              Completable
     */
    fun removeOfflineNode(nodeHandle: Long, activity: Activity): Completable =
        Completable.fromCallable {
            val offlineNode = databaseHandler.findByHandle(nodeHandle)
            OfflineUtils.removeOffline(offlineNode, databaseHandler, activity)
        }

    /**
     * Copy node to a different location, either passing handles or node itself.
     *
     * @param nodeHandle        Node handle to be copied
     * @param toParentHandle    Parent node handle to be copied to
     * @param node              Node to be copied
     * @param toParentNode      Parent node to be copied to
     * @return                  Completable
     */
    fun copyNode(
        nodeHandle: Long? = null,
        toParentHandle: Long? = null,
        node: MegaNode? = null,
        toParentNode: MegaNode? = null
    ): Completable =
        Completable.fromCallable {
            require((node != null || nodeHandle != null) && (toParentNode != null || toParentHandle != null))
            copyNode(
                node ?: nodeHandle?.getMegaNode(),
                toParentNode ?: toParentHandle?.getMegaNode()
            ).blockingAwait()
        }

    /**
     * Copy node to a different location.
     *
     * @param currentNode   Node to be copied
     * @param toParentNode  Parent node to be copied to
     * @return              Completable
     */
    fun copyNode(currentNode: MegaNode?, toParentNode: MegaNode?): Completable =
        Completable.create { emitter ->
            if (currentNode == null || toParentNode == null) {
                emitter.onError(IllegalArgumentException("Null nodes"))
                return@create
            }

            megaApi.copyNode(currentNode, toParentNode, OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    if (emitter.isDisposed) return@OptionalMegaRequestListenerInterface

                    when (error.errorCode) {
                        MegaError.API_OK ->
                            emitter.onComplete()
                        MegaError.API_EBUSINESSPASTDUE ->
                            emitter.onError(BusinessAccountOverdueMegaError())
                        else ->
                            emitter.onError(error.toThrowable())
                    }
                }
            ))
        }

    /**
     * Move node to a different location
     *
     * @param nodeHandle        Node handle to be moved
     * @param toParentHandle    Parent node handle to be moved to
     * @return                  Completable
     */
    fun moveNode(nodeHandle: Long, toParentHandle: Long): Completable =
        Completable.fromCallable {
            moveNode(nodeHandle.getMegaNode(), toParentHandle.getMegaNode()).blockingAwait()
        }

    /**
     * Move a node to a different location
     *
     * @param currentNode   Node to be moved
     * @param toParentNode  Parent node to be moved to
     * @return              Completable
     */
    fun moveNode(currentNode: MegaNode?, toParentNode: MegaNode?): Completable =
        Completable.create { emitter ->
            if (currentNode == null || toParentNode == null) {
                emitter.onError(IllegalArgumentException("Null node"))
                return@create
            }

            megaApi.moveNode(currentNode, toParentNode, OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    if (emitter.isDisposed) return@OptionalMegaRequestListenerInterface

                    when (error.errorCode) {
                        MegaError.API_OK ->
                            emitter.onComplete()
                        MegaError.API_EBUSINESSPASTDUE ->
                            emitter.onError(BusinessAccountOverdueMegaError())
                        else ->
                            emitter.onError(error.toThrowable())
                    }
                }
            ))
        }

    /**
     * Move a node to the Rubbish bin
     *
     * @param nodeHandle    Node handle to be moved
     * @return              Completable
     */
    fun moveToRubbishBin(nodeHandle: Long): Completable =
        Completable.fromCallable {
            moveNode(nodeHandle, megaApi.rubbishNode.handle).blockingAwait()
        }

    /**
     * Remove a node
     *
     * @param nodeHandle    Node handle to be removed
     * @return              Completable
     */
    fun removeNode(nodeHandle: Long): Completable =
        Completable.fromCallable {
            removeNode(nodeHandle.getMegaNode()).blockingAwait()
        }

    /**
     * Remove a node
     *
     * @param node  Node to be removed
     * @return      Completable
     */
    fun removeNode(node: MegaNode?): Completable =
        Completable.create { emitter ->
            if (node == null) {
                emitter.onError(IllegalArgumentException("Null node"))
                return@create
            }

            megaApi.remove(node, OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    if (emitter.isDisposed) return@OptionalMegaRequestListenerInterface

                    when (error.errorCode) {
                        MegaError.API_OK ->
                            emitter.onComplete()
                        MegaError.API_EMASTERONLY ->
                            emitter.onError(IllegalStateException("Sub-user business account"))
                        else ->
                            emitter.onError(error.toThrowable())
                    }
                }
            ))
        }

    /**
     * Get a MegaNode given a Long handle in a synchronous way.
     * This will also authorize the Node if required.
     *
     * @return  MegaNode
     */
    private fun Long.getMegaNode(): MegaNode? =
        megaApi.getNodeByHandle(this)
            ?: megaApiFolder.authorizeNode(megaApiFolder.getNodeByHandle(this))
}
