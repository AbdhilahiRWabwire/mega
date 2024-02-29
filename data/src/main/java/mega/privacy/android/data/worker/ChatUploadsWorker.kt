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
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pendingMessageId
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.chat.message.AttachNodeWithPendingMessageUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.AddOrUpdateActiveTransferUseCase
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
    addOrUpdateActiveTransferUseCase: AddOrUpdateActiveTransferUseCase,
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
) : AbstractTransfersWorker(
    context,
    workerParams,
    TransferType.CHAT_UPLOAD,
    ioDispatcher,
    monitorTransferEventsUseCase,
    addOrUpdateActiveTransferUseCase,
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

    override suspend fun onTransferEventReceived(event: TransferEvent) {
        (event as? TransferEvent.TransferFinishEvent)?.transfer?.pendingMessageId()
            ?.let { pendingMessageId ->
                runCatching {
                    Timber.d("Node will be attached")
                    //once uploaded, it can be attached to the chat
                    attachNodeWithPendingMessageUseCase(
                        pendingMessageId,
                        NodeId(event.transfer.nodeHandle)
                    )
                }.onFailure {
                    Timber.e(it, "Node could not be attached")
                }
            }
    }

    companion object {
        /**
         * Tag for enqueue the worker to work manager
         */
        const val SINGLE_CHAT_UPLOAD_TAG = "MEGA_CHAT_UPLOAD_TAG"
        private const val NOTIFICATION_CHAT_UPLOAD = 15
    }
}