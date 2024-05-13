package mega.privacy.android.app.components.saver

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StatFs
import android.text.TextUtils
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.launch
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.R
import mega.privacy.android.app.di.getDbHandler
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.PermissionRequester
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showNotEnoughSpaceSnackbar
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.main.FileStorageActivity
import mega.privacy.android.app.main.FileStorageActivity.EXTRA_PATH
import mega.privacy.android.app.main.FileStorageActivity.EXTRA_PROMPT
import mega.privacy.android.app.main.FileStorageActivity.Mode.PICK_FOLDER
import mega.privacy.android.app.main.FileStorageActivity.PICK_FOLDER_TYPE
import mega.privacy.android.app.main.FileStorageActivity.PickFolderType
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_TREE
import mega.privacy.android.app.utils.Constants.REQUEST_WRITE_STORAGE
import mega.privacy.android.app.utils.FileUtil.getDownloadLocation
import mega.privacy.android.app.utils.FileUtil.getDownloadLocationForPreviewingFiles
import mega.privacy.android.app.utils.FileUtil.getFullPathFromTreeUri
import mega.privacy.android.app.utils.FileUtil.getTotalSize
import mega.privacy.android.app.utils.MegaNodeUtil.autoPlayNode
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFile
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import mega.privacy.android.app.utils.RxUtil.logErr
import mega.privacy.android.app.utils.SDCardOperator
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.getSizeString
import mega.privacy.android.app.utils.Util.storeDownloadLocationIfNeeded
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.domain.entity.StorageState
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.util.concurrent.Callable

/**
 * A class that encapsulate all the procedure of saving nodes into device,
 * including choose save to internal storage or external sdcard,
 * choose save path, check download size, check other apps that could open this file, etc,
 * the final step that really download the node into a file is handled in sub-classes of Saving,
 * by implementing the abstract doDownload function.
 *
 * The initiation API of save should also be added by sub-classes, because it's usually
 * related with the final download step.
 *
 * It simplifies code in app/fragment where nodes need to be saved.
 */
class NodeSaver(
    private val activityLauncher: ActivityLauncher,
    private val permissionRequester: PermissionRequester,
    private val snackbarShower: SnackbarShower,
    private val confirmDialogShower: (message: String, onConfirmed: (Boolean) -> Unit) -> Unit,
) {
    private val compositeDisposable = CompositeDisposable()

    private val app = MegaApplication.getInstance()
    private val megaApi = app.megaApi
    private val megaApiFolder = app.megaApiFolder
    private val dbHandler = getDbHandler()

    private var saving: Saving = Saving.Companion.NOTHING

    /**
     * Save an offline node into device.
     *
     * @param handle handle of the offline node to save
     * @param fromMediaViewer whether this download is from media viewer
     */
    @JvmOverloads
    fun saveOfflineNode(
        handle: Long,
        fromMediaViewer: Boolean = false,
    ) {
        val node = dbHandler.findByHandle(handle) ?: return
        saveOfflineNodes(listOf(node), fromMediaViewer)
    }

    /**
     * Save offline nodes into device.
     *
     * @param nodes the offline nodes to save
     * @param fromMediaViewer whether this download is from media viewer
     */
    @JvmOverloads
    fun saveOfflineNodes(
        nodes: List<MegaOffline>,
        fromMediaViewer: Boolean = false,
    ) {
        save(app) {
            var totalSize = 0L
            for (node in nodes) {
                totalSize += getTotalSize(getOfflineFile(app, node))
            }
            OfflineSaving(totalSize, nodes, fromMediaViewer)
        }
    }

    /**
     * Save a list of MegaNode into device.
     *
     * @param nodes nodes to save
     * @param highPriority whether this download is high priority or not
     * @param isFolderLink whether this download is a folder link
     * @param fromMediaViewer whether this download is from media viewer
     * @param needSerialize whether this download need serialize
     * @param downloadForPreview whether this download is for preview
     * @param downloadByOpenWith whether this download is triggered by open with
     */
    @JvmOverloads
    fun saveNodes(
        nodes: List<MegaNode>,
        highPriority: Boolean = false,
        isFolderLink: Boolean = false,
        fromMediaViewer: Boolean = false,
        needSerialize: Boolean = false,
        downloadForPreview: Boolean = false,
        downloadByOpenWith: Boolean = false,
    ) {
        save(app) {
            MegaNodeSaving(
                totalSize = nodesTotalSize(nodes = nodes),
                highPriority = highPriority,
                isFolderLink = isFolderLink,
                nodes = nodes,
                fromMediaViewer = fromMediaViewer,
                needSerialize = needSerialize,
                downloadForPreview = downloadForPreview,
                downloadByOpenWith = downloadByOpenWith
            )
        }
    }

    /**
     * Save an Uri into device.
     *
     * @param uri uri to save
     * @param name name of this uri
     * @param size size of this uri content
     * @param fromMediaViewer whether this download is from media viewer
     */
    @JvmOverloads
    fun saveUri(
        uri: Uri,
        name: String,
        size: Long,
        fromMediaViewer: Boolean = false,
    ) {
        save(app) {
            UriSaving(uri, name, size, fromMediaViewer)
        }
    }

    /**
     * Handle app result from [FileStorageActivity] launched by requestLocalFolder,
     * and take actions according to the state and result.
     *
     * It should be called in onActivityResult.
     *
     * @param activity      Activity required to show a confirmation dialog.
     * @param requestCode   The requestCode from onActivityResult.
     * @param resultCode    The resultCode from onActivityResult.
     * @param intent        The intent from onActivityResult.
     * @return whether NodeSaver handles this result, if this method return false,
     * fragment/app should handle the result by other code.
     */
    fun handleActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        intent: Intent?,
    ): Boolean {
        if (saving == Saving.Companion.NOTHING) {
            return false
        }

        if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == Activity.RESULT_OK) {
            Timber.d("REQUEST_CODE_SELECT_LOCAL_FOLDER")
            if (intent == null) {
                Timber.w("Intent null")
                return false
            }

            val parentPath = intent.getStringExtra(EXTRA_PATH)
            if (parentPath == null) {
                Timber.w("parentPath null")
                return false
            }

            if (dbHandler.askSetDownloadLocation && activity is BaseActivity) {
                activity.lifecycleScope.launch {
                    val credentials =
                        runCatching { activity.getAccountCredentialsUseCase() }.getOrNull()
                    if (credentials != null) {
                        activity.showConfirmationSaveInSameLocation(parentPath)
                    }
                }
            }

            Completable
                .fromCallable {
                    storeDownloadLocationIfNeeded(parentPath)
                    checkSizeBeforeDownload(getCorrectDownloadPath(parentPath), activity)
                }
                .subscribeOn(Schedulers.io())
                .subscribeBy(onError = { logErr("NodeSaver handleActivityResult") })
                .addTo(compositeDisposable)

            return true
        } else if (requestCode == REQUEST_CODE_TREE) {
            if (intent == null) {
                Timber.w("handleActivityResult REQUEST_CODE_TREE: result intent is null")

                val message = if (resultCode != Activity.RESULT_OK) {
                    activity.getString(R.string.download_requires_permission)
                } else {
                    activity.getString(R.string.no_external_SD_card_detected)
                }

                snackbarShower.showSnackbar(message)

                return false
            }

            val uri = intent.data
            if (uri == null) {
                Timber.w("handleActivityResult REQUEST_CODE_TREE: tree uri is null!")
                return false
            }

            val pickedDir = DocumentFile.fromTreeUri(app, uri)
            if (pickedDir == null || !pickedDir.canWrite()) {
                Timber.w("handleActivityResult REQUEST_CODE_TREE: pickedDir not writable")
                return false
            }

            dbHandler.sdCardUri = uri.toString()

            val parentPath = getFullPathFromTreeUri(uri, app)
            if (parentPath == null) {
                Timber.w("handleActivityResult REQUEST_CODE_TREE: parentPath is null")
                return false
            }

            Completable.fromCallable {
                checkSizeBeforeDownload(
                    getCorrectDownloadPath(parentPath),
                    activity
                )
            }
                .subscribeOn(Schedulers.io())
                .subscribeBy(onError = { logErr("NodeSaver handleActivityResult") })
                .addTo(compositeDisposable)

            return true
        }

        return false
    }

    /**
     * Handle request permission result, and take actions according to the state and result.
     *
     * It should be called in onRequestPermissionsResult (but this doesn't mean NodeSaver should be
     * owned by an app).
     *
     * @param requestCode the requestCode from onRequestPermissionsResult
     * @return whether NodeSaver handles this result, if this method return false,
     * app should handle the result by other code.
     */
    fun handleRequestPermissionsResult(requestCode: Int): Boolean {
        if (requestCode != REQUEST_WRITE_STORAGE) {
            return false
        }

        if (hasWriteExternalStoragePermission()) {
            Completable
                .fromCallable { doSave(app) }
                .subscribeOn(Schedulers.io())
                .subscribeBy(onError = { logErr("NodeSaver handleRequestPermissionsResult") })
                .addTo(compositeDisposable)
        }

        return true
    }

    /**
     * Save instance state, should be called from onSaveInstanceState of the owning
     * activity/fragment.
     *
     * @param outState outState param of onSaveInstanceState
     */
    fun saveState(outState: Bundle) {
        outState.putParcelable(STATE_KEY_SAVING, saving)
    }

    /**
     * Restore instance state, should be called from onCreate of the owning
     * activity/fragment.
     *
     * @param savedInstanceState savedInstanceState param of onCreate
     */
    fun restoreState(savedInstanceState: Bundle) {
        val oldSaving = with(savedInstanceState) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelable(STATE_KEY_SAVING, Saving::class.java)
            } else {
                @Suppress("DEPRECATION")
                getParcelable(STATE_KEY_SAVING)
            }
        } ?: return

        saving = oldSaving
    }

    /**
     * Clear all internal state and cancel all flying operation, should be called
     * in onDestroy lifecycle callback.
     */
    fun destroy() {
        compositeDisposable.dispose()
    }

    private fun save(context: Context, savingProducer: () -> Saving?) {
        Completable
            .fromCallable(Callable {
                val saving = savingProducer() ?: return@Callable
                this.saving = saving

                if (lackPermission()) {
                    return@Callable
                }

                doSave(context)
            })
            .subscribeOn(Schedulers.io())
            .subscribeBy(onError = { logErr("NodeSaver save") })
            .addTo(compositeDisposable)
    }

    private fun doSave(context: Context) {
        if (Util.askMe()) {
            requestLocalFolder(null, activityLauncher)
        } else {
            checkSizeBeforeDownload(getCorrectDownloadPath(), context)
        }
    }

    private fun requestLocalFolder(
        prompt: String?, activityLauncher: ActivityLauncher,
    ) {
        val intent = Intent(PICK_FOLDER.action)
        intent.putExtra(PICK_FOLDER_TYPE, PickFolderType.DOWNLOAD_FOLDER.folderType)
        intent.setClass(app, FileStorageActivity::class.java)

        if (prompt != null) {
            intent.putExtra(EXTRA_PROMPT, prompt)
        }

        activityLauncher.launchActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER)
    }

    private fun nodesTotalSize(nodes: List<MegaNode>): Long {
        var totalSize = 0L

        for (node in nodes) {
            totalSize += if (node.isFolder) nodesTotalSize(megaApi.getChildren(node)) else node.size
        }

        return totalSize
    }

    private fun notEnoughSpace(parentPath: String, totalSize: Long): Boolean {
        var availableFreeSpace = Long.MAX_VALUE
        try {
            val stat = StatFs(parentPath)
            availableFreeSpace = stat.availableBlocksLong * stat.blockSizeLong
        } catch (ex: Exception) {
        }
        Timber.d("availableFreeSpace: $availableFreeSpace, totalSize: $totalSize")

        if (availableFreeSpace < totalSize) {
            post { snackbarShower.showNotEnoughSpaceSnackbar() }
            Timber.w("Not enough space")
            return true
        }

        return false
    }

    private fun checkSizeBeforeDownload(parentPath: String, context: Context) {
        if (notEnoughSpace(parentPath, saving.totalSize())) {
            return
        }

        if (TextUtils.equals(dbHandler.attributes?.askSizeDownload, false.toString())
            || saving.totalSize() < CONFIRM_SIZE_MIN_BYTES
        ) {
            checkInstalledAppBeforeDownload(parentPath, context)
            return
        }

        showConfirmationDialog(
            context.getString(R.string.alert_larger_file, getSizeString(saving.totalSize(), app))
        ) { notShowAgain ->
            if (notShowAgain) {
                Completable.fromCallable { dbHandler.setAttrAskSizeDownload(false.toString()) }
                    .subscribeOn(Schedulers.io())
                    .subscribeBy(onError = { logErr("NodeSaver checkSizeBeforeDownload") })
                    .addTo(compositeDisposable)
            }

            checkInstalledAppBeforeDownload(parentPath, context)
        }
    }

    private fun checkInstalledAppBeforeDownload(parentPath: String, context: Context) {
        if (TextUtils.equals(dbHandler.attributes?.askNoAppDownload, false.toString())) {
            download(getCorrectDownloadPath(parentPath), context)
            return
        }

        if (!saving.hasUnsupportedFile(app)) {
            download(getCorrectDownloadPath(parentPath), context)
            return
        }

        showConfirmationDialog(
            context.getString(R.string.alert_no_app, saving.unsupportedFileName)
        ) { notShowAgain ->
            if (notShowAgain) {
                Completable.fromCallable { dbHandler.setAttrAskNoAppDownload(false.toString()) }
                    .subscribeOn(Schedulers.io())
                    .subscribeBy(onError = { logErr("NodeSaver checkInstalledAppBeforeDownload") })
                    .addTo(compositeDisposable)
            }
            download(getCorrectDownloadPath(parentPath), context)
        }
    }

    /**
     * Returns correct download path based on tap
     *
     * @return download path string
     */
    private fun getCorrectDownloadPath(parentPath: String? = null): String {
        return if (saving.isDownloadForPreview()) {
            getDownloadLocationForPreviewingFiles().absolutePath
        } else {
            parentPath ?: getDownloadLocation()
        }
    }

    private fun download(parentPath: String, context: Context) {
        Completable
            .fromCallable {
                checkParentPathAndDownload(parentPath, context)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onError = { logErr("NodeSaver download") })
            .addTo(compositeDisposable)
    }

    private fun checkParentPathAndDownload(parentPath: String, context: Context) {
        if (getStorageState() == StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning()
            return
        }

        val sdCardOperator = SDCardOperator.initSDCardOperator(app, parentPath)
        if (sdCardOperator == null) {
            requestLocalFolder(
                context.getString(R.string.no_external_SD_card_detected),
                activityLauncher
            )
            return
        }

        val autoPlayInfo = saving.doDownload(
            megaApi, megaApiFolder, parentPath, SDCardOperator.isSDCardPath(parentPath),
            sdCardOperator, snackbarShower
        )

        if (!autoPlayInfo.couldAutoPlay || dbHandler.autoPlayEnabled != true.toString()) {
            return
        }

        if (saving.fromMediaViewer()) {
            snackbarShower.showSnackbar(context.getString(R.string.general_already_downloaded))
        } else {
            autoPlayNode(app, autoPlayInfo, activityLauncher, snackbarShower)
        }
    }

    private fun showConfirmationDialog(message: String, onConfirmed: (Boolean) -> Unit) {
        post { confirmDialogShower(message, onConfirmed) }
    }

    private fun hasWriteExternalStoragePermission(): Boolean =
        hasPermissions(app, permission.WRITE_EXTERNAL_STORAGE)

    private fun lackPermission(): Boolean {
        if (!hasWriteExternalStoragePermission()) {
            permissionRequester.askPermissions(
                arrayOf(permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_STORAGE
            )
            return true
        }
        return false
    }

    companion object {
        const val CONFIRM_SIZE_MIN_BYTES = 100 * 1024 * 1024L

        private const val STATE_KEY_SAVING = "saving"
    }
}
