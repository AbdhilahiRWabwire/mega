package mega.privacy.android.app.presentation.qrcode

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.FileStorageActivity
import mega.privacy.android.app.namecollision.data.NameCollisionUiEntity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.app.usecase.UploadUseCase
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * QR code compose activity
 */
@AndroidEntryPoint
class QRCodeComposeActivity : PasscodeActivity() {

    private val viewModel: QRCodeViewModel by viewModels()
    private var inviteContacts = false
    private var showScanQrView = false

    /**
     * QR code mapper
     */
    @Inject
    lateinit var qrCodeMapper: QRCodeMapper

    /**
     * Upload use case
     */
    @Inject
    lateinit var uploadUseCase: UploadUseCase

    /**
     * Get feature flag value use case
     */
    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase


    /**
     * Get theme mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val selectStorageDestinationLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result.data
            if (result.resultCode == RESULT_OK) {
                data?.getStringExtra(FileStorageActivity.EXTRA_PATH)?.let { parentPath ->
                    viewModel.saveToFileSystem(parentPath)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        showScanQrView = intent.getBooleanExtra(Constants.OPEN_SCAN_QR, false)
        inviteContacts = intent.getBooleanExtra(Constants.INVITE_CONTACT, false)
        var showLoader = true

        viewModel.setFinishActivityOnScanComplete(inviteContacts)
        setContent {
            val mode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val viewState by viewModel.uiState.collectAsStateWithLifecycle()
            OriginalTempTheme(isDark = mode.isDarkMode()) {
                QRCodeView(
                    viewState = viewState,
                    onBackPressed = onBackPressedDispatcher::onBackPressed,
                    onCreateQRCode = { viewModel.createQRCode(true) },
                    onDeleteQRCode = viewModel::deleteQRCode,
                    onResetQRCode = viewModel::resetQRCode,
                    onScanQrCodeClicked = { viewModel.scanCode(this) },
                    onCopyLinkClicked = viewModel::copyContactLink,
                    onViewContactClicked = ::onViewContact,
                    onInviteContactClicked = viewModel::sendInvite,
                    onResultMessageConsumed = viewModel::resetResultMessage,
                    onScannedContactLinkResultConsumed = viewModel::resetScannedContactLinkResult,
                    onInviteContactResultConsumed = viewModel::resetInviteContactResult,
                    onInviteResultDialogDismiss = viewModel::resetScannedContactEmail,
                    onInviteContactDialogDismiss = viewModel::resetScannedContactAvatar,
                    onCloudDriveClicked = viewModel::saveToCloudDrive,
                    onFileSystemClicked = ::saveToFileSystem,
                    onShowCollision = ::showCollision,
                    onUploadFile = { uploadFile(qrFile = it.first, parentHandle = it.second) },
                    onShowCollisionConsumed = viewModel::resetShowCollision,
                    onUploadFileConsumed = viewModel::resetUploadFile,
                    onScanCancelConsumed = viewModel::resetScanCancel,
                    onUploadEventConsumed = viewModel::onUploadEventConsumed,
                    qrCodeMapper = qrCodeMapper,
                )
            }
        }
        if (showScanQrView || inviteContacts) {
            showLoader = false
            viewModel.scanCode(this)
        }
        viewModel.createQRCode(showLoader)
    }

    private fun onViewContact(email: String) {
        ContactUtil.openContactInfoActivity(this, email)
        finish()
    }

    @SuppressLint("CheckResult")
    private fun uploadFile(qrFile: File, parentHandle: Long) {
        lifecycleScope.launch {
            if (getFeatureFlagValueUseCase(AppFeatures.UploadWorker)) {
                viewModel.uploadFile(qrFile, parentHandle)
            } else {
                PermissionUtils.checkNotificationsPermission(this@QRCodeComposeActivity)
                uploadUseCase.upload(this@QRCodeComposeActivity, qrFile, parentHandle)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        viewModel.setResultMessage(
                            R.string.save_qr_cloud_drive,
                            arrayOf(qrFile.name)
                        )
                    }) { t: Throwable? -> Timber.e(t) }
            }
        }
    }


    private fun saveToFileSystem() {
        val intent = Intent(this, FileStorageActivity::class.java).apply {
            putExtra(
                FileStorageActivity.PICK_FOLDER_TYPE,
                FileStorageActivity.PickFolderType.DOWNLOAD_FOLDER.folderType
            )
            action = FileStorageActivity.Mode.PICK_FOLDER.action
        }
        selectStorageDestinationLauncher.launch(intent)
    }

    private fun showCollision(collision: NameCollisionUiEntity) {
        legacyNameCollisionActivityContract?.launch(arrayListOf(collision))
    }
}

/**
 * Find the Activity in a given Context.
 */
internal fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}