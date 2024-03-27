package mega.privacy.android.app.presentation.transfers.startdownload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.middlelayer.iar.OnCompleteListener
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.transfers.TransfersConstants
import mega.privacy.android.app.presentation.transfers.startdownload.model.StartDownloadTransferEvent
import mega.privacy.android.app.presentation.transfers.startdownload.model.StartDownloadTransferJobInProgress
import mega.privacy.android.app.presentation.transfers.startdownload.model.StartDownloadTransferViewState
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.SetStorageDownloadAskAlwaysUseCase
import mega.privacy.android.domain.usecase.SetStorageDownloadLocationUseCase
import mega.privacy.android.domain.usecase.file.TotalFileSizeOfNodesUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.GetFilePreviewDownloadPathUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflinePathForNodeUseCase
import mega.privacy.android.domain.usecase.setting.IsAskBeforeLargeDownloadsSettingUseCase
import mega.privacy.android.domain.usecase.setting.SetAskBeforeLargeDownloadsSettingUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorActiveTransferFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetCurrentDownloadSpeedUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetOrCreateStorageDownloadLocationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.SaveDoNotPromptToSaveDestinationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.ShouldAskDownloadDestinationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.ShouldPromptToSaveDestinationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.StartDownloadsWithWorkerUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * View model to handle start downloading
 */
@HiltViewModel
internal class StartDownloadComponentViewModel @Inject constructor(
    private val getOfflinePathForNodeUseCase: GetOfflinePathForNodeUseCase,
    private val getOrCreateStorageDownloadLocationUseCase: GetOrCreateStorageDownloadLocationUseCase,
    private val getFilePreviewDownloadPathUseCase: GetFilePreviewDownloadPathUseCase,
    private val startDownloadsWithWorkerUseCase: StartDownloadsWithWorkerUseCase,
    private val clearActiveTransfersIfFinishedUseCase: ClearActiveTransfersIfFinishedUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val totalFileSizeOfNodesUseCase: TotalFileSizeOfNodesUseCase,
    private val fileSizeStringMapper: FileSizeStringMapper,
    private val isAskBeforeLargeDownloadsSettingUseCase: IsAskBeforeLargeDownloadsSettingUseCase,
    private val setAskBeforeLargeDownloadsSettingUseCase: SetAskBeforeLargeDownloadsSettingUseCase,
    private val monitorOngoingActiveTransfersUseCase: MonitorOngoingActiveTransfersUseCase,
    private val getCurrentDownloadSpeedUseCase: GetCurrentDownloadSpeedUseCase,
    private val shouldAskDownloadDestinationUseCase: ShouldAskDownloadDestinationUseCase,
    private val shouldPromptToSaveDestinationUseCase: ShouldPromptToSaveDestinationUseCase,
    private val saveDoNotPromptToSaveDestinationUseCase: SaveDoNotPromptToSaveDestinationUseCase,
    private val setStorageDownloadAskAlwaysUseCase: SetStorageDownloadAskAlwaysUseCase,
    private val setStorageDownloadLocationUseCase: SetStorageDownloadLocationUseCase,
    private val monitorActiveTransferFinishedUseCase: MonitorActiveTransferFinishedUseCase,
) : ViewModel() {

    private var currentInProgressJob: Job? = null

    private val _uiState = MutableStateFlow(StartDownloadTransferViewState())

    /**
     * the state of the view
     */
    internal val uiState = _uiState.asStateFlow()

    init {
        checkRating()
    }

    /**
     * It starts downloading the related nodes, asking for confirmation in case of large transfers if corresponds
     * @param transferTriggerEvent the event that triggered this download
     */
    fun startDownload(
        transferTriggerEvent: TransferTriggerEvent,
    ) {
        if (checkAndHandleDeviceIsNotConnected()) {
            return
        }
        viewModelScope.launch {
            if (transferTriggerEvent.nodes.isEmpty()) {
                Timber.e("Node in $transferTriggerEvent must exist")
                _uiState.updateEventAndClearProgress(StartDownloadTransferEvent.Message.TransferCancelled)
            } else if (!checkAndHandleNeedConfirmationForLargeDownload(transferTriggerEvent)) {
                startDownloadWithoutConfirmation(transferTriggerEvent)
            }
        }
    }

    /**
     * It starts downloading the related nodes, without asking confirmation for large transfers (because it's already asked)
     * @param transferTriggerEvent the event that triggered this download
     * @param saveDoNotAskAgainForLargeTransfers if true, it will save in settings to don't ask again for confirmation on large files
     */
    fun startDownloadWithoutConfirmation(
        transferTriggerEvent: TransferTriggerEvent,
        saveDoNotAskAgainForLargeTransfers: Boolean = false,
    ) {
        if (saveDoNotAskAgainForLargeTransfers) {
            viewModelScope.launch {
                setAskBeforeLargeDownloadsSettingUseCase(askForConfirmation = false)
            }
        }
        val node = transferTriggerEvent.nodes.firstOrNull()
        if (node == null) {
            Timber.e("Node in $transferTriggerEvent must exist")
            _uiState.updateEventAndClearProgress(StartDownloadTransferEvent.Message.TransferCancelled)
        } else {
            _uiState.update {
                it.copy(transferTriggerEvent = transferTriggerEvent)
            }
            when (transferTriggerEvent) {
                is TransferTriggerEvent.StartDownloadForOffline -> {
                    startDownloadForOffline(node, transferTriggerEvent.isHighPriority)
                }

                is TransferTriggerEvent.StartDownloadNode -> {
                    viewModelScope.launch {
                        if (shouldAskDownloadDestinationUseCase()) {
                            _uiState.updateEventAndClearProgress(
                                StartDownloadTransferEvent.AskDestination(
                                    transferTriggerEvent
                                )
                            )
                        } else {
                            startDownloadNodes(
                                transferTriggerEvent,
                                getOrCreateStorageDownloadLocationUseCase()
                            )
                        }
                    }
                }

                is TransferTriggerEvent.StartDownloadForPreview -> {
                    startDownloadNodeForPreview(node)
                }
            }
        }
    }

    /**
     * Start download with the destination manually set by the user
     * @param startDownloadNode initial event that triggered this download
     * @param destinationUri the chosen destination
     */
    fun startDownloadWithDestination(
        startDownloadNode: TransferTriggerEvent.StartDownloadNode,
        destinationUri: Uri,
    ) {
        viewModelScope.launch {
            startDownloadNodes(startDownloadNode, destinationUri.toString())
            if (shouldPromptToSaveDestinationUseCase()) {
                _uiState.update {
                    it.copy(promptSaveDestination = triggered(destinationUri.toString()))
                }
            }
        }
    }

    /**
     * It starts downloading the node for preview with the appropriate use case
     * @param node the [Node] to be downloaded for preview
     */
    private fun startDownloadNodeForPreview(node: TypedNode) {
        currentInProgressJob = viewModelScope.launch {
            startDownloadNodes(
                nodes = listOf(node),
                isHighPriority = true,
                getPath = {
                    getFilePreviewDownloadPathUseCase()
                },
            )
        }
    }

    /**
     * It starts downloading the nodes with the appropriate use case
     * @param startDownloadNode the [TransferTriggerEvent] that starts this download
     * @param destination the destination where to download the nodes
     */
    private fun startDownloadNodes(
        startDownloadNode: TransferTriggerEvent.StartDownloadNode,
        destination: String?,
    ) {
        val siblingNodes = startDownloadNode.nodes
        if (siblingNodes.isEmpty()) return
        val firstSibling = siblingNodes.first()
        val parentId = firstSibling.parentId
        if (!siblingNodes.all { it.parentId == parentId }) {
            Timber.e("All nodes must have the same parent")
            _uiState.updateEventAndClearProgress(StartDownloadTransferEvent.Message.TransferCancelled)
        } else {
            currentInProgressJob = viewModelScope.launch {
                startDownloadNodes(
                    nodes = siblingNodes,
                    isHighPriority = startDownloadNode.isHighPriority,
                    getPath = {
                        destination?.ensureSuffix(File.separator)
                    },
                )
            }
        }
    }

    /**
     * It starts downloading the node for offline with the appropriate use case
     * @param node the [Node] to be saved offline
     */
    private fun startDownloadForOffline(node: TypedNode, isHighPriority: Boolean) {
        currentInProgressJob = viewModelScope.launch {
            startDownloadNodes(
                nodes = listOf(node),
                isHighPriority,
                getPath = {
                    getOfflinePathForNodeUseCase(node)
                },
            )
        }
    }

    /**
     * common logic to start downloading nodes, either for offline or ordinary download
     */
    private suspend fun startDownloadNodes(
        nodes: List<TypedNode>,
        isHighPriority: Boolean,
        getPath: suspend () -> String?,
    ) {
        monitorDownloadFinishJob?.cancel()
        clearActiveTransfersIfFinishedUseCase(TransferType.DOWNLOAD)
        monitorDownloadFinish()
        _uiState.update {
            it.copy(jobInProgressState = StartDownloadTransferJobInProgress.ProcessingFiles)
        }
        var lastError: Throwable? = null
        val terminalEvent = runCatching {
            getPath().also {
                if (it.isNullOrBlank()) {
                    throw NullPointerException("path not found!")
                }
            }
        }.onFailure { lastError = it }
            .getOrNull()?.let { path ->
                startDownloadsWithWorkerUseCase(
                    destinationPath = path,
                    nodes = nodes,
                    isHighPriority = isHighPriority,
                ).catch {
                    lastError = it
                    Timber.e(it)
                }.onCompletion {
                    if (it is CancellationException) {
                        _uiState.updateEventAndClearProgress(StartDownloadTransferEvent.Message.TransferCancelled)
                    }
                }.last()
            }
        checkRating()
        _uiState.updateEventAndClearProgress(
            when (terminalEvent) {
                MultiTransferEvent.InsufficientSpace -> StartDownloadTransferEvent.Message.NotSufficientSpace
                else -> {
                    val finishedEvent = (terminalEvent as? MultiTransferEvent.SingleTransferEvent)
                    StartDownloadTransferEvent.FinishProcessing(
                        exception = lastError?.takeIf { terminalEvent == null },
                        totalNodes = nodes.size,
                        totalFiles = finishedEvent?.startedFiles ?: 0,
                        totalAlreadyDownloaded = finishedEvent?.alreadyTransferred ?: 0,
                    )
                }
            }
        )
    }

    /**
     * Some events need to be consumed to don't be missed or fired more than once
     */
    fun consumeOneOffEvent() {
        _uiState.updateEventAndClearProgress(null)
    }

    /**
     * Cancel current in progress job
     */
    fun cancelCurrentJob() {
        currentInProgressJob?.cancel()
        _uiState.update {
            it.copy(
                jobInProgressState = null,
            )
        }
    }

    /**
     * consume prompt save destination event
     */
    fun consumePromptSaveDestination() {
        _uiState.update {
            it.copy(promptSaveDestination = consumed())
        }
    }

    /**
     * Save selected destination as location for future downloads
     */
    fun saveDestination(destination: String) {
        viewModelScope.launch {
            runCatching {
                setStorageDownloadLocationUseCase(destination)
                setStorageDownloadAskAlwaysUseCase(false)
            }.onFailure {
                Timber.e("Error saving the destination:\n$it")
            }
        }
    }

    /**
     * Save setting to don't prompt the user again to save selected destination
     */
    fun doNotPromptToSaveDestinationAgain() {
        viewModelScope.launch {
            runCatching {
                saveDoNotPromptToSaveDestinationUseCase()
            }.onFailure {
                Timber.e("Error saving the don't save destination again prompt:\n$it")
            }

        }
    }

    private fun checkAndHandleDeviceIsNotConnected() =
        if (!isConnectedToInternetUseCase()) {
            _uiState.updateEventAndClearProgress(StartDownloadTransferEvent.NotConnected)
            true
        } else {
            false
        }

    /**
     * Checks if confirmation dialog for large download should be shown and updates uiState if so
     *
     * @return true if the state has been handled to ask for confirmation, so no extra action should be done
     */
    private suspend fun checkAndHandleNeedConfirmationForLargeDownload(transferTriggerEvent: TransferTriggerEvent): Boolean {
        if (isAskBeforeLargeDownloadsSettingUseCase()) {
            val size = totalFileSizeOfNodesUseCase(transferTriggerEvent.nodes)
            if (size > TransfersConstants.CONFIRM_SIZE_MIN_BYTES) {
                _uiState.updateEventAndClearProgress(
                    StartDownloadTransferEvent.ConfirmLargeDownload(
                        fileSizeStringMapper(size), transferTriggerEvent
                    )
                )
                return true
            }
        }
        return false
    }

    private var checkShowRating = true
    private fun checkRating() {
        //check download speed and size to show rating
        if (checkShowRating) {
            viewModelScope.launch {
                monitorOngoingActiveTransfersUseCase(TransferType.DOWNLOAD).conflate().takeWhile {
                    checkShowRating
                }.collect { (transferTotals, paused) ->
                    if (checkShowRating && !paused && transferTotals.totalFileTransfers > 0) {
                        val currentDownloadSpeed = getCurrentDownloadSpeedUseCase()
                        RatingHandlerImpl().showRatingBaseOnSpeedAndSize(
                            size = transferTotals.totalFileTransfers.toLong(),
                            speed = currentDownloadSpeed.toLong(),
                            listener = object : OnCompleteListener {
                                override fun onComplete() {
                                    checkShowRating = false
                                }


                                override fun onConditionsUnmet() {
                                    checkShowRating = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private var monitorDownloadFinishJob: Job? = null

    /**
     * Monitor finish to send the corresponding event (will display a "Download Finished" snackbar or similar)
     */
    private fun monitorDownloadFinish() {
        monitorDownloadFinishJob?.cancel()
        monitorDownloadFinishJob = viewModelScope.launch {
            monitorActiveTransferFinishedUseCase(TransferType.DOWNLOAD).collect { totalNodes ->
                when (_uiState.value.transferTriggerEvent) {
                    is TransferTriggerEvent.StartDownloadForOffline -> {
                        StartDownloadTransferEvent.Message.FinishOffline
                    }

                    is TransferTriggerEvent.StartDownloadNode -> {
                        StartDownloadTransferEvent.MessagePlural.FinishDownloading(
                            totalNodes
                        )
                    }

                    else -> null
                }?.let { finishEvent ->
                    _uiState.updateEventAndClearProgress(finishEvent)
                }
            }
        }
    }

    private fun MutableStateFlow<StartDownloadTransferViewState>.updateEventAndClearProgress(
        event: StartDownloadTransferEvent?,
    ) =
        this.update {
            it.copy(
                oneOffViewEvent = event?.let { triggered(event) } ?: consumed(),
                jobInProgressState = null,
            )
        }

    private fun String.ensureSuffix(suffix: String) =
        if (this.endsWith(suffix)) this else this.plus(suffix)
}
