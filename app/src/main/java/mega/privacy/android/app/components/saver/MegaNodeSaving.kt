package mega.privacy.android.app.components.saver

import android.content.Context
import android.content.Intent
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.TypeParceler
import mega.privacy.android.app.*
import mega.privacy.android.app.DownloadService.*
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.FileUtil.isFileDownloadedLatest
import mega.privacy.android.app.utils.MegaNodeUtil.getDlList
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString
import nz.mega.sdk.MegaNode
import java.io.File
import java.util.*

@Parcelize
@TypeParceler<MegaNode, MegaNodeParceler>()
class MegaNodeSaving(
    private val totalSize: Long,
    private val highPriority: Boolean,
    private val isFolderLink: Boolean,
    private val nodes: List<MegaNode>,
    private val fromMediaViewer: Boolean,
    private val needSerialize: Boolean,
    private val isVoiceClip: Boolean = false
) : Saving() {

    override fun totalSize() = totalSize

    override fun hasUnsupportedFile(context: Context): Boolean {
        for (node in nodes) {
            if (node.isFolder) {
                continue
            }

            unsupportedFileName = node.name
            val checkIntent = Intent(Intent.ACTION_GET_CONTENT)
            checkIntent.type = MimeTypeList.typeForName(node.name).type
            try {
                val intentAvailable = MegaApiUtils.isIntentAvailable(context, checkIntent)
                if (!intentAvailable) {
                    return true
                }
            } catch (e: Exception) {
                LogUtil.logWarning("isIntentAvailable error", e)
                return true
            }
        }

        return false
    }

    override fun fromMediaViewer() = fromMediaViewer

    override fun doDownload(
        parentPath: String,
        externalSDCard: Boolean,
        sdCardOperator: SDCardOperator?,
        snackbarShower: SnackbarShower,
    ): AutoPlayInfo {
        val app = MegaApplication.getInstance()
        val megaApi = if (isFolderLink) app.megaApiFolder else app.megaApi
        val dbHandler = DatabaseHandler.getDbHandler(app)

        var numberOfNodesAlreadyDownloaded = 0
        var numberOfNodesPending = 0
        var emptyFolders = 0

        var theOnlyLocalFilePath = ""

        for (node in nodes) {
            val dlFiles = HashMap<MegaNode, String>()
            val targets = HashMap<Long, String>()

            if (node.type == MegaNode.TYPE_FOLDER) {
                if (sdCardOperator != null && sdCardOperator.isSDCardDownload) {
                    sdCardOperator.buildFileStructure(targets, parentPath, megaApi, node)
                    getDlList(megaApi, dlFiles, node, File(sdCardOperator.downloadRoot, node.name))
                } else {
                    getDlList(megaApi, dlFiles, node, File(parentPath, node.name))
                }
            } else {
                if (sdCardOperator != null && sdCardOperator.isSDCardDownload) {
                    targets[node.handle] = parentPath
                    dlFiles[node] = sdCardOperator.downloadRoot
                } else {
                    dlFiles[node] = parentPath
                }
            }

            if (dlFiles.isEmpty()) {
                emptyFolders++
            }

            for (document in dlFiles.keys) {
                val path = dlFiles[document]
                val targetPath = targets[document.handle]
                if (TextUtil.isTextEmpty(path)) {
                    continue
                }

                val destDir = File(path!!)
                val destFile = if (destDir.isDirectory) {
                    File(
                        destDir,
                        app.megaApi.escapeFsIncompatible(
                            document.name, destDir.absolutePath + SEPARATOR
                        )
                    )
                } else {
                    destDir
                }

                if (isFileAvailable(destFile)
                    && document.size == destFile.length()
                    && isFileDownloadedLatest(destFile, document)
                ) {
                    numberOfNodesAlreadyDownloaded++

                    theOnlyLocalFilePath = destFile.absolutePath
                } else {
                    numberOfNodesPending++

                    val intent = Intent(app, DownloadService::class.java)

                    if (needSerialize) {
                        intent.putExtra(EXTRA_SERIALIZE_STRING, document.serialize())
                    } else {
                        intent.putExtra(EXTRA_HASH, document.handle)
                    }

                    if (isVoiceClip) {
                        intent.putExtra(EXTRA_OPEN_FILE, false)
                        intent.putExtra(EXTRA_TRANSFER_TYPE, APP_DATA_VOICE_CLIP)
                    }

                    if (sdCardOperator?.isSDCardDownload == true) {
                        intent.putExtra(EXTRA_PATH, path)
                        intent.putExtra(EXTRA_DOWNLOAD_TO_SDCARD, true)
                        intent.putExtra(EXTRA_TARGET_PATH, targetPath)
                        intent.putExtra(EXTRA_TARGET_URI, dbHandler.sdCardUri)
                    } else {
                        intent.putExtra(EXTRA_PATH, path)
                    }

                    intent.putExtra(EXTRA_SIZE, document.size)

                    if (highPriority) {
                        intent.putExtra(HIGH_PRIORITY_TRANSFER, true)
                    }

                    if (fromMediaViewer) {
                        intent.putExtra(EXTRA_FROM_MV, true)
                    }

                    intent.putExtra(EXTRA_FOLDER_LINK, isFolderLink)

                    app.startService(intent)
                }
            }
        }

        val message = if (numberOfNodesPending == 0 && numberOfNodesAlreadyDownloaded == 0) {
            getQuantityString(R.plurals.empty_folders, emptyFolders)
        } else if (numberOfNodesAlreadyDownloaded == 0) {
            getQuantityString(R.plurals.download_began, numberOfNodesPending, numberOfNodesPending)
        } else if (numberOfNodesPending > 0) {
            getQuantityString(
                R.plurals.file_pending_download, numberOfNodesPending, numberOfNodesPending
            )
        } else {
            getQuantityString(
                R.plurals.file_already_downloaded, numberOfNodesAlreadyDownloaded,
                numberOfNodesAlreadyDownloaded
            )
        }

        snackbarShower.showSnackbar(message)

        if (nodes.size != 1 || nodes[0].isFolder || numberOfNodesAlreadyDownloaded != 1) {
            return AutoPlayInfo.NO_AUTO_PLAY
        }

        return AutoPlayInfo(nodes[0].name, nodes[0].handle, theOnlyLocalFilePath)
    }
}
