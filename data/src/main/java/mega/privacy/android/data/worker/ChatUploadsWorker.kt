package mega.privacy.android.data.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import mega.privacy.android.data.mapper.transfer.ChatUploadNotificationMapper
import mega.privacy.android.data.mapper.transfer.OverQuotaNotificationBuilder
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pendingMessageId
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.chat.message.AttachNodeWithPendingMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.CheckFinishedChatUploadsUseCase
import mega.privacy.android.domain.usecase.chat.message.UpdatePendingMessageUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleTransferEventUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.CorrectActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.active.GetActiveTransferTotalsUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import timber.log.Timber

/**
 * Worker that will monitor current active chat upload transfers while there are some.
 * This should be used once the uploads are actually started, it won't start any upload.
 */
@HiltWorker
class ChatUploadsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    handleTransferEventUseCase: HandleTransferEventUseCase,
    monitorOngoingActiveTransfersUseCase: MonitorOngoingActiveTransfersUseCase,
    areTransfersPausedUseCase: AreTransfersPausedUseCase,
    getActiveTransferTotalsUseCase: GetActiveTransferTotalsUseCase,
    overQuotaNotificationBuilder: OverQuotaNotificationBuilder,
    notificationManager: NotificationManagerCompat,
    areNotificationsEnabledUseCase: AreNotificationsEnabledUseCase,
    correctActiveTransfersUseCase: CorrectActiveTransfersUseCase,
    clearActiveTransfersIfFinishedUseCase: ClearActiveTransfersIfFinishedUseCase,
    private val chatUploadNotificationMapper: ChatUploadNotificationMapper,
    private val attachNodeWithPendingMessageUseCase: AttachNodeWithPendingMessageUseCase,
    private val updatePendingMessageUseCase: UpdatePendingMessageUseCase,
    private val checkFinishedChatUploadsUseCase: CheckFinishedChatUploadsUseCase,
) : AbstractTransfersWorker(
    context,
    workerParams,
    TransferType.CHAT_UPLOAD,
    ioDispatcher,
    monitorTransferEventsUseCase,
    handleTransferEventUseCase,
    monitorOngoingActiveTransfersUseCase,
    areTransfersPausedUseCase,
    getActiveTransferTotalsUseCase,
    overQuotaNotificationBuilder,
    notificationManager,
    areNotificationsEnabledUseCase,
    correctActiveTransfersUseCase,
    clearActiveTransfersIfFinishedUseCase,
) {
    override val updateNotificationId = NOTIFICATION_CHAT_UPLOAD

    override suspend fun createUpdateNotification(
        activeTransferTotals: ActiveTransferTotals,
        paused: Boolean,
    ) = chatUploadNotificationMapper(activeTransferTotals, null, paused)

    override suspend fun onStart() {
        checkFinishedChatUploadsUseCase()
    }

    override suspend fun onTransferEventReceived(event: TransferEvent) {
        event.transfer.pendingMessageId()?.let { pendingMessageId ->
            (event as? TransferEvent.TransferFinishEvent)?.let { finishEvent ->
                if (finishEvent.error == null) {
                    runCatching {
                        Timber.d("Node will be attached")
                        //once uploaded, it can be attached to the chat
                        attachNodeWithPendingMessageUseCase(
                            pendingMessageId,
                            NodeId(event.transfer.nodeHandle)
                        )
                    }.onFailure {
                        updateState(pendingMessageId, PendingMessageState.ERROR_ATTACHING)
                        Timber.e(it, "Node could not be attached")
                    }
                } else {
                    updateState(pendingMessageId, PendingMessageState.ERROR_UPLOADING)
                }
            }
        }
    }

    private suspend fun updateState(
        pendingMessageId: Long,
        state: PendingMessageState,
    ) {
        updatePendingMessageUseCase(UpdatePendingMessageStateRequest(pendingMessageId, state))
    }

    companion object {
        /**
         * Tag for enqueue the worker to work manager
         */
        const val SINGLE_CHAT_UPLOAD_TAG = "MEGA_CHAT_UPLOAD_TAG"
        private const val NOTIFICATION_CHAT_UPLOAD = 15
    }
}