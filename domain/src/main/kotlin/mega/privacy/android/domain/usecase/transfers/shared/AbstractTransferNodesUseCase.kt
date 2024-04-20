package mega.privacy.android.domain.usecase.transfers.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.canceltoken.InvalidateCancelTokenUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleTransferEventUseCase

/**
 * Helper class to implement common logic for transfer multiple items (upload or download)
 * @param T type of the items to be transferred
 * @param R type of the items key to match the item with the related transfer
 */
abstract class AbstractTransferNodesUseCase<T, R>(
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase,
    private val invalidateCancelTokenUseCase: InvalidateCancelTokenUseCase,
    private val handleTransferEventUseCase: HandleTransferEventUseCase,
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
) {

    internal abstract fun generateIdFromItem(item: T): R
    internal abstract fun generateIdFromTransferEvent(transferEvent: TransferEvent): R

    internal fun commonInvoke(
        items: List<T>,
        beforeStartTransfer: (suspend () -> Unit)?,
        doTransfer: (T) -> Flow<TransferEvent>,
    ): Flow<MultiTransferEvent> {
        val alreadyScanned =
            mutableSetOf<R>() //to check if all [items] have been scanned (childs not needed here)
        val filesStarted =
            mutableSetOf<R>() //to count the number of files that have been started (no folders but including children)
        val alreadyTransferredFiles = mutableSetOf<R>()
        val alreadyTransferredNodeIds = mutableSetOf<NodeId>()
        val allIds = items.map(::generateIdFromItem)
        var scanningFinishedSend = false
        return channelFlow {
            monitorTransferEvents()
            //start all transfers in parallel
            items.map { node ->
                launch {
                    doTransfer(node)
                        .catch { cause ->
                            val id = generateIdFromItem(node)
                            if (cause is NodeDoesNotExistsException) {
                                send(MultiTransferEvent.TransferNotStarted(id, cause))
                            }
                            alreadyScanned.add(id)
                        }
                        .buffer(capacity = Channel.UNLIMITED)
                        .collect { transferEvent ->
                            totalBytesMap[transferEvent.transfer.tag] =
                                transferEvent.transfer.totalBytes
                            transferredBytesMap[transferEvent.transfer.tag] =
                                transferEvent.transfer.transferredBytes

                            if (transferEvent is TransferEvent.TransferStartEvent) {
                                rootTags += transferEvent.transfer.tag
                            }
                            //update active transfers db, sd transfers, etc.
                            handleTransferEventUseCase(transferEvent)

                            //keep track of file counters
                            if (transferEvent.isFileTransfer) {
                                val id = generateIdFromTransferEvent(transferEvent)
                                filesStarted.add(id)
                                if (transferEvent.isAlreadyTransferredEvent) {
                                    alreadyTransferredFiles.add(id)
                                    alreadyTransferredNodeIds.add(NodeId(transferEvent.transfer.nodeHandle))
                                }
                            }
                            //check if is a single node scanning finish event
                            if (!scanningFinishedSend && transferEvent.isFinishScanningEvent) {
                                val id = generateIdFromTransferEvent(transferEvent)
                                if (!alreadyScanned.contains(id)) {
                                    //this node is already scanned: save it and emit the event
                                    alreadyScanned.add(id)

                                    //check if all nodes have been scanned
                                    if (alreadyScanned.containsAll(allIds)) {
                                        scanningFinishedSend = true
                                        invalidateCancelTokenUseCase() //we need to avoid a future cancellation from now on
                                    }
                                }
                            }

                            send(
                                MultiTransferEvent.SingleTransferEvent(
                                    transferEvent = transferEvent,
                                    totalBytesTransferred = transferredBytes,
                                    totalBytesToTransfer = totalBytes,
                                    startedFiles = filesStarted.size,
                                    alreadyTransferred = alreadyTransferredFiles.size,
                                    alreadyTransferredIds = alreadyTransferredNodeIds,
                                    scanningFinished = scanningFinishedSend,
                                )
                            )
                        }
                }
            }.joinAll()
            close()
        }
            .onStart {
                beforeStartTransfer?.invoke()
            }
            .onCompletion {
                runCatching { cancelCancelTokenUseCase() }
            }.cancellable()
    }

    /**
     * tags of the transfers directly initiated by a sdk call, so we can check children transfers of all nodes
     */
    private val rootTags = mutableListOf<Int>()

    /**
     * total bytes for each transfer directly initiated by a sdk call, so we can compute the sum of all nodes
     */
    private val totalBytesMap = mutableMapOf<Int, Long>()
    private val totalBytes get() = totalBytesMap.values.sum()

    /**
     * total transferredBytes for each transfer directly initiated by a sdk call, so we can compute the sum of all nodes
     */
    private val transferredBytesMap = mutableMapOf<Int, Long>()
    private val transferredBytes get() = transferredBytesMap.values.sum()

    /**
     * Monitors download child transfer global events and update the related active transfers
     */
    private fun CoroutineScope.monitorTransferEvents() =
        this.launch {
            monitorTransferEventsUseCase()
                .filter { event ->
                    //only children as events of the related nodes are already handled
                    event.transfer.folderTransferTag?.let { rootTags.contains(it) } == true
                }
                .collect { transferEvent ->
                    withContext(NonCancellable) {
                        handleTransferEventUseCase(transferEvent)
                    }
                }
        }

    /**
     * This event indicates that the transfer was not done due to being already transferred.
     */
    private val TransferEvent.isAlreadyTransferredEvent: Boolean
        get() = with(this.transfer) {
            !isFolderTransfer && isAlreadyDownloaded
        }

    /**
     * This event is related to a file transfer, not a folder.
     */
    private val TransferEvent.isFileTransfer: Boolean
        get() = !this.transfer.isFolderTransfer

    /**
     * return true if this event represents a finish processing event (or already finished)
     */
    private val TransferEvent.isFinishScanningEvent: Boolean
        get() = when {
            this is TransferEvent.TransferUpdateEvent &&
                    transfer.isFolderTransfer && transfer.stage == mega.privacy.android.domain.entity.transfer.TransferStage.STAGE_TRANSFERRING_FILES -> {
                true
            }

            this is TransferEvent.TransferFinishEvent -> true
            this is TransferEvent.TransferUpdateEvent && !transfer.isFolderTransfer -> true
            else -> false
        }
}