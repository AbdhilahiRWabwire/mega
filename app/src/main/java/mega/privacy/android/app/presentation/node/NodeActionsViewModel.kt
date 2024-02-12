package mega.privacy.android.app.presentation.node

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.chat.mapper.ChatRequestMessageMapper
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.app.presentation.node.model.NodeActionState
import mega.privacy.android.app.presentation.node.model.mapper.NodeAccessPermissionIconMapper
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent
import mega.privacy.android.app.presentation.versions.mapper.VersionHistoryRemoveMessageMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.backup.BackupNodeType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.account.SetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.SetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.chat.AttachMultipleNodesUseCase
import mega.privacy.android.domain.usecase.filenode.DeleteNodeVersionsUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.CopyNodesUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesUseCase
import mega.privacy.android.domain.usecase.node.backup.CheckBackupNodeTypeByHandleUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Node actions view model
 *
 * @property checkNodesNameCollisionUseCase
 * @property moveNodesUseCase
 * @property copyNodesUseCase
 * @property setMoveLatestTargetPathUseCase
 * @property setCopyLatestTargetPathUseCase
 * @property deleteNodeVersionsUseCase
 * @property snackBarHandler
 * @property moveRequestMessageMapper
 * @property versionHistoryRemoveMessageMapper
 * @property nodeAccessPermissionIconMapper
 * @property checkBackupNodeTypeByHandleUseCase
 * @property attachMultipleNodesUseCase
 * @property chatRequestMessageMapper
 * @property applicationScope
 */
@HiltViewModel
class NodeActionsViewModel @Inject constructor(
    private val checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase,
    private val moveNodesUseCase: MoveNodesUseCase,
    private val copyNodesUseCase: CopyNodesUseCase,
    private val setMoveLatestTargetPathUseCase: SetMoveLatestTargetPathUseCase,
    private val setCopyLatestTargetPathUseCase: SetCopyLatestTargetPathUseCase,
    private val deleteNodeVersionsUseCase: DeleteNodeVersionsUseCase,
    private val snackBarHandler: SnackBarHandler,
    private val moveRequestMessageMapper: MoveRequestMessageMapper,
    private val versionHistoryRemoveMessageMapper: VersionHistoryRemoveMessageMapper,
    private val nodeAccessPermissionIconMapper: NodeAccessPermissionIconMapper,
    private val checkBackupNodeTypeByHandleUseCase: CheckBackupNodeTypeByHandleUseCase,
    private val attachMultipleNodesUseCase: AttachMultipleNodesUseCase,
    private val chatRequestMessageMapper: ChatRequestMessageMapper,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val _state = MutableStateFlow(NodeActionState())

    /**
     * public UI State
     */
    val state: StateFlow<NodeActionState> = _state

    /**
     * Check move nodes name collision
     *
     * @param nodes
     * @param targetNode
     */
    fun checkNodesNameCollision(
        nodes: List<Long>,
        targetNode: Long,
        type: NodeNameCollisionType,
    ) {
        viewModelScope.launch {
            runCatching {
                checkNodesNameCollisionUseCase(
                    nodes.associateWith { targetNode },
                    type
                )
            }.onSuccess { result ->
                _state.update { it.copy(nodeNameCollisionResult = triggered(result)) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Move nodes
     *
     * @param nodes
     */
    fun moveNodes(nodes: Map<Long, Long>) {
        applicationScope.launch {
            runCatching {
                moveNodesUseCase(nodes)
            }.onSuccess {
                setMoveTargetPath(nodes.values.first())
                snackBarHandler.postSnackbarMessage(moveRequestMessageMapper(it))
            }.onFailure {
                manageCopyMoveError(it)
                Timber.e(it)
            }
        }
    }

    /**
     * Copy nodes
     *
     * @param nodes
     */
    fun copyNodes(nodes: Map<Long, Long>) {
        applicationScope.launch {
            runCatching {
                copyNodesUseCase(nodes)
            }.onSuccess {
                setCopyTargetPath(nodes.values.first())
                snackBarHandler.postSnackbarMessage(moveRequestMessageMapper(it))
            }.onFailure {
                manageCopyMoveError(it)
                Timber.e(it)
            }
        }
    }

    private fun manageCopyMoveError(error: Throwable?) = when (error) {
        is ForeignNodeException -> _state.update { it.copy(showForeignNodeDialog = triggered) }
        is QuotaExceededMegaException -> _state.update {
            it.copy(showQuotaDialog = triggered(true))
        }

        is NotEnoughQuotaMegaException -> _state.update {
            it.copy(showQuotaDialog = triggered(false))
        }

        else -> Timber.e("Error copying/moving nodes $error")
    }

    /**
     * Set last used path of move as target path for next move
     */
    private fun setMoveTargetPath(path: Long) {
        viewModelScope.launch {
            runCatching { setMoveLatestTargetPathUseCase(path) }
                .onFailure { Timber.e(it) }
        }
    }

    /**
     * Set last used path of copy as target path for next copy
     */
    private fun setCopyTargetPath(path: Long) {
        viewModelScope.launch {
            runCatching { setCopyLatestTargetPathUseCase(path) }
                .onFailure { Timber.e(it) }
        }
    }

    /**
     * Mark handle node name collision result
     */
    fun markHandleNodeNameCollisionResult() {
        _state.update { it.copy(nodeNameCollisionResult = consumed()) }
    }

    /**
     * Delete version history of selected node
     */
    fun deleteVersionHistory(it: Long) = applicationScope.launch {
        val result = runCatching {
            deleteNodeVersionsUseCase(NodeId(it))
        }
        versionHistoryRemoveMessageMapper(result.exceptionOrNull()).let {
            snackBarHandler.postSnackbarMessage(it)
        }
    }

    /**
     * Mark foreign node dialog shown
     */
    fun markForeignNodeDialogShown() {
        _state.update { it.copy(showForeignNodeDialog = consumed) }
    }

    /**
     * Mark quota dialog shown
     */
    fun markQuotaDialogShown() {
        _state.update { it.copy(showQuotaDialog = consumed()) }
    }

    /**
     * Get access permission icon
     * Access permission icon is only shown for incoming shares
     *
     * @return icon
     */
    private fun getAccessPermissionIcon(accessPermission: AccessPermission, node: TypedNode): Int? =
        nodeAccessPermissionIconMapper(accessPermission).takeIf { node.isIncomingShare }

    /**
     * Contact selected for folder share
     */
    fun contactSelectedForShareFolder(contactsData: List<String>) {
        state.value.selectedNodes.firstOrNull()?.let { node ->
            viewModelScope.launch {
                val isFromBackUps = checkBackupNodeTypeByHandleUseCase(node)
                _state.update {
                    it.copy(
                        contactsData = triggered(
                            Pair(
                                contactsData,
                                isFromBackUps != BackupNodeType.NonBackupNode
                            )
                        )
                    )
                }
            }
        }
    }

    /**
     * attach node to chat
     *
     * @param nodeHandles [LongArray] on which node is attached
     * @param chatIds [LongArray] chat ids
     */
    fun attachNodeToChats(
        nodeHandles: LongArray?,
        chatIds: LongArray?,
    ) {
        if (nodeHandles != null && chatIds != null) {
            val nodeIds = nodeHandles.map {
                NodeId(it)
            }
            viewModelScope.launch {
                val attachNodeRequest =
                    attachMultipleNodesUseCase(
                        nodeIds = nodeIds,
                        chatIds = chatIds
                    )
                val message = chatRequestMessageMapper(attachNodeRequest)
                message?.let {
                    snackBarHandler.postSnackbarMessage(it)
                }
            }
        }
    }

    /**
     * Contact selected for folder share
     */
    fun markShareFolderAccessDialogShown() {
        _state.update {
            it.copy(contactsData = consumed())
        }
    }

    /**
     * Download node
     * Triggers TransferTriggerEvent.StartDownloadNode with parameter [TypedNode]
     */
    fun downloadNode() {
        state.value.selectedNodes.let { nodes ->
            _state.update {
                it.copy(downloadEvent = triggered(TransferTriggerEvent.StartDownloadNode(nodes)))
            }
        }
    }

    /**
     * Download node for offline
     * Triggers TransferTriggerEvent.StartDownloadNode with parameter [TypedNode]
     */
    fun downloadNodeForOffline() {
        state.value.selectedNodes.firstOrNull().let { node ->
            _state.update {
                it.copy(downloadEvent = triggered(TransferTriggerEvent.StartDownloadForOffline(node)))
            }
        }
    }

    /**
     * Download node for preview
     * Triggers TransferTriggerEvent.StartDownloadNode with parameter [TypedNode]
     */
    fun downloadNodeForPreview() {
        state.value.selectedNodes.firstOrNull()?.let { node ->
            _state.update {
                it.copy(downloadEvent = triggered(TransferTriggerEvent.StartDownloadForPreview(node)))
            }
        }
    }

    /**
     * Mark download event consumed
     */
    fun markDownloadEventConsumed() {
        _state.update {
            it.copy(downloadEvent = consumed())
        }
    }

    /**
     * Update selected nodes
     * @param selectedNodes
     */
    fun updateSelectedNodes(selectedNodes: List<TypedNode>) {
        _state.update {
            it.copy(selectedNodes = selectedNodes)
        }
    }

}