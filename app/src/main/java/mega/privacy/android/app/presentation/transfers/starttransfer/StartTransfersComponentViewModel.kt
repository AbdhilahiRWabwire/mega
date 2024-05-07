package mega.privacy.android.app.presentation.transfers.starttransfer

import android.net.Uri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.middlelayer.iar.OnCompleteListener
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.transfers.TransfersConstants
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferEvent
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferJobInProgress
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferViewState
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.SetStorageDownloadAskAlwaysUseCase
import mega.privacy.android.domain.usecase.SetStorageDownloadLocationUseCase
import mega.privacy.android.domain.usecase.chat.message.SendChatAttachmentsUseCase
import mega.privacy.android.domain.usecase.file.TotalFileSizeOfNodesUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.GetFilePreviewDownloadPathUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflinePathForNodeUseCase
import mega.privacy.android.domain.usecase.setting.IsAskBeforeLargeDownloadsSettingUseCase
import mega.privacy.android.domain.usecase.setting.SetAskBeforeLargeDownloadsSettingUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorActiveTransferFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUntilFinishedUseCase
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
 * View model to handle start transfers component
 */
@HiltViewModel
internal class StartTransfersComponentViewModel @Inject constructor(
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
    private val monitorOngoingActiveTransfersUntilFinishedUseCase: MonitorOngoingActiveTransfersUntilFinishedUseCase,
    private val getCurrentDownloadSpeedUseCase: GetCurrentDownloadSpeedUseCase,
    private val shouldAskDownloadDestinationUseCase: ShouldAskDownloadDestinationUseCase,
    private val shouldPromptToSaveDestinationUseCase: ShouldPromptToSaveDestinationUseCase,
    private val saveDoNotPromptToSaveDestinationUseCase: SaveDoNotPromptToSaveDestinationUseCase,
    private val setStorageDownloadAskAlwaysUseCase: SetStorageDownloadAskAlwaysUseCase,
    private val setStorageDownloadLocationUseCase: SetStorageDownloadLocationUseCase,
    private val monitorActiveTransferFinishedUseCase: MonitorActiveTransferFinishedUseCase,
    private val sendChatAttachmentsUseCase: SendChatAttachmentsUseCase,
) : ViewModel(), DefaultLifecycleObserver {

    private var currentInProgressJob: Job? = null

    private val _uiState = MutableStateFlow(StartTransferViewState())

    /**
     * the state of the view
     */
    internal val uiState = _uiState.asStateFlow()

    init {
        checkRating()
        monitorDownloadFinish()
    }

    /**
     * It starts the triggered transfer, asking for confirmation in case of large transfers if corresponds
     * @param transferTriggerEvent the event that triggered this transfer
     */
    fun startTransfer(
        transferTriggerEvent: TransferTriggerEvent,
    ) {
        viewModelScope.launch {
            when (transferTriggerEvent) {
                is TransferTriggerEvent.DownloadTriggerEvent -> {
                    if (checkAndHandleDeviceIsNotConnected()) {
                        return@launch
                    }
                    if (transferTriggerEvent.nodes.isEmpty()) {
                        Timber.e("Node in $transferTriggerEvent must exist")
                        _uiState.updateEventAndClearProgress(StartTransferEvent.Message.TransferCancelled)
                    } else if (!checkAndHandleNeedConfirmationForLargeDownload(transferTriggerEvent)) {
                        startDownloadWithoutConfirmation(transferTriggerEvent)
                    }
                }

                is TransferTriggerEvent.StartChatUpload -> {
                    startChatUploads(
                        chatId = transferTriggerEvent.chatId,
                        uris = transferTriggerEvent.uris,
                        isVoiceClip = transferTriggerEvent.isVoiceClip
                    )
                }
            }
        }
    }

    /**
     * It starts downloading the related nodes, without asking confirmation for large transfers (because it's already asked)
     * @param transferTriggerEvent the event that triggered this download
     * @param saveDoNotAskAgainForLargeTransfers if true, it will save in settings to don't ask again for confirmation on large files
     */
    fun startDownloadWithoutConfirmation(
        transferTriggerEvent: TransferTriggerEvent.DownloadTriggerEvent,
        saveDoNotAskAgainForLargeTransfers: Boolean = false,
    ) {
        if (saveDoNotAskAgainForLargeTransfers) {
            viewModelScope.launch {
                runCatching { setAskBeforeLargeDownloadsSettingUseCase(askForConfirmation = false) }
                    .onFailure { Timber.e(it) }
            }
        }
        val node = transferTriggerEvent.nodes.firstOrNull()
        if (node == null) {
            Timber.e("Node in $transferTriggerEvent must exist")
            _uiState.updateEventAndClearProgress(StartTransferEvent.Message.TransferCancelled)
        } else {
            lastTriggerEvent = transferTriggerEvent
            when (transferTriggerEvent) {
                is TransferTriggerEvent.StartDownloadForOffline -> {
                    startDownloadForOffline(transferTriggerEvent)
                }

                is TransferTriggerEvent.StartDownloadNode -> {
                    viewModelScope.launch {
                        if (runCatching { shouldAskDownloadDestinationUseCase() }.getOrDefault(false)) {
                            _uiState.updateEventAndClearProgress(
                                StartTransferEvent.AskDestination(
                                    transferTriggerEvent
                                )
                            )
                        } else {
                            runCatching { getOrCreateStorageDownloadLocationUseCase() }
                                .onFailure { Timber.e(it) }
                                .getOrNull()?.let { location ->
                                    startDownloadNodes(
                                        transferTriggerEvent,
                                        location
                                    )
                                }
                        }
                    }
                }

                is TransferTriggerEvent.StartDownloadForPreview -> {
                    startDownloadNodeForPreview(transferTriggerEvent)
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
        Timber.d("Selected destination $destinationUri")
        viewModelScope.launch {
            startDownloadNodes(startDownloadNode, destinationUri.toString())
            if (runCatching { shouldPromptToSaveDestinationUseCase() }.getOrDefault(false)) {
                _uiState.update {
                    it.copy(promptSaveDestination = triggered(destinationUri.toString()))
                }
            }
        }
    }

    /**
     * It starts downloading the node for preview with the appropriate use case
     * @param event the [TransferTriggerEvent.StartDownloadForPreview] event that starts this download
     */
    private fun startDownloadNodeForPreview(event: TransferTriggerEvent.StartDownloadForPreview) {
        if (event.node == null) {
            return
        }
        currentInProgressJob = viewModelScope.launch {
            startDownloadNodes(
                nodes = listOf(event.node),
                isHighPriority = true,
                getUri = {
                    runCatching { getFilePreviewDownloadPathUseCase() }
                        .onFailure { Timber.e(it) }
                        .getOrNull()
                },
                transferTriggerEvent = event
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
        val nodes = startDownloadNode.nodes
        if (nodes.isEmpty()) return
        currentInProgressJob = viewModelScope.launch {
            startDownloadNodes(
                nodes = nodes,
                isHighPriority = startDownloadNode.isHighPriority,
                getUri = {
                    destination?.ensureSuffix(File.separator)
                },
                transferTriggerEvent = startDownloadNode
            )
        }
    }

    /**
     * It starts downloading the node for offline with the appropriate use case
     * @param event the [TransferTriggerEvent.StartDownloadForOffline] event that starts this download
     */
    private fun startDownloadForOffline(event: TransferTriggerEvent.StartDownloadForOffline) {
        if (event.node == null) {
            return
        }
        currentInProgressJob = viewModelScope.launch {
            startDownloadNodes(
                nodes = listOf(event.node),
                event.isHighPriority,
                getUri = {
                    runCatching { getOfflinePathForNodeUseCase(event.node) }
                        .onFailure { Timber.e(it) }
                        .getOrNull()
                },
                transferTriggerEvent = event,
            )
        }
    }

    /**
     * common logic to start downloading nodes, either for offline or ordinary download
     */
    private suspend fun startDownloadNodes(
        nodes: List<TypedNode>,
        isHighPriority: Boolean,
        getUri: suspend () -> String?,
        transferTriggerEvent: TransferTriggerEvent,
    ) {
        monitorDownloadFinishJob?.cancel()
        runCatching { clearActiveTransfersIfFinishedUseCase(TransferType.DOWNLOAD) }
            .onFailure { Timber.e(it) }
        monitorDownloadFinish()
        _uiState.update {
            it.copy(jobInProgressState = StartTransferJobInProgress.ScanningTransfers)
        }
        var lastError: Throwable? = null
        var startMessageShown = false
        val terminalEvent = runCatching {
            getUri().also {
                if (it.isNullOrBlank()) {
                    throw NullPointerException("path not found!")
                }
            }
        }.onFailure { lastError = it }
            .getOrNull()?.let { uri ->
                startDownloadsWithWorkerUseCase(
                    destinationPathOrUri = uri,
                    nodes = nodes,
                    isHighPriority = isHighPriority,
                ).onEach { event ->
                    val singleTransferEvent = (event as? MultiTransferEvent.SingleTransferEvent)
                    // clear scanning transfers state
                    if (_uiState.value.jobInProgressState == StartTransferJobInProgress.ScanningTransfers &&
                        singleTransferEvent?.scanningFinished == true
                    ) {
                        _uiState.update {
                            it.copy(jobInProgressState = null)
                        }
                    }
                    //show start message as soon as an event with all transfers updated is received
                    if (!startMessageShown && singleTransferEvent?.allTransfersUpdated == true) {
                        startMessageShown = true
                        updateWithFinishProcessing(
                            singleTransferEvent,
                            transferTriggerEvent,
                            nodes.size,
                        )
                    }
                }.catch {
                    lastError = it
                    Timber.e(it)
                }.onCompletion {
                    if (it is CancellationException) {
                        _uiState.updateEventAndClearProgress(StartTransferEvent.Message.TransferCancelled)
                    }
                }.last()
            }
        checkRating()
        when {
            terminalEvent == MultiTransferEvent.InsufficientSpace ->
                _uiState.updateEventAndClearProgress(StartTransferEvent.Message.NotSufficientSpace)

            !startMessageShown -> updateWithFinishProcessing(
                terminalEvent as? MultiTransferEvent.SingleTransferEvent,
                transferTriggerEvent,
                nodes.size,
                lastError?.takeIf { terminalEvent == null },
            )
        }
    }

    private fun updateWithFinishProcessing(
        event: MultiTransferEvent.SingleTransferEvent?,
        transferTriggerEvent: TransferTriggerEvent,
        totalNodes: Int,
        error: Throwable? = null,
    ) {
        _uiState.updateEventAndClearProgress(
            StartTransferEvent.FinishProcessing(
                exception = error,
                totalNodes = totalNodes,
                totalFiles = event?.startedFiles ?: 0,
                totalAlreadyDownloaded = event?.alreadyTransferred ?: 0,
                triggerEvent = transferTriggerEvent,
            )
        )
    }

    private suspend fun startChatUploads(
        chatId: Long,
        uris: List<Uri>,
        isVoiceClip: Boolean = false,
    ) {
        runCatching { clearActiveTransfersIfFinishedUseCase(TransferType.CHAT_UPLOAD) }
            .onFailure { Timber.e(it) }
        sendChatAttachmentsUseCase(
            uris.map { it.toString() }.associateWith { null }, isVoiceClip, chatId
        ).catch { Timber.e(it) }.collect {
            when (it) {
                is MultiTransferEvent.TransferNotStarted<*> -> {
                    Timber.e(it.exception, "Error starting chat upload")
                    StartTransferEvent.Message.TransferCancelled
                }

                MultiTransferEvent.InsufficientSpace -> StartTransferEvent.Message.NotSufficientSpace
                is MultiTransferEvent.SingleTransferEvent -> null
            }?.let {
                _uiState.updateEventAndClearProgress(it)
            }
        }
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
        if (runCatching { isConnectedToInternetUseCase() }.getOrDefault(true)) {
            false
        } else {
            _uiState.updateEventAndClearProgress(StartTransferEvent.NotConnected)
            true
        }

    /**
     * Checks if confirmation dialog for large download should be shown and updates uiState if so
     *
     * @return true if the state has been handled to ask for confirmation, so no extra action should be done
     */
    private suspend fun checkAndHandleNeedConfirmationForLargeDownload(transferTriggerEvent: TransferTriggerEvent.DownloadTriggerEvent): Boolean {
        if (runCatching { isAskBeforeLargeDownloadsSettingUseCase() }.getOrDefault(false)) {
            val size = runCatching { totalFileSizeOfNodesUseCase(transferTriggerEvent.nodes) }
                .getOrDefault(0L)
            if (size > TransfersConstants.CONFIRM_SIZE_MIN_BYTES) {
                _uiState.updateEventAndClearProgress(
                    StartTransferEvent.ConfirmLargeDownload(
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
                monitorOngoingActiveTransfersUntilFinishedUseCase(TransferType.DOWNLOAD)
                    .conflate()
                    .takeWhile {
                        checkShowRating
                    }.catch {
                        Timber.e(it)
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
            monitorActiveTransferFinishedUseCase(TransferType.DOWNLOAD)
                .catch { Timber.e(it) }
                .collect { totalNodes ->
                    if (active) {
                        when (lastTriggerEvent) {
                            is TransferTriggerEvent.StartDownloadForOffline -> {
                                StartTransferEvent.Message.FinishOffline
                            }

                            is TransferTriggerEvent.StartDownloadNode -> {
                                StartTransferEvent.MessagePlural.FinishDownloading(
                                    totalNodes
                                )
                            }

                            else -> null
                        }?.let { finishEvent ->
                            _uiState.updateEventAndClearProgress(finishEvent)
                        }
                        lastTriggerEvent = null
                    }
                }
        }
    }

    private fun MutableStateFlow<StartTransferViewState>.updateEventAndClearProgress(
        event: StartTransferEvent?,
    ) =
        this.update {
            it.copy(
                oneOffViewEvent = event?.let { triggered(event) } ?: consumed(),
                jobInProgressState = null,
            )
        }

    private fun String.ensureSuffix(suffix: String) =
        if (this.endsWith(suffix)) this else this.plus(suffix)

    private var active = false
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        active = true
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        active = false
    }

    companion object {
        /**
         * The last trigger event that started the download, we need to keep it to know what to do when the download finishes even in other screens
         */
        private var lastTriggerEvent: TransferTriggerEvent.DownloadTriggerEvent? = null
    }
}
