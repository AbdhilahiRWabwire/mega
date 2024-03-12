package mega.privacy.android.app.presentation.imagepreview

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.usecase.GetFileUrlByImageNodeUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import java.io.File
import javax.inject.Inject

/**
 * AlbumImport Preview help class
 */
class ImagePreviewVideoLauncher @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val getFileUrlByImageNodeUseCase: GetFileUrlByImageNodeUseCase,
) {

    suspend fun launchVideoScreen(
        context: Context,
        imageNode: ImageNode,
        adapterType: Int = Constants.FROM_IMAGE_VIEWER,
    ) {
        val nodeHandle = imageNode.id.longValue
        val nodeName = imageNode.name

        val intent = Util.getMediaIntent(context, nodeName).apply {
            putExtra(Constants.INTENT_EXTRA_KEY_POSITION, 0)
            putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, nodeHandle)
            putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, nodeName)
            if (imageNode.serializedData?.contains("local") == true) { //handle zip file
                putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.VIEWER_FROM_ZIP_BROWSER)
                putExtra(Constants.INTENT_EXTRA_KEY_PATH, imageNode.fullSizePath)
                putExtra(
                    Constants.INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY,
                    imageNode.fullSizePath
                )
            } else {
                putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, adapterType)
            }
            putExtra(
                Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
                imageNode.parentId.longValue
            )
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val newIntent = updateVideoPlayerIntent(
            intent = intent,
            imageNode = imageNode,
            context = context,
        )
        context.startActivity(newIntent)
    }

    /**
     * Detect the node whether is local file
     *
     * @param handle node handle
     * @return local file path
     */
    private suspend fun isLocalFile(
        imageNode: ImageNode,
    ): String? {
        return if (imageNode.serializedData?.contains("local") == true) {//handle zip file
            imageNode.fullSizePath
        } else {
            getNodeByHandle(imageNode.id.longValue)?.let { node ->
                val localPath = FileUtil.getLocalFile(node)
                File(FileUtil.getDownloadLocation(), node.name).let { file ->
                    if (localPath != null && ((FileUtil.isFileAvailable(file) && file.length() == node.size)
                                || (node.fingerprint == getFingerprintUseCase(localPath)))
                    ) {
                        localPath
                    } else {
                        null
                    }
                }
            }
        }
    }

    private suspend fun updateIntent(
        imageNode: ImageNode,
        intent: Intent,
    ): Intent {
        if (megaApiHttpServerIsRunningUseCase() == 0) {
            megaApiHttpServerStartUseCase()
            intent.putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
        }
        getFileUrlByImageNodeUseCase(imageNode)?.let { url ->
            Uri.parse(url)?.let { uri ->
                intent.setDataAndType(uri, MimeTypeList.typeForName(imageNode.name).type)
            }
        }

        return intent
    }

    private suspend fun updateVideoPlayerIntent(
        context: Context,
        imageNode: ImageNode,
        intent: Intent,
    ): Intent {
        return isLocalFile(imageNode)?.let { localPath ->
            File(localPath).let { mediaFile ->
                kotlin.runCatching {
                    FileProvider.getUriForFile(
                        context,
                        Constants.AUTHORITY_STRING_FILE_PROVIDER,
                        mediaFile
                    )
                }.onFailure {
                    Uri.fromFile(mediaFile)
                }.map { mediaFileUri ->
                    intent.setDataAndType(
                        mediaFileUri,
                        MimeTypeList.typeForName(imageNode.name).type
                    )
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
            }
            intent
        } ?: updateIntent(
            imageNode = imageNode,
            intent = intent,
        )
    }
}