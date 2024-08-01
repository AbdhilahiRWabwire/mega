package mega.privacy.android.app.usecase

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.UploadService
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.namecollision.data.LegacyNameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionResult
import mega.privacy.android.app.namecollision.exception.NoPendingCollisionsException
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.uploadFolder.list.data.UploadFolderResult
import mega.privacy.android.app.usecase.exception.BreakTransfersProcessingException
import mega.privacy.android.app.usecase.exception.OverDiskQuotaPaywallMegaException
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.namecollision.NodeNameCollisionResult
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import java.io.File
import javax.inject.Inject

/**
 * Use case for uploading files.
 *
 * @property transfersManagement    Required for checking transfers status.
 */
class UploadUseCase @Inject constructor(
    private val transfersManagement: TransfersManagement,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
) {
    /**
     * Uploads a file.
     *
     * @param context       Required Context for starting the service.
     * @param absolutePath  Absolute path of the file.
     * @param fileName      Name with which the file has to be uploaded.
     * @param lastModified  Last modified date of the file.
     * @param parentHandle  Handle of the MegaNode in which the file has to be uploaded.
     * @return Completable.
     */
    fun upload(
        context: Context,
        absolutePath: String,
        fileName: String,
        lastModified: Long,
        parentHandle: Long?,
    ): Completable = Completable.create { emitter ->
        if (monitorStorageStateEventUseCase.getState() == StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning()
            emitter.onError(OverDiskQuotaPaywallMegaException(""))
            return@create
        }

        if (transfersManagement.shouldBreakTransfersProcessing()) {
            emitter.onError(BreakTransfersProcessingException())
            return@create
        }

        val uploadServiceIntent = Intent(context, UploadService::class.java)
            .putExtra(UploadService.EXTRA_FILE_PATH, absolutePath)
            .putExtra(UploadService.EXTRA_NAME, fileName)
            .putExtra(UploadService.EXTRA_LAST_MODIFIED, lastModified / 1000)
            .putExtra(UploadService.EXTRA_PARENT_HASH, parentHandle)

        tryToStartForegroundService(context = context, intent = uploadServiceIntent)

        if (emitter.isDisposed) {
            return@create
        }

        emitter.onComplete()
    }

    /**
     * Attempts to start a Foreground Service when the requirements are met. If not, it would start
     * with startService
     *
     * @param context The Context object used to start the Foreground Service
     * @param intent The Intent to start the Foreground Service
     */
    private fun tryToStartForegroundService(context: Context, intent: Intent) {
        val active =
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        // Beginning with Android 12, only active apps can start Foreground Services
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || active) {
            context.startForegroundService(intent)
        }
    }

    /**
     * Uploads a file.
     *
     * @param context       Application Context required to start the service.
     * @param file          File to upload.
     * @param parentHandle  Handle of the MegaNode in which the file has to be uploaded.
     * @return Completable.
     */
    fun upload(context: Context, file: File, parentHandle: Long): Completable =
        upload(
            context = context,
            absolutePath = file.absolutePath,
            fileName = file.name,
            lastModified = file.lastModified(),
            parentHandle = parentHandle
        )

    /**
     * Uploads a file from ShareInfo.
     *
     * @param context       Application Context required to start the service.
     * @param shareInfo     The ShareInfo to upload.
     * @param renameName    A valid name if the file has to be uploaded with other name, null otherwise.
     * @param parentHandle  Handle of the MegaNode in which the file has to be uploaded.
     * @return Completable.
     */
    fun upload(
        context: Context,
        shareInfo: ShareInfo,
        renameName: String? = null,
        parentHandle: Long?,
    ): Completable =
        upload(
            context = context,
            absolutePath = shareInfo.fileAbsolutePath,
            fileName = renameName ?: shareInfo.title,
            lastModified = shareInfo.lastModified,
            parentHandle = parentHandle
        )

    /**
     * Uploads a file after resolving a name collision.
     *
     * @param context           Application Context required to start the service.
     * @param collisionResult   The result of the name collision.
     * @param rename            True if should rename the file, false otherwise.
     * @return Completable.
     */
    fun upload(
        context: Context,
        collisionResult: NameCollisionResult,
        rename: Boolean,
    ): Completable =
        upload(
            context,
            (collisionResult.nameCollision as LegacyNameCollision.Upload).absolutePath,
            if (rename) collisionResult.renameName!! else collisionResult.nameCollision.name,
            collisionResult.nameCollision.lastModified,
            collisionResult.nameCollision.parentHandle
        )

    /**
     * Uploads a file after resolving a name collision.
     *
     * @param context           Application Context required to start the service.
     * @param collisionResult   The result of the node name collision.
     * @param rename            True if should rename the file, false otherwise.
     * @return Completable.
     */
    fun upload(
        context: Context,
        collisionResult: NodeNameCollisionResult,
        rename: Boolean,
    ): Completable =
        upload(
            context,
            (collisionResult.nameCollision as FileNameCollision).path.value,
            if (rename) collisionResult.renameName!! else collisionResult.nameCollision.name,
            collisionResult.nameCollision.lastModified,
            collisionResult.nameCollision.parentHandle
        )

    /**
     * Uploads a file. Upload folder context.
     *
     * @param context       Application Context required to start the service.
     * @param uploadResult  The result of the upload folder.
     * @return Completable.
     */
    fun upload(
        context: Context,
        uploadResult: UploadFolderResult,
    ): Completable =
        upload(
            context,
            uploadResult.absolutePath,
            uploadResult.renameName ?: uploadResult.name,
            uploadResult.lastModified,
            uploadResult.parentHandle
        )

    /**
     * Uploads a list of ShareInfo.
     *
     * @param context       Application Context required to start the service.
     * @param infos         The result of the name collisions.
     * @param nameFiles     Map containing info to rename files if required, null otherwise.
     * @param parentHandle  Handle of the MegaNode in which the file has to be uploaded.
     * @return Completable.
     */
    fun uploadInfos(
        context: Context,
        infos: List<ShareInfo>,
        nameFiles: HashMap<String, String>? = null,
        parentHandle: Long,
    ): Completable =
        Completable.create { emitter ->
            if (infos.isEmpty()) {
                emitter.onError(NoPendingCollisionsException())
                return@create
            }

            for (shareInfo in infos) {
                if (emitter.isDisposed) {
                    return@create
                }

                upload(context, shareInfo, nameFiles?.get(shareInfo.getTitle()), parentHandle)
                    .blockingSubscribeBy(onError = { error -> emitter.onError(error) })
            }

            if (emitter.isDisposed) {
                return@create
            }

            emitter.onComplete()
        }

    /**
     * Uploads a list of files after resolving name collisions.
     *
     * @param context       Application Context required to start the service.
     * @param collisions    The result of the name collisions.
     * @param rename        True if should rename the files, false otherwise.
     * @return Completable.
     */
    fun upload(
        context: Context,
        collisions: List<NameCollisionResult>,
        rename: Boolean,
    ): Completable =
        Completable.create { emitter ->
            if (collisions.isEmpty()) {
                emitter.onError(NoPendingCollisionsException())
                return@create
            }

            for (collision in collisions) {
                if (emitter.isDisposed) {
                    return@create
                }

                upload(context, collision, rename).blockingSubscribeBy(
                    onError = { error -> emitter.onError(error) }
                )
            }

            if (emitter.isDisposed) {
                return@create
            }

            emitter.onComplete()
        }

    /**
     * Uploads a list of files after resolving name collisions.
     *
     * @param context       Application Context required to start the service.
     * @param collisions    The result of the node name collisions.
     * @param rename        True if should rename the files, false otherwise.
     * @return Completable.
     */
    fun uploadByCollisions(
        context: Context,
        collisions: List<NodeNameCollisionResult>,
        rename: Boolean,
    ): Completable =
        Completable.create { emitter ->
            if (collisions.isEmpty()) {
                emitter.onError(NoPendingCollisionsException())
                return@create
            }

            for (collision in collisions) {
                if (emitter.isDisposed) {
                    return@create
                }

                upload(context, collision, rename).blockingSubscribeBy(
                    onError = { error -> emitter.onError(error) }
                )
            }

            if (emitter.isDisposed) {
                return@create
            }

            emitter.onComplete()
        }
}