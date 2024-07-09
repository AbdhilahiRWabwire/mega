package mega.privacy.android.data.mapper.transfer

import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferStage
import nz.mega.sdk.MegaTransfer
import javax.inject.Inject

/**
 * [MegaTransfer] to [Transfer] mapper
 */
internal class TransferMapper @Inject constructor(
    private val transferAppDataMapper: TransferAppDataMapper,
    private val transferTypeMapper: TransferTypeMapper,
    private val transferStateMapper: TransferStateMapper,
) {
    /**
     * Invoke
     *
     * @param transfer [MegaTransfer]
     */
    operator fun invoke(transfer: MegaTransfer): Transfer {
        val appData = transferAppDataMapper(transfer.appData.orEmpty())
        return Transfer(
            transferType = transferTypeMapper(transfer.type, appData),
            transferredBytes = transfer.transferredBytes,
            totalBytes = transfer.totalBytes,
            localPath = transfer.path.orEmpty(),
            parentPath = transfer.parentPath.orEmpty(),
            nodeHandle = transfer.nodeHandle,
            parentHandle = transfer.parentHandle,
            fileName = transfer.fileName.orEmpty(),
            stage = transfer.stage.toTransferStage(),
            tag = transfer.tag,
            folderTransferTag = transfer.folderTransferTag.takeIf { it > 0 },
            speed = transfer.speed,
            isSyncTransfer = transfer.isSyncTransfer,
            isBackupTransfer = transfer.isBackupTransfer,
            isForeignOverQuota = transfer.isForeignOverquota,
            isStreamingTransfer = transfer.isStreamingTransfer,
            isFinished = transfer.isFinished,
            isFolderTransfer = transfer.isFolderTransfer,
            appData = appData,
            state = transferStateMapper(transfer.state),
            priority = transfer.priority,
            notificationNumber = transfer.notificationNumber,
        )
    }

    private fun Long.toTransferStage(): TransferStage = when (this) {
        MegaTransfer.STAGE_SCAN.toLong() -> TransferStage.STAGE_SCANNING
        MegaTransfer.STAGE_CREATE_TREE.toLong() -> TransferStage.STAGE_CREATING_TREE
        MegaTransfer.STAGE_TRANSFERRING_FILES.toLong() -> TransferStage.STAGE_TRANSFERRING_FILES
        else -> TransferStage.STAGE_NONE
    }
}
