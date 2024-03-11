package mega.privacy.android.app.presentation.meeting.chat.model.messages

import android.content.Context
import android.content.Intent
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.mediaplayer.AudioPlayerActivity
import mega.privacy.android.app.mediaplayer.VideoPlayerActivity
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.NodeAttachmentMessageView
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.NodeAttachmentMessageViewModel
import mega.privacy.android.app.presentation.node.FileNodeContent
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.textEditor.TextEditorActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.zippreview.ui.ZipBrowserActivity
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.core.ui.controls.layouts.LocalSnackBarHostState
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.chat.ChatFile
import timber.log.Timber
import java.io.File

/**
 * Node attachment Ui message
 *
 * @param message [NodeAttachmentMessageView]
 */
data class NodeAttachmentUiMessage(
    override val message: NodeAttachmentMessage,
    override val reactions: List<UIReaction>,
) : AvatarMessage() {

    @Composable
    override fun ContentComposable(
        interactionEnabled: Boolean,
        initialiseModifier: (onClick: () -> Unit) -> Modifier,
    ) {
        val viewModel: NodeAttachmentMessageViewModel = hiltViewModel()
        val chatViewModel: ChatViewModel = hiltViewModel()
        val uiState by viewModel.updateAndGetUiStateFlow(message).collectAsStateWithLifecycle()
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        val snackbarHostState = LocalSnackBarHostState.current
        val onClick: () -> Unit = {
            coroutineScope.launch {
                if (!message.exists) {
                    snackbarHostState?.showSnackbar(context.getString(R.string.error_file_not_available))
                    return@launch
                }
                runCatching {
                    viewModel.handleFileNode(message)
                }.onSuccess { content ->
                    when (content) {
                        is FileNodeContent.ImageForChat -> openImageViewer(
                            context,
                            message.chatId,
                            content.allAttachmentMessageIds.toLongArray(),
                            message.fileNode.id.longValue
                        )

                        is FileNodeContent.AudioOrVideo -> openVideoOrAudioFile(
                            context,
                            message,
                            content.uri,
                            viewModel
                        )

                        is FileNodeContent.Other -> openOtherFiles(
                            content.localFile,
                            viewModel,
                            message,
                            context,
                            snackbarHostState,
                            chatViewModel
                        )

                        is FileNodeContent.Pdf -> openPdfActivity(
                            viewModel = viewModel,
                            context = context,
                            message = message,
                            chatId = message.chatId,
                            content = content.uri
                        )

                        FileNodeContent.TextContent -> handleTextEditor(
                            context,
                            message.msgId,
                            message.chatId
                        )

                        else -> {}
                    }
                }.onFailure {
                    Timber.e(it, "Failed to handle file node")
                }
            }
        }

        NodeAttachmentMessageView(
            message = message,
            modifier = initialiseModifier(onClick),
            uiState = uiState,
        )
    }

    private fun openVideoOrAudioFile(
        context: Context,
        message: NodeAttachmentMessage,
        uri: NodeContentUri,
        viewModel: NodeAttachmentMessageViewModel,
    ) {
        val fileNode = message.fileNode
        val intent = buildVideoOrAudioIntent(fileNode, context, message)
        val mimeType =
            if (fileNode.type.extension == "opus") "audio/*" else fileNode.type.mimeType
        viewModel.applyNodeContentUri(
            intent = intent,
            content = uri,
            mimeType = mimeType,
            isSupported = fileNode.type.isSupported
        )
        safeLaunchActivity(context, intent)
    }

    private fun buildVideoOrAudioIntent(
        fileNode: FileNode,
        context: Context,
        message: NodeAttachmentMessage,
    ): Intent = when {
        fileNode.type.isSupported && fileNode.type is VideoFileTypeInfo -> Intent(
            context,
            VideoPlayerActivity::class.java
        )

        fileNode.type.isSupported && fileNode.type is AudioFileTypeInfo -> Intent(
            context,
            AudioPlayerActivity::class.java
        )

        else -> Intent(Intent.ACTION_VIEW)
    }.apply {
        putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.FROM_CHAT)
        putExtra(Constants.INTENT_EXTRA_KEY_IS_PLAYLIST, false)
        putExtra(Constants.INTENT_EXTRA_KEY_MSG_ID, message.msgId)
        putExtra(Constants.INTENT_EXTRA_KEY_CHAT_ID, message.chatId)
        putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, fileNode.name)
        putExtra("HANDLE", fileNode.id.longValue)
    }

    private fun openPdfActivity(
        context: Context,
        viewModel: NodeAttachmentMessageViewModel,
        message: NodeAttachmentMessage,
        chatId: Long,
        content: NodeContentUri,
    ) {
        val pdfIntent = Intent(
            context,
            PdfViewerActivity::class.java
        )
        pdfIntent.putExtra(Constants.INTENT_EXTRA_KEY_INSIDE, true)
        pdfIntent.putExtra(
            Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
            Constants.FROM_CHAT
        )
        pdfIntent.putExtra(Constants.INTENT_EXTRA_KEY_MSG_ID, message.msgId)
        pdfIntent.putExtra(Constants.INTENT_EXTRA_KEY_CHAT_ID, chatId)
        viewModel.applyNodeContentUri(
            intent = pdfIntent,
            content = content,
            mimeType = PdfFileTypeInfo.mimeType
        )
        pdfIntent.putExtra("HANDLE", message.fileNode.id.longValue)
        context.startActivity(pdfIntent)
    }

    private fun openImageViewer(
        context: Context,
        chatId: Long,
        messageIds: LongArray,
        nodeId: Long,
    ) {
        val intent = ImageViewerActivity.getIntentForChatMessages(
            context = context,
            chatRoomId = chatId,
            messageIds = messageIds,
            currentNodeHandle = nodeId
        )
        context.startActivity(intent)
    }

    private fun handleTextEditor(context: Context, msgId: Long, chatId: Long) {
        context.startActivity(
            Intent(context, TextEditorActivity::class.java)
                .putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.FROM_CHAT)
                .putExtra(Constants.MESSAGE_ID, msgId)
                .putExtra(Constants.CHAT_ID, chatId)
        )
    }

    private suspend fun openOtherFiles(
        localFile: File?,
        viewModel: NodeAttachmentMessageViewModel,
        message: NodeAttachmentMessage,
        context: Context,
        snackbarHostState: SnackbarHostState?,
        chatViewModel: ChatViewModel,
    ) {
        val fileNode = message.fileNode
        if (localFile != null) {
            if (fileNode.type is ZipFileTypeInfo) {
                openZipFile(context, localFile, fileNode, snackbarHostState)
            } else {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    viewModel.applyNodeContentUri(
                        intent = this,
                        content = NodeContentUri.LocalContentUri(localFile),
                        mimeType = fileNode.type.mimeType,
                        isSupported = false
                    )
                }
                runCatching {
                    context.startActivity(intent)
                }.onFailure {
                    // no activity found to open file, just show download snackbar
                    snackbarHostState?.showSnackbar(context.getString(R.string.general_already_downloaded))
                }
            }
        } else {
            chatViewModel.onDownloadForPreviewChatNode(fileNode)
        }
    }

    private suspend fun openZipFile(
        context: Context,
        localFile: File,
        fileNode: ChatFile,
        snackbarHostState: SnackbarHostState?,
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
            snackbarHostState?.showSnackbar(context.getString(R.string.message_zip_format_error))
        }
    }

    /**
     * Safe launch activity
     *
     * Only need to call this function in cases of starting activity by ACTION, for example:
     * ACTION_VIEW, ACTION_EDIT etc
     * Some devices may not have an activity to handle the intent with the specified mime type
     * We do not need to call this for internal intents handled by our own app
     */
    private fun safeLaunchActivity(context: Context, intent: Intent) {
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            Timber.e(it, "No activity found to open file")
        }
    }

    override val showAvatar = message.shouldShowAvatar
    override val displayAsMine = message.isMine
    override val shouldDisplayForwardIcon = message.exists
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
}