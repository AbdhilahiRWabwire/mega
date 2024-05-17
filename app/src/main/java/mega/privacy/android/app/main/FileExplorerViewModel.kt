package mega.privacy.android.app.main

import android.content.Context
import android.content.Intent
import android.webkit.URLUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.extensions.serializable
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ShareTextInfo
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.GetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.GetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.message.AttachNodeUseCase
import mega.privacy.android.domain.usecase.chat.message.SendChatAttachmentsUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * ViewModel class responsible for preparing and managing the data for FileExplorerActivity.
 *
 * @property storageState    [StorageState]
 * @property isImportingText True if it is importing text, false if it is importing files.
 * @property fileNames       File names.
 */
@HiltViewModel
class FileExplorerViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val getCopyLatestTargetPathUseCase: GetCopyLatestTargetPathUseCase,
    private val getMoveLatestTargetPathUseCase: GetMoveLatestTargetPathUseCase,
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val attachNodeUseCase: AttachNodeUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val sendChatAttachmentsUseCase: SendChatAttachmentsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
) : ViewModel() {

    private var dataAlreadyRequested = false
    var latestCopyTargetPath: Long? = null
    var latestCopyTargetPathTab: Int = 0
    var latestMoveTargetPath: Long? = null
    var latestMoveTargetPathTab: Int = 0
    private val _filesInfo = MutableLiveData<List<ShareInfo>>()
    private val _textInfo = MutableLiveData<ShareTextInfo>()
    private val _fileNames = MutableLiveData<HashMap<String, String>>()

    /**
     * File names
     */
    val fileNames: LiveData<HashMap<String, String>> = _fileNames

    /**
     * Storage state
     */
    val storageState: StorageState
        get() = monitorStorageStateEventUseCase.getState()

    /**
     * Notifies observers about filesInfo changes.
     */
    val filesInfo: LiveData<List<ShareInfo>> = _filesInfo

    /**
     * Notifies observers about textInfo updates.
     */
    val textInfo: LiveData<ShareTextInfo> = _textInfo

    /**
     * Gets [ShareTextInfo].
     */
    val textInfoContent get() = _textInfo.value

    private val _copyTargetPathFlow = MutableStateFlow<Long?>(null)

    /**
     * Gets the latest used target path of move/copy
     */
    val copyTargetPathFlow: StateFlow<Long?> = _copyTargetPathFlow.asStateFlow()

    private val _moveTargetPathFlow = MutableStateFlow<Long?>(null)

    /**
     * Gets the latest used target path of move
     */
    val moveTargetPathFlow: StateFlow<Long?> = _moveTargetPathFlow.asStateFlow()

    private var _accountDetail: AccountDetail? = null

    val accountDetail: AccountDetail? get() = _accountDetail

    private var _showHiddenItems: Boolean = true

    val showHiddenItems: Boolean get() = _showHiddenItems

    fun init() = viewModelScope.launch {
        if (getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)) {
            _accountDetail = monitorAccountDetailUseCase().firstOrNull()
            _showHiddenItems = monitorShowHiddenItemsUseCase().firstOrNull() ?: true
        }
    }

    /**
     * Set file names
     *
     * @param fileNames
     */
    fun setFileNames(fileNames: HashMap<String, String>) {
        _fileNames.value = fileNames
    }

    /**
     * Get the ShareInfo list
     *
     * @param context Current context
     * @param intent  The intent that started the current activity
     */
    fun ownFilePrepareTask(context: Context, intent: Intent) {
        if (dataAlreadyRequested) return

        viewModelScope.launch(ioDispatcher) {
            dataAlreadyRequested = true
            if (isImportingText(intent)) {
                updateTextInfoFromIntent(intent, context)
            } else {
                updateFilesInfoFromIntent(intent, context)
            }
        }
    }

    /**
     * Update text info from intent
     *
     * @param intent
     */
    private fun updateTextInfoFromIntent(intent: Intent, context: Context) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        val isUrl = URLUtil.isHttpUrl(sharedText) || URLUtil.isHttpsUrl(sharedText)
        val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        val sharedEmail = intent.getStringExtra(Intent.EXTRA_EMAIL)
        val subject = sharedSubject ?: ""

        val fileContent = buildFileContent(
            text = sharedText,
            subject = sharedSubject,
            email = sharedEmail,
            isUrl = isUrl,
            context = context,
        )
        val messageContent = buildMessageContent(
            text = sharedText,
            email = sharedEmail,
            context = context,
        )

        _fileNames.postValue(hashMapOf(subject to subject))
        _textInfo.postValue(ShareTextInfo(isUrl, subject, fileContent, messageContent))
    }

    /**
     * Update files info from intent
     *
     * @param intent
     * @param context
     */
    private fun updateFilesInfoFromIntent(
        intent: Intent,
        context: Context?,
    ) {
        val shareInfo: List<ShareInfo> =
            getShareInfoList(intent, context) ?: emptyList()

        _fileNames.postValue(getShareInfoFileNamesMap(shareInfo))
        _filesInfo.postValue(shareInfo)
    }

    /**
     * Get share info list
     *
     * @param intent
     * @param context
     */
    private fun getShareInfoList(
        intent: Intent,
        context: Context?,
    ): List<ShareInfo>? = (intent.serializable(FileExplorerActivity.EXTRA_SHARE_INFOS)
        ?: ShareInfo.processIntent(intent, context))

    private fun getShareInfoFileNamesMap(shareInfo: List<ShareInfo>?) =
        shareInfo?.map { info ->
            info.getTitle().takeUnless {
                it.isNullOrBlank()
            } ?: info.originalFileName
        }?.associateTo(hashMapOf()) { it to it }

    /**
     * Builds file content from the shared text.
     *
     * @param text    Shared text.
     * @param subject Shared subject.
     * @param email   Shared email.
     * @param isUrl   True if it is sharing a link, false otherwise.
     * @return The file content.
     */
    private fun buildFileContent(
        text: String?,
        subject: String?,
        email: String?,
        isUrl: Boolean,
        context: Context,
    ): String {
        return if (isUrl && text != null) {
            buildUrlContent(text, subject, email, context)
        } else {
            buildMessageContent(text, email, context)
        }
    }

    /**
     * Build url content
     *
     * @param text
     * @param subject
     * @param email
     */
    private fun buildUrlContent(
        text: String?,
        subject: String?,
        email: String?,
        context: Context,
    ): String {
        val builder = StringBuilder()
        builder.append("[InternetShortcut]\n").append("URL=").append(text).append("\n\n")
        subject?.let {
            builder.append(context.getString(R.string.new_file_subject_when_uploading))
                .append(": ").append(it).append("\n")
        }
        email?.let {
            builder.append(context.getString(R.string.new_file_email_when_uploading))
                .append(": ").append(it)
        }
        return builder.toString()
    }

    /**
     * Builds message content from the shared text.
     *
     * @param text    Shared text.
     * @param email   Shared email.
     * @return The message content.
     */
    private fun buildMessageContent(
        text: String?,
        email: String?,
        context: Context,
    ): String {
        val builder = StringBuilder()
        email?.let {
            builder.append(context.getString(R.string.new_file_email_when_uploading))
                .append(": ").append(it).append("\n\n")
        }
        text?.let {
            builder.append(it)
        }
        return builder.toString()
    }

    /**
     * Builds the final content text to share as chat message.
     *
     * @return Text to share as chat message.
     */
    val messageToShare: String?
        get() {
            return _textInfo.value?.let {
                """
                ${fileNames.value?.get(it.subject) ?: it.subject}
                
                ${it.messageContent}
                """.trimIndent()
            }
        }

    /**
     * Checks if it is importing a text instead of files.
     * This is true if the action of the intent is ACTION_SEND, the type of the intent
     * is TYPE_TEXT_PLAIN and the intent does not contain EXTRA_STREAM extras.
     *
     */
    fun isImportingText(intent: Intent): Boolean =
        intent.action == Intent.ACTION_SEND
                && intent.type == Constants.TYPE_TEXT_PLAIN
                && intent.extras?.containsKey(Intent.EXTRA_STREAM)?.not() ?: true

    /**
     * Get the last target path of copy if not valid then return null
     */
    fun getCopyTargetPath() {
        viewModelScope.launch {
            latestCopyTargetPath = runCatching { getCopyLatestTargetPathUseCase() }.getOrNull()
            latestCopyTargetPath?.let {
                val accessPermission =
                    runCatching { getNodeAccessPermission(NodeId(it)) }.getOrNull()
                latestCopyTargetPathTab =
                    if (accessPermission == null || accessPermission == AccessPermission.OWNER)
                        FileExplorerActivity.CLOUD_TAB
                    else
                        FileExplorerActivity.INCOMING_TAB
            }
            _copyTargetPathFlow.emit(latestCopyTargetPath ?: -1)
        }
    }

    /**
     * Get the last target path of move if not valid then return null
     */
    fun getMoveTargetPath() {
        viewModelScope.launch {
            latestMoveTargetPath = runCatching { getMoveLatestTargetPathUseCase() }.getOrNull()
            latestMoveTargetPath?.let {
                val accessPermission =
                    runCatching { getNodeAccessPermission(NodeId(it)) }.getOrNull()
                latestMoveTargetPathTab =
                    if (accessPermission == null || accessPermission == AccessPermission.OWNER)
                        FileExplorerActivity.CLOUD_TAB
                    else
                        FileExplorerActivity.INCOMING_TAB
            }
            _moveTargetPathFlow.emit(latestMoveTargetPath ?: -1)
        }
    }

    /**
     * Reset copyTargetPathFlow state
     */
    fun resetCopyTargetPathState() {
        _copyTargetPathFlow.value = null
    }

    /**
     * Reset moveTargetPathFlow state
     */
    fun resetMoveTargetPathState() {
        _moveTargetPathFlow.value = null
    }

    /**
     * Upload files and nodes to the specified chats if the NewChatActivity feature flag is true, otherwise it invokes [toDoIfFalse]
     * In both cases, it will call [toDoAfter] after starting the upload
     */
    fun uploadFilesToChatIfFeatureFlagIsTrue(
        chatIds: List<Long>,
        filePaths: List<String>,
        nodeIds: List<NodeId>,
        toDoIfFalse: () -> Unit,
        toDoAfter: () -> Unit,
    ) {
        viewModelScope.launch {
            if (getFeatureFlagValueUseCase(AppFeatures.NewChatActivity)) {
                chatIds.forEach {
                    attachNodes(it, nodeIds)
                }
                attachFiles(chatIds, filePaths)
            } else {
                toDoIfFalse()
            }
            toDoAfter()
        }
    }

    private suspend fun attachFiles(chatIds: List<Long>, filePaths: List<String>) {
        val filePathsWithNames =
            filePaths.associateWith { fileNames.value?.get(it.split(File.separator).last()) }
        sendChatAttachmentsUseCase(filePathsWithNames, chatIds = chatIds.toLongArray())
            .catch { Timber.e("Error attaching files", it) }
            .collect()
    }

    private suspend fun attachNodes(chatId: Long, nodes: List<NodeId>) {
        nodes
            .mapNotNull { runCatching { getNodeByIdUseCase(it) }.getOrNull() }
            .filterIsInstance<FileNode>()
            .forEach {
                runCatching {
                    attachNodeUseCase(chatId, it as TypedFileNode)
                }.onFailure { Timber.e("Error attaching a node", it) }
            }
    }
}