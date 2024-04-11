package mega.privacy.android.app.presentation.node.action

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.mediaplayer.AudioPlayerActivity
import mega.privacy.android.app.mediaplayer.VideoPlayerActivity
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.CloudDriveImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.RubbishBinImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.node.FileNodeContent
import mega.privacy.android.app.presentation.node.NodeActionsViewModel
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.textEditor.TextEditorActivity
import mega.privacy.android.app.textEditor.TextEditorViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.zippreview.ui.ZipBrowserActivity
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.TypedFileNode
import timber.log.Timber
import java.io.File

/**
 * Handle node action click
 *
 * @param typedFileNode [TypedFileNode]
 * @param nodeSourceType from where item click is performed
 * @param nodeActionsViewModel [NodeActionsViewModel]
 * @param sortOrder [SortOrder]
 * @param snackBarHostState [SnackbarHostState]
 * @param onActionHandled callback after file clicked
 */
@Composable
fun HandleNodeAction(
    typedFileNode: TypedFileNode,
    nodeSourceType: Int? = null,
    nodeActionsViewModel: NodeActionsViewModel = hiltViewModel(),
    sortOrder: SortOrder = SortOrder.ORDER_NONE,
    onActionHandled: () -> Unit,
    snackBarHostState: SnackbarHostState,
) {
    val context = LocalContext.current

    LaunchedEffect(key1 = typedFileNode) {
        runCatching {
            nodeActionsViewModel.handleFileNodeClicked(typedFileNode)
        }.onSuccess { content ->
            when (content) {
                is FileNodeContent.Pdf -> openPdfActivity(
                    context = context,
                    type = nodeSourceType,
                    content = content.uri,
                    currentFileNode = typedFileNode,
                    nodeActionsViewModel = nodeActionsViewModel
                )

                is FileNodeContent.ImageForNode -> {
                    openImageViewerActivity(
                        context = context,
                        isImagePreview = content.isImagePreview,
                        currentFileNode = typedFileNode,
                        nodeSourceType = nodeSourceType,
                        sortOrder = sortOrder
                    )
                }

                is FileNodeContent.TextContent -> openTextEditorActivity(
                    context = context,
                    currentFileNode = typedFileNode,
                    nodeSourceType = nodeSourceType ?: Constants.FILE_BROWSER_ADAPTER
                )

                is FileNodeContent.AudioOrVideo -> {
                    openVideoOrAudioFile(
                        context = context,
                        content = content.uri,
                        fileNode = typedFileNode,
                        snackBarHostState = snackBarHostState,
                        nodeActionsViewModel = nodeActionsViewModel,
                        sortOrder = sortOrder,
                        viewType = nodeSourceType ?: Constants.FILE_BROWSER_ADAPTER
                    )
                }

                is FileNodeContent.UrlContent -> {
                    openUrlFile(
                        context = context,
                        content = content,
                        snackBarHostState = snackBarHostState
                    )
                }

                is FileNodeContent.Other -> {
                    content.localFile?.let {
                        if (typedFileNode.type is ZipFileTypeInfo) {
                            openZipFile(
                                context = context,
                                localFile = it,
                                fileNode = typedFileNode,
                                snackBarHostState = snackBarHostState
                            )
                        } else {
                            handleOtherFiles(
                                context = context,
                                localFile = it,
                                currentFileNode = typedFileNode,
                                snackBarHostState = snackBarHostState,
                                nodeActionsViewModel = nodeActionsViewModel
                            )
                        }
                    } ?: run {
                        nodeActionsViewModel.downloadNodeForPreview(typedFileNode)
                    }
                }

                else -> {

                }
            }
            onActionHandled()
        }.onFailure {
            Timber.e(it)
        }
    }
}

private fun openTextEditorActivity(
    context: Context,
    currentFileNode: TypedFileNode,
    nodeSourceType: Int,
) {
    val textFileIntent = Intent(context, TextEditorActivity::class.java)
    textFileIntent.putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, currentFileNode.id.longValue)
        .putExtra(TextEditorViewModel.MODE, TextEditorViewModel.VIEW_MODE)
        .putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, nodeSourceType)
    context.startActivity(textFileIntent)
}

private fun openPdfActivity(
    context: Context,
    content: NodeContentUri,
    type: Int?,
    currentFileNode: TypedFileNode,
    nodeActionsViewModel: NodeActionsViewModel,
) {
    val pdfIntent = Intent(context, PdfViewerActivity::class.java)
    val mimeType = currentFileNode.type.mimeType
    pdfIntent.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, currentFileNode.id.longValue)
        putExtra(Constants.INTENT_EXTRA_KEY_INSIDE, true)
        putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, type)
        putExtra(Constants.INTENT_EXTRA_KEY_APP, true)
    }
    nodeActionsViewModel.applyNodeContentUri(
        intent = pdfIntent,
        content = content,
        mimeType = mimeType,
    )
    context.startActivity(pdfIntent)
}

private fun openImageViewerActivity(
    context: Context,
    isImagePreview: Boolean,
    currentFileNode: TypedFileNode,
    nodeSourceType: Int?,
    sortOrder: SortOrder,
) {
    val intent = if (isImagePreview &&
        (nodeSourceType == Constants.FILE_BROWSER_ADAPTER)
    ) {
        ImagePreviewActivity.createIntent(
            context = context,
            imageSource = ImagePreviewFetcherSource.CLOUD_DRIVE,
            menuOptionsSource = ImagePreviewMenuSource.CLOUD_DRIVE,
            anchorImageNodeId = currentFileNode.id,
            params = mapOf(CloudDriveImageNodeFetcher.PARENT_ID to currentFileNode.parentId.longValue),
        )
    } else if (isImagePreview && nodeSourceType == Constants.RUBBISH_BIN_ADAPTER) {
        ImagePreviewActivity.createIntent(
            context = context,
            imageSource = ImagePreviewFetcherSource.RUBBISH_BIN,
            menuOptionsSource = ImagePreviewMenuSource.RUBBISH_BIN,
            anchorImageNodeId = currentFileNode.id,
            params = mapOf(RubbishBinImageNodeFetcher.PARENT_ID to currentFileNode.parentId.longValue),
        )
    } else {
        ImageViewerActivity.getIntentForParentNode(
            context,
            currentFileNode.parentId.longValue,
            sortOrder,
            currentFileNode.id.longValue
        )
    }
    context.startActivity(intent)
}

private suspend fun openVideoOrAudioFile(
    context: Context,
    fileNode: TypedFileNode,
    content: NodeContentUri,
    snackBarHostState: SnackbarHostState?,
    nodeActionsViewModel: NodeActionsViewModel,
    sortOrder: SortOrder,
    viewType: Int
) {
    val intent = when {
        fileNode.type.isSupported && fileNode.type is VideoFileTypeInfo ->
            Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(
                    Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
                    sortOrder
                )
            }

        fileNode.type.isSupported && fileNode.type is AudioFileTypeInfo -> Intent(
            context,
            AudioPlayerActivity::class.java
        ).apply {
            putExtra(
                Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
                sortOrder
            )
        }

        else -> Intent(Intent.ACTION_VIEW)
    }.apply {
        putExtra(Constants.INTENT_EXTRA_KEY_PLACEHOLDER, 0)
        putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, viewType)
        putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, fileNode.name)
        putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, fileNode.id.longValue)
        putExtra(Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, fileNode.parentId.longValue)
        putExtra(Constants.INTENT_EXTRA_KEY_IS_FOLDER_LINK, false)
        val mimeType =
            if (fileNode.type.extension == "opus") "audio/*" else fileNode.type.mimeType
        nodeActionsViewModel.applyNodeContentUri(
            intent = this,
            content = content,
            mimeType = mimeType,
        )
    }
    safeLaunchActivity(context = context, intent = intent, snackBarHostState = snackBarHostState)
}

private suspend fun openUrlFile(
    context: Context,
    content: FileNodeContent.UrlContent,
    snackBarHostState: SnackbarHostState?,
) {
    content.path?.let {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(it)
        }
        context.startActivity(intent)
    } ?: run {
        snackBarHostState?.showSnackbar(message = context.getString(R.string.general_text_error))
    }
}

private suspend fun safeLaunchActivity(
    context: Context,
    intent: Intent,
    snackBarHostState: SnackbarHostState?,
) {
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        Timber.e(it)
        snackBarHostState?.showSnackbar(message = context.getString(R.string.intent_not_available))
    }
}

private suspend fun Intent.openShareIntent(
    context: Context,
    snackBarHostState: SnackbarHostState?,
) {
    if (resolveActivity(context.packageManager) == null) {
        action = Intent.ACTION_SEND
    }
    runCatching {
        context.startActivity(this)
    }.onFailure { error ->
        Timber.e(error)
        snackBarHostState?.showSnackbar(context.getString(R.string.intent_not_available))
    }
}

private suspend fun openZipFile(
    context: Context,
    localFile: File,
    fileNode: TypedFileNode,
    snackBarHostState: SnackbarHostState?,
) {
    Timber.d("The file is zip, open in-app.")
    if (ZipBrowserActivity.zipFileFormatCheck(context, localFile.absolutePath)) {
        context.startActivity(
            Intent(context, ZipBrowserActivity::class.java).apply {
                putExtra(
                    ZipBrowserActivity.EXTRA_PATH_ZIP, localFile.absolutePath
                )
                putExtra(
                    ZipBrowserActivity.EXTRA_HANDLE_ZIP, fileNode.id.longValue
                )
            }
        )
    } else {
        snackBarHostState?.showSnackbar(context.getString(R.string.message_zip_format_error))
    }
}

private suspend fun handleOtherFiles(
    context: Context,
    localFile: File,
    currentFileNode: TypedFileNode,
    snackBarHostState: SnackbarHostState?,
    nodeActionsViewModel: NodeActionsViewModel,
) {
    Intent(Intent.ACTION_VIEW).apply {
        nodeActionsViewModel.applyNodeContentUri(
            intent = this,
            content = NodeContentUri.LocalContentUri(localFile),
            mimeType = currentFileNode.type.mimeType,
            isSupported = false
        )
        runCatching {
            context.startActivity(this)
        }.onFailure { error ->
            Timber.e(error)
            openShareIntent(context = context, snackBarHostState = snackBarHostState)
        }
    }
}
