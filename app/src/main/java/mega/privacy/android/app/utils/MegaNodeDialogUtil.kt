package mega.privacy.android.app.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.BUTTON_NEGATIVE
import android.content.DialogInterface.BUTTON_POSITIVE
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.EmojiEditText
import mega.privacy.android.app.interfaces.ActionBackupNodeCallback
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.listeners.MoveListener
import mega.privacy.android.app.listeners.RemoveListener
import mega.privacy.android.app.listeners.RenameListener
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop
import mega.privacy.android.app.textEditor.TextEditorActivity
import mega.privacy.android.app.textEditor.TextEditorViewModel.Companion.CREATE_MODE
import mega.privacy.android.app.textEditor.TextEditorViewModel.Companion.MODE
import mega.privacy.android.app.utils.AlertsAndWarnings.showForeignStorageOverQuotaWarningDialog
import mega.privacy.android.app.utils.ColorUtils.setErrorAwareInputAppearance
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.TXT_EXTENSION
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.MegaNodeUtil.getRootParentNode
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.TextUtil.getCursorPositionOfName
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.Util.*
import nz.mega.documentscanner.utils.ViewUtils.hideKeyboard
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaNode
import java.util.*

object MegaNodeDialogUtil {
    private const val TYPE_RENAME = 0
    private const val TYPE_NEW_FOLDER = 1
    private const val TYPE_NEW_FILE = 2
    private const val TYPE_NEW_URL_FILE = 3
    private const val TYPE_NEW_TXT_FILE = 4
    const val IS_NEW_TEXT_FILE_SHOWN = "IS_NEW_TEXT_FILE_SHOWN"
    const val NEW_TEXT_FILE_TEXT = "NEW_TEXT_FILE_TEXT"
    private const val ERROR_EMPTY_EXTENSION = "ERROR_EMPTY_EXTENSION"
    private const val ERROR_DIFFERENT_EXTENSION = "ERROR_DIFFERENT_EXTENSION"
    private const val NO_ERROR = "NO_ERROR"

    // Backup warning dialog
    const val BACKUP_HANDLED_ITEM: String = "BackupHandleItem"
    const val BACKUP_HANDLED_NODE = "BackupHandleNode"
    const val BACKUP_NODE_TYPE = "BackupNodeType"
    const val BACKUP_ACTION_TYPE = "BackupActionType"
    const val BACKUP_DIALOG_WARN = "BackupDialogWarn"

    // Backup node type
    const val BACKUP_NONE = -1 // The folder is not belong to the MyBackup
    const val BACKUP_ROOT = 0 // MyBackup folder
    const val BACKUP_DEVICE = 1 // Device folder
    const val BACKUP_FOLDER = 2 // Backup folders underneath device folders
    const val BACKUP_FOLDER_CHILD = 3 // All backups underneath BACKUP_FOLDER

    // For backup node actions
    const val ACTION_BACKUP_NONE = -1
    const val ACTION_BACKUP_MOVE = 0
    const val ACTION_BACKUP_REMOVE = 1
    const val ACTION_BACKUP_FAB = 2
    const val ACTION_BACKUP_SHARE_FOLDER = 6
    const val ACTION_MENU_BACKUP_SHARE_FOLDER = 7
    const val ACTION_BACKUP_SHARE = 9
    const val ACTION_BACKUP_COPY = 12
    const val ACTION_MOVE_TO_BACKUP = 13
    const val ACTION_COPY_TO_BACKUP = 14

    /**
     * Creates and shows a TYPE_RENAME dialog to rename a node.
     *
     * @param context            Current context.
     * @param node               A valid node.
     * @param snackbarShower     Interface to show snackbar.
     * @param actionNodeCallback Callback to finish the rename action if needed, null otherwise.
     * @return The rename dialog.
     */
    @JvmStatic
    fun showRenameNodeDialog(
        context: Context,
        node: MegaNode,
        snackbarShower: SnackbarShower?,
        actionNodeCallback: ActionNodeCallback?
    ): AlertDialog {
        val renameDialogBuilder = MaterialAlertDialogBuilder(context)

        renameDialogBuilder
            .setTitle(getString(R.string.context_rename))
            .setPositiveButton(R.string.context_rename, null)
            .setNegativeButton(R.string.general_cancel, null)

        return setFinalValuesAndShowDialog(
            context, node, actionNodeCallback, snackbarShower,
            null, null, false, renameDialogBuilder, TYPE_RENAME
        )
    }

    /**
     * Creates and shows a TYPE_NEW_FOLDER dialog to create a new folder.
     *
     * @param context            Current context.
     * @param actionNodeCallback Callback to finish the create folder action if needed, null otherwise.
     * @return The create new folder dialog.
     */
    @JvmStatic
    fun showNewFolderDialog(
        context: Context,
        actionNodeCallback: ActionNodeCallback?
    ): AlertDialog {
        val newFolderDialogBuilder = MaterialAlertDialogBuilder(context)

        newFolderDialogBuilder
            .setTitle(R.string.menu_new_folder)
            .setPositiveButton(R.string.general_create, null)
            .setNegativeButton(R.string.general_cancel, null)

        return setFinalValuesAndShowDialog(
            context, null, actionNodeCallback, null,
            null, null, false, newFolderDialogBuilder, TYPE_NEW_FOLDER
        )
    }

    /**
     * Creates and shows a TYPE_NEW_FILE dialog to create a new file.
     *
     * @param context Current context.
     * @param parent  A valid node. Specifically the parent in which the folder will be created.
     * @param data    Valid data. Specifically the content of the new file.
     * @return The create new file dialog.
     */
    @JvmStatic
    fun showNewFileDialog(context: Context, parent: MegaNode, data: String): AlertDialog {
        val newFileDialogBuilder = MaterialAlertDialogBuilder(context)

        newFileDialogBuilder
            .setTitle(R.string.context_new_file_name)
            .setPositiveButton(R.string.general_ok, null)
            .setNegativeButton(R.string.general_cancel, null)

        return setFinalValuesAndShowDialog(
            context, parent, null, null,
            data, null, false, newFileDialogBuilder, TYPE_NEW_FILE
        )
    }

    /**
     * Creates and shows a TYPE_NEW_URL_FILE dialog to create a new URL file.
     *
     * @param context        Current context.
     * @param parent         A valid node. Specifically the parent in which the folder will be created.
     * @param data           Valid data. Specifically the content of the new URL file.
     * @param defaultURLName Default name of the URL if has, null otherwise.
     * @return The create new URL file dialog.
     */
    @JvmStatic
    fun showNewURLFileDialog(
        context: Context,
        parent: MegaNode,
        data: String,
        defaultURLName: String?
    ): AlertDialog {
        val newURLFileDialogBuilder = MaterialAlertDialogBuilder(context)

        newURLFileDialogBuilder
            .setTitle(R.string.dialog_title_new_link)
            .setPositiveButton(R.string.general_ok, null)
            .setNegativeButton(R.string.general_cancel, null)

        return setFinalValuesAndShowDialog(
            context, parent, null, null,
            data, defaultURLName, false, newURLFileDialogBuilder, TYPE_NEW_URL_FILE
        )
    }

    /**
     * Creates and shows a TYPE_NEW_TXT_FILE dialog to create a new text file.
     *
     * @param context   Current context.
     * @param parent    A valid node. Specifically the parent in which the file will be created.
     * @param typedName The previous typed text.
     * @param fromHome  True if the text file will be created from Homepage, false otherwise.
     * @return The create new text file dialog.
     */
    @JvmStatic
    fun showNewTxtFileDialog(
        context: Context,
        parent: MegaNode,
        typedName: String?,
        fromHome: Boolean
    ): AlertDialog {
        val newTxtFileDialogBuilder = MaterialAlertDialogBuilder(context)

        newTxtFileDialogBuilder
            .setTitle(R.string.dialog_title_new_text_file)
            .setPositiveButton(R.string.general_create, null)
            .setNegativeButton(R.string.general_cancel, null)

        val dialog = setFinalValuesAndShowDialog(
            context,
            parent,
            null,
            null,
            null,
            null,
            fromHome,
            newTxtFileDialogBuilder,
            TYPE_NEW_TXT_FILE
        )

        if (typedName != null && typedName != TXT_EXTENSION) {
            dialog.findViewById<EmojiEditText>(R.id.type_text)?.setText(typedName)
        }

        return dialog
    }

    /**
     * Finishes the initialization of the dialog and shows it.
     *
     * @param context            Current context.
     * @param node               A valid node if needed to confirm the action, null otherwise.
     * @param actionNodeCallback Callback to finish the node action if needed, null otherwise.
     * @param snackbarShower     Interface to show snackbar.
     * @param data               Valid data if needed to confirm the action, null otherwise.
     * @param defaultURLName     The default URL name if the dialog is TYPE_NEW_URL_FILE.
     * @param fromHome           True if the text file will be created from Homepage, false otherwise.
     * @param builder            The AlertDialog.Builder to create and show the final dialog.
     * @param dialogType         Indicates the type of dialog. It can be:
     *                              - TYPE_RENAME:       Rename action.
     *                              - TYPE_NEW_FOLDER:   Create new folder action.
     *                              - TYPE_NEW_FILE:     Create new file action.
     *                              - TYPE_NEW_URL_FILE: Create new URL file action.
     * @return The created dialog.
     */
    private fun setFinalValuesAndShowDialog(
        context: Context,
        node: MegaNode?,
        actionNodeCallback: ActionNodeCallback?,
        snackbarShower: SnackbarShower?,
        data: String?,
        defaultURLName: String?,
        fromHome: Boolean,
        builder: AlertDialog.Builder,
        dialogType: Int
    ): AlertDialog {
        builder.setView(R.layout.dialog_create_rename_node)

        val dialog = builder.create()

        dialog.apply {
            setOnShowListener {
                val typeText = findViewById<EmojiEditText>(R.id.type_text)
                val errorText = findViewById<TextView>(R.id.error_text)

                typeText?.apply {
                    when (dialogType) {
                        TYPE_RENAME -> {
                            if (node != null) {
                                setText(node.name)
                                setSelection(0, getCursorPositionOfName(node.isFile, node.name))
                            }
                        }
                        TYPE_NEW_FOLDER -> {
                            setHint(R.string.context_new_folder_name)
                        }
                        TYPE_NEW_FILE -> {
                            setHint(R.string.context_new_file_name_hint)
                        }
                        TYPE_NEW_URL_FILE -> {
                            if (isTextEmpty(defaultURLName)) setHint(R.string.context_new_link_name)
                            else {
                                setText(defaultURLName)
                                setSelection(0, getCursorPositionOfName(false, defaultURLName))
                            }
                        }
                        TYPE_NEW_TXT_FILE -> {
                            setHint(R.string.context_new_file_name)
                            setText(TXT_EXTENSION)
                            runDelay(SHOW_IM_DELAY.toLong()) { setSelection(0) }
                        }
                    }

                    doAfterTextChanged {
                        quitDialogError(typeText, errorText)
                    }

                    setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            checkActionDialogValue(
                                context, node, actionNodeCallback, snackbarShower,
                                typeText, data, errorText, fromHome, dialog, dialogType
                            )
                        }

                        false
                    }
                }

                quitDialogError(typeText, errorText)

                dialog.getButton(BUTTON_POSITIVE)
                    .setOnClickListener {
                        checkActionDialogValue(
                            context, node, actionNodeCallback, snackbarShower,
                            typeText, data, errorText, fromHome, dialog, dialogType
                        )
                    }

                showKeyboardDelayed(typeText)
            }
        }.show()

        return dialog
    }

    /**
     * Checks, after user's confirmation, if the typed value is valid.
     * - If so, confirms the action.
     * - If not, shows the error in question.
     *
     * @param context            Current context.
     * @param node               A valid node if needed to confirm the action, null otherwise.
     * @param actionNodeCallback Callback to finish the node action if needed, null otherwise.
     * @param snackbarShower     Interface to show snackbar.
     * @param typeText           The input text field.
     * @param data               Valid data if needed to confirm the action, null otherwise.
     * @param errorText          The text field to show the error.
     * @param fromHome           True if the text file will be created from Homepage, false otherwise.
     * @param dialog             The AlertDialog to check.
     * @param dialogType         Indicates the type of dialog. It can be:
     *                              - TYPE_RENAME:       Rename action.
     *                              - TYPE_NEW_FOLDER:   Create new folder action.
     *                              - TYPE_NEW_FILE:     Create new file action.
     *                              - TYPE_NEW_URL_FILE: Create new URL file action.
     */
    private fun checkActionDialogValue(
        context: Context,
        node: MegaNode?,
        actionNodeCallback: ActionNodeCallback?,
        snackbarShower: SnackbarShower?,
        typeText: EditText?,
        data: String?,
        errorText: TextView?,
        fromHome: Boolean,
        dialog: AlertDialog,
        dialogType: Int
    ) {
        val typedString = typeText?.text.toString().trim()

        when {
            typedString.isEmpty() -> {
                showDialogError(
                    typeText,
                    errorText,
                    getString(R.string.invalid_string)
                )
            }
            NODE_NAME_REGEX.matcher(typedString).find() -> {
                showDialogError(
                    typeText,
                    errorText,
                    getString(R.string.invalid_characters_defined)
                )
            }
            nameAlreadyExists(typedString, dialogType == TYPE_RENAME, node) -> {
                showDialogError(
                    typeText,
                    errorText,
                    getString(
                        if (dialogType == TYPE_RENAME || dialogType == TYPE_NEW_FOLDER) R.string.same_item_name_warning
                        else R.string.same_file_name_warning
                    )
                )
            }
            else -> {
                when (dialogType) {
                    TYPE_RENAME -> {
                        if (node != null && typedString != node.name) {
                            if (isOffline(context)) {
                                return
                            }

                            val oldMimeType = MimeTypeList.typeForName(node.name)
                            var newExtension = MimeTypeList.typeForName(typedString).extension
                            if (newExtension == typedString.lowercase(Locale.ROOT)) newExtension =
                                ""

                            when (if (node.isFolder) NO_ERROR else isValidRenameDialogValue(
                                oldMimeType,
                                newExtension
                            )) {
                                ERROR_EMPTY_EXTENSION -> {
                                    typeText?.hideKeyboard()

                                    showDialogError(
                                        typeText,
                                        errorText,
                                        getString(
                                            R.string.file_without_extension,
                                            oldMimeType.extension
                                        )
                                    )

                                    snackbarShower?.showSnackbar(
                                        SNACKBAR_TYPE,
                                        getString(R.string.file_without_extension_warning),
                                        MEGACHAT_INVALID_HANDLE
                                    )

                                    return
                                }
                                ERROR_DIFFERENT_EXTENSION -> {
                                    typeText?.hideKeyboard()

                                    showFileExtensionWarning(
                                        context,
                                        node,
                                        typedString,
                                        snackbarShower,
                                        actionNodeCallback
                                    )
                                }
                                NO_ERROR -> {
                                    confirmRenameAction(
                                        context,
                                        node,
                                        typedString,
                                        snackbarShower,
                                        actionNodeCallback
                                    )
                                }
                            }
                        }
                    }
                    TYPE_NEW_FOLDER -> {
                        actionNodeCallback?.createFolder(typedString)
                    }
                    TYPE_NEW_FILE -> {
                        if (context is FileExplorerActivityLollipop) {
                            context.createFile(typedString, data, node, false)
                        }
                    }
                    TYPE_NEW_URL_FILE -> {
                        if (context is FileExplorerActivityLollipop) {
                            context.createFile(typedString, data, node, true)
                        }
                    }
                    TYPE_NEW_TXT_FILE -> {
                        val textFileEditor = Intent(context, TextEditorActivity::class.java)
                            .putExtra(MODE, CREATE_MODE)
                            .putExtra(INTENT_EXTRA_KEY_FILE_NAME, typedString)
                            .putExtra(INTENT_EXTRA_KEY_HANDLE, node?.handle)
                            .putExtra(FROM_HOME_PAGE, fromHome)

                        context.startActivity(textFileEditor)
                    }
                }

                dialog.dismiss()
            }
        }
    }

    /**
     * Confirms the rename action.
     *
     * @param context            Current context.
     * @param node               A valid node if needed to confirm the action, null otherwise.
     * @param typedString        Typed name.
     * @param snackbarShower     Interface to show snackbar.
     * @param actionNodeCallback Callback to finish the node action if needed, null otherwise.
     */
    private fun confirmRenameAction(
        context: Context,
        node: MegaNode,
        typedString: String,
        snackbarShower: SnackbarShower?,
        actionNodeCallback: ActionNodeCallback?
    ) {
        val megaApi = MegaApplication.getInstance().megaApi

        megaApi.renameNode(
            node,
            typedString,
            RenameListener(
                snackbarShower,
                context,
                showSnackbar = true,
                isMyChatFilesFolder = false,
                actionNodeCallback = actionNodeCallback
            )
        )

        actionNodeCallback?.actionConfirmed()
    }

    /**
     * Checks if should allow the rename action:
     * - Should allow it if the new file name has the same extension than the old one.
     * - Should not allow it and show the corresponding error if the new file name has:
     *      * An empty extension and is not a text file.
     *      * A different extension than the old name.
     *
     * @param oldMimeType   Current mimeType of the file.
     * @param newExtension  New typed extension name for the file.
     * @return The corresponding error to show or not the corresponding warning.
     */
    private fun isValidRenameDialogValue(oldMimeType: MimeTypeList, newExtension: String): String {
        return when {
            newExtension.isEmpty() && !oldMimeType.isValidTextFileType -> ERROR_EMPTY_EXTENSION
            oldMimeType.extension != newExtension -> ERROR_DIFFERENT_EXTENSION
            else -> NO_ERROR
        }
    }

    /**
     * Shows a warning dialog informing the file extension changed after rename a file.
     *
     * @param context            Current context.
     * @param node               A valid node if needed to confirm the action, null otherwise.
     * @param typedString        Typed name.
     * @param snackbarShower     Interface to show snackbar.
     * @param actionNodeCallback Callback to finish the node action if needed, null otherwise.
     */
    private fun showFileExtensionWarning(
        context: Context,
        node: MegaNode,
        typedString: String,
        snackbarShower: SnackbarShower?,
        actionNodeCallback: ActionNodeCallback?
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.file_extension_change_title))
            .setMessage(getString(R.string.file_extension_change_warning))
            .setPositiveButton(getString(R.string.general_cancel), null)
            .setNegativeButton(getString(R.string.action_change_anyway)) { _, _ ->
                confirmRenameAction(context, node, typedString, snackbarShower, actionNodeCallback)
            }
            .show()
    }

    /**
     * Shows an error in a dialog and updates the input text field UI in consequence.
     *
     * @param typeText  The input text field.
     * @param errorText The text field to show the error.
     * @param error     Text to show as error.
     */
    @SuppressLint("UseCompatLoadingForColorStateLists")
    private fun showDialogError(typeText: EditText?, errorText: TextView?, error: String) {
        if (typeText != null) {
            setErrorAwareInputAppearance(typeText, true)
        }

        typeText?.requestFocus()

        errorText?.apply {
            visibility = VISIBLE
            text = error
        }
    }

    /**
     * Hides an error from a dialog and updates the input text field UI in consequence.
     *
     * @param typeText  The input text field.
     * @param errorText The text field to hide the error.
     */
    @SuppressLint("UseCompatLoadingForColorStateLists")
    private fun quitDialogError(typeText: EditText?, errorText: TextView?) {
        if (typeText != null) {
            setErrorAwareInputAppearance(typeText, false)
        }

        typeText?.requestFocus()
        errorText?.visibility = GONE
    }

    /**
     * Checks if the newTextFileDialog is shown. If so, saves it's state on outState.
     *
     * @param newTextFileDialog The dialog to check.
     * @param outState          Bundle where the state of the dialog will be save.
     */
    @JvmStatic
    fun checkNewTextFileDialogState(newTextFileDialog: AlertDialog?, outState: Bundle) {
        val isNewTextFileDialogShown = newTextFileDialog != null && newTextFileDialog.isShowing

        if (isNewTextFileDialogShown) {
            outState.putBoolean(IS_NEW_TEXT_FILE_SHOWN, true)
            val typeText = newTextFileDialog?.findViewById<EmojiEditText>(R.id.type_text)

            if (typeText != null) {
                outState.putString(NEW_TEXT_FILE_TEXT, typeText.text.toString())
            }
        }
    }

    /**
     * Move a node into rubbish bin, or remove it if it's already moved into rubbish bin.
     *
     * @param handle handle of the node
     * @param activity Android activity
     * @param snackbarShower interface to show snackbar
     */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun moveToRubbishOrRemove(
        handle: Long,
        activity: Activity,
        snackbarShower: SnackbarShower
    ) {
        val megaApi = MegaApplication.getInstance().megaApi

        if (!isOnline(activity)) {
            snackbarShower.showSnackbar(getString(R.string.error_server_connection_problem))
            return
        }

        val node = megaApi.getNodeByHandle(handle) ?: return
        val rubbishNode = megaApi.rubbishNode

        if (rubbishNode.handle != getRootParentNode(node).handle) {
            MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
                .setMessage(getString(R.string.confirmation_move_to_rubbish))
                .setPositiveButton(getString(R.string.general_move)) { _, _ ->
                    val progress = MegaProgressDialogUtil.createProgressDialog(
                        activity,
                        getString(R.string.context_move_to_trash)
                    )

                    megaApi.moveNode(
                        node, rubbishNode,
                        MoveListener { success, isForeignOverQuota ->
                            progress.dismiss()

                            when {
                                success -> activity.finish()
                                isForeignOverQuota -> showForeignStorageOverQuotaWarningDialog(
                                    activity
                                )
                                else -> snackbarShower.showSnackbar(getString(R.string.context_no_moved))
                            }
                        })

                    progress.show()
                }
                .setNegativeButton(getString(R.string.general_cancel), null)
                .show()
        } else {
            MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
                .setMessage(getString(R.string.confirmation_delete_from_mega))
                .setPositiveButton(getString(R.string.general_remove)) { _, _ ->
                    val progress = MegaProgressDialogUtil.createProgressDialog(
                        activity,
                        getString(R.string.context_delete_from_mega)
                    )

                    megaApi.remove(node, RemoveListener {
                        progress.dismiss()

                        if (it) {
                            snackbarShower.showSnackbar(getString(R.string.context_correctly_removed))
                            activity.finish()
                        } else {
                            snackbarShower.showSnackbar(getString(R.string.context_no_removed))
                        }
                    })

                    progress.show()
                }
                .setNegativeButton(getString(R.string.general_cancel), null)
                .show()
        }
    }

    /**
     * Checks if exists a node with the typed name within the same parent folder:
     * - If the action is rename, then gets the parent node.
     * - If not:
     *      * If the received node is null, then the parent node is the root.
     *      * If not, then the received node is the parent.
     *
     * @param typedString    Typed text to set as the new node name.
     * @param isRenameAction True if the action is rename, false otherwise.
     * @param node           Node to rename or parent where new file/folder should be created.
     */
    @JvmStatic
    fun nameAlreadyExists(
        typedString: String,
        isRenameAction: Boolean,
        node: MegaNode?
    ): Boolean {
        val megaApi = MegaApplication.getInstance().megaApi

        val parentNode = when {
            isRenameAction -> megaApi.getParentNode(node)
            node == null -> megaApi.rootNode
            else -> node
        } ?: return false

        val existingNode = megaApi.getChildren(parentNode)?.find { childNode ->
            childNode.name == typedString
        }

        return existingNode != null
    }

    /**
     * Show the warning dialog when moving or deleting folders with "My backup" folder
     * @param handleList handle list of the nodes that selected
     * @param pNodeBackup Can not be null, for the actions:
     * ACTION_COPY_TO_BACKUP            - The destination node belongs to "My backups"
     * ACTION_BACKUP_REMOVE             - The root of backup node in handleList,
     *                                    otherwise one of the node in handleList
     * ACTION_MOVE_TO_BACKUP            - The destination node belongs to "My backups"
     * ACTION_BACKUP_MOVE               - The root of backup node in handleList,
     *                                    otherwise one of the node in handleList
     * ACTION_MENU_BACKUP_SHARE_FOLDER  - The root of backup node
     * ACTION_BACKUP_SHARE_FOLDER       - The node that select for share.
     * ACTION_BACKUP_FAB                - if the folder is empty, pNodeBackup is the parent node of empty folder,
     *                                    otherwise one of the node in handleList.
     * @param nodeType the type of the backup node - BACKUP_NONE / BACKUP_ROOT / BACKUP_DEVICE / BACKUP_FOLDER / BACKUP_FOLDER_CHILD
     * @param actionType Indicates the action to backup folder or file:
     *                                  - ACTION_COPY_TO_BACKUP
     *                                  - ACTION_BACKUP_REMOVE
     *                                  - ACTION_MOVE_TO_BACKUP
     *                                  - ACTION_BACKUP_MOVE
     *                                  - ACTION_MENU_BACKUP_SHARE_FOLDER
     *                                  - ACTION_BACKUP_SHARE_FOLDER
     *                                  - ACTION_BACKUP_FAB
     */
    @JvmStatic
    fun showTipDialogWithBackup(
        activity: Activity,
        actionBackupNodeCallback: ActionBackupNodeCallback,
        handleList: ArrayList<Long>?,
        pNodeBackup: MegaNode,
        nodeType: Int,
        actionType: Int
    ): AlertDialog {

        logDebug("MyBackup + actWithBackupTips nodeType = $nodeType")
        logDebug("MyBackup + actWithBackupTips actionType = $actionType")
        if (handleList != null) {
            logDebug("MyBackup + handleList.size = " + handleList.size)
        }

        val dialogClickListener =
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                when (which) {
                    BUTTON_POSITIVE -> {
                        if(actionType == ACTION_BACKUP_SHARE_FOLDER
                            || actionType == ACTION_MENU_BACKUP_SHARE_FOLDER
                            || actionType == ACTION_BACKUP_SHARE
                            || (actionType == ACTION_COPY_TO_BACKUP && (nodeType == BACKUP_ROOT || nodeType == BACKUP_DEVICE))
                            || (actionType == ACTION_MOVE_TO_BACKUP && (nodeType == BACKUP_ROOT || nodeType == BACKUP_DEVICE))
                            || (actionType == ACTION_BACKUP_FAB && nodeType != BACKUP_FOLDER_CHILD)) {
                            logDebug("MyBackup + showTipDialogWithBackup / actionExecute")
                            actionBackupNodeCallback.actionExecute(
                                handleList,
                                pNodeBackup,
                                nodeType,
                                actionType)
                        } else {
                            logDebug("MyBackup + showTipDialogWithBackup / actionConfirmed")
                            actionBackupNodeCallback.actionConfirmed(
                                handleList,
                                pNodeBackup,
                                nodeType,
                                actionType
                            )
                        }
                    }
                    BUTTON_NEGATIVE -> {
                        logDebug("MyBackup + showTipDialogWithBackup / actionCancel")
                        actionBackupNodeCallback.actionCancel(dialog, actionType)
                    }
                }
            }
        val layout: LayoutInflater = activity.layoutInflater
        val view = layout.inflate(R.layout.dialog_backup_operate_tip, null)
        val tvTitle = view.findViewById<TextView>(R.id.title)
        val tvContent = view.findViewById<TextView>(R.id.backup_tip_content)
        val nodeName = pNodeBackup.name

        when (actionType) {
            ACTION_BACKUP_FAB -> {
                if(nodeType == BACKUP_DEVICE) {
                    tvTitle.text = getString(R.string.backup_add_item_title)
                    tvContent.setText(R.string.backup_add_item_to_root_text)
                } else {
                    val displayName = getString(R.string.backup_add_confirm_title, nodeName)
                    tvTitle.text = displayName
                    tvContent.setText(R.string.backup_add_item_text)
                }
            }
            ACTION_COPY_TO_BACKUP, ACTION_MOVE_TO_BACKUP -> {
                if (nodeType == BACKUP_ROOT || nodeType == BACKUP_DEVICE || nodeType == BACKUP_FOLDER) {
                    tvTitle.text = getString(R.string.backup_add_item_title)
                    tvContent.setText(R.string.backup_add_item_to_root_text)
                } else {
                    val displayName = getString(R.string.backup_add_confirm_title, nodeName)
                    tvTitle.text = displayName
                    tvContent.setText(R.string.backup_add_item_text)
                }
            }
            ACTION_BACKUP_MOVE -> {
                if (nodeType == BACKUP_ROOT) {
                    // Move root folder of backup
                    val displayName = getString(R.string.backup_move_folder_title, nodeName)
                    tvTitle.text = displayName
                    tvContent.setText(R.string.backup_move_root_folder)

                } else if (nodeType == BACKUP_DEVICE || nodeType == BACKUP_FOLDER || nodeType == BACKUP_FOLDER_CHILD) {
                    // Move sub folder of backup
                    if (handleList != null) {
                        tvTitle.text = if (handleList.size == 1) {
                            getString(R.string.backup_move_folder_title, nodeName)
                        } else {
                            getString(R.string.backup_move_multiple_folder_title)
                        }
                    }
                    tvContent.setText(R.string.backup_move_sub_folder)
                } else {
                    logDebug("Not in the backup folder")
                }
            }
            ACTION_BACKUP_REMOVE -> {
                if (nodeType == BACKUP_ROOT) {
                    // Move root folder of backup to rubbish bin
                    val displayName = getString(R.string.backup_remove_folder_title, nodeName)
                    tvTitle.text = displayName
                    tvContent.setText(R.string.backup_remove_root_folder)
                } else if (nodeType == BACKUP_DEVICE || nodeType == BACKUP_FOLDER || nodeType == BACKUP_FOLDER_CHILD) {
                    // Move sub folder of backup to rubbish bin
                    if (handleList != null) {
                        tvTitle.text = if (handleList.size == 1) {
                            getString(R.string.backup_remove_folder_title, nodeName)
                        } else {
                            getString(R.string.backup_remove_multiple_folder_title)
                        }
                    }
                    tvContent.setText(R.string.backup_remove_sub_folder)
                } else {
                    logDebug("Not in the backup folder")
                }
            }
            ACTION_BACKUP_SHARE, ACTION_BACKUP_SHARE_FOLDER, ACTION_MENU_BACKUP_SHARE_FOLDER -> {
                tvTitle.setText(R.string.backup_share_permission_title)
                tvContent.setText(R.string.backup_share_permission_text)
                handleList?.let {
                    val nodeSize = it.size
                    if(nodeSize > 1) {
                        when (nodeType) {
                            BACKUP_ROOT -> {
                                tvContent.setText(R.string.backup_share_with_root_permission_text)
                            }
                            else -> tvContent.setText(R.string.backup_multi_share_permission_text)
                        }
                    }
                }
            }
        }
        val builder = MaterialAlertDialogBuilder(activity)
            .setView(view)
        when (actionType) {
            ACTION_BACKUP_SHARE, ACTION_BACKUP_SHARE_FOLDER, ACTION_MENU_BACKUP_SHARE_FOLDER -> {
                if (handleList!=null) {
                    val nodeSize = handleList.size
                    if(nodeSize > 1 && nodeType == BACKUP_ROOT) {
                        builder.setPositiveButton(
                            getString(R.string.general_positive_button),
                            dialogClickListener
                        )
                        builder.setNegativeButton(
                            getString(R.string.general_cancel),
                            dialogClickListener
                        )
                    } else {
                        builder.setPositiveButton(
                            getString(R.string.button_permission_info),
                            dialogClickListener
                        )
                    }
                } else {
                    builder.setPositiveButton(
                        getString(R.string.button_permission_info),
                        dialogClickListener
                    )
                }
            }
            else -> {
                builder.setPositiveButton(
                    getString(R.string.button_continue),
                    dialogClickListener
                )
                builder.setNegativeButton(
                    getString(R.string.general_cancel),
                    dialogClickListener
                )
            }
        }
        val dialog = builder.show()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    /**
     * Show the confirm dialog when moving or deleting folders with "My backup" folder
     * @param handleList handle list of the nodes that selected
     * @param pNodeBackup Can not be null, for the actions:
     * ACTION_COPY_TO_BACKUP            - The destination node belongs to "My backups"
     * ACTION_BACKUP_REMOVE             - The root of backup node in handleList,
     *                                    otherwise one of the node in handleList
     * ACTION_MOVE_TO_BACKUP            - The destination node belongs to "My backups"
     * ACTION_BACKUP_MOVE               - The root of backup node in handleList,
     *                                    otherwise one of the node in handleList
     * ACTION_MENU_BACKUP_SHARE_FOLDER  - The root of backup node
     * ACTION_BACKUP_SHARE_FOLDER       - The node that select for share.
     * ACTION_BACKUP_FAB                - if the folder is empty, pNodeBackup is the parent node of empty folder,
     *                                    otherwise one of the node in handleList.
     * @param nodeType the type of the backup node - BACKUP_NONE / BACKUP_ROOT / BACKUP_DEVICE / BACKUP_FOLDER / BACKUP_FOLDER_CHILD
     * @param actionType Indicates the action to backup folder or file:
     *                                  - ACTION_COPY_TO_BACKUP
     *                                  - ACTION_BACKUP_REMOVE
     *                                  - ACTION_MOVE_TO_BACKUP
     *                                  - ACTION_BACKUP_MOVE
     *                                  - ACTION_MENU_BACKUP_SHARE_FOLDER
     *                                  - ACTION_BACKUP_SHARE_FOLDER
     *                                  - ACTION_BACKUP_FAB
     */
    @JvmStatic
    fun showConfirmDialogWithBackup(
        activity: Activity,
        actionBackupNodeCallback: ActionBackupNodeCallback,
        handleList: ArrayList<Long>?,
        pNodeBackup: MegaNode,
        nodeType: Int,
        actionType: Int
    ): AlertDialog {
        val layout: LayoutInflater = activity.layoutInflater
        val view = layout.inflate(R.layout.dialog_backup_action_confirm, null)
        val tvTitle = view.findViewById<TextView>(R.id.title)
        val tvConfirmString = view.findViewById<TextView>(R.id.confirm_string)
        val editTextLayout: TextInputLayout = view.findViewById(R.id.confirm_text_layout)
        val editText: TextInputEditText = view.findViewById(R.id.confirm_text)
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                editTextLayout.error = null
                editTextLayout.setHintTextAppearance(R.style.TextAppearance_Design_Hint)
            }
        })

        var checkStr = getString(R.string.backup_disable_confirm)
        var confirmInfo = getString(R.string.backup_action_confirm, checkStr)
        tvConfirmString.text = confirmInfo


        val nodeName = pNodeBackup.name

        when (actionType) {
            ACTION_COPY_TO_BACKUP, ACTION_MOVE_TO_BACKUP, ACTION_BACKUP_FAB -> {
                // Add
                tvTitle.text = getString(R.string.backup_add_confirm_title, nodeName)
            }
            ACTION_BACKUP_MOVE -> {
                if (nodeType == BACKUP_ROOT) {
                    // Move root folder of backup
                    tvTitle.text = getString(R.string.backup_move_folder_title, nodeName)
                    checkStr = getString(R.string.backup_move_confirm)
                    confirmInfo = getString(R.string.backup_action_confirm, checkStr)
                    tvConfirmString.text = confirmInfo
                    editTextLayout.hint = checkStr
                } else if (nodeType == BACKUP_DEVICE || nodeType == BACKUP_FOLDER || nodeType == BACKUP_FOLDER_CHILD) {
                    // Move sub folder of backup
                    if (handleList != null) {
                        tvTitle.text = if (handleList.size == 1) {
                            getString(R.string.backup_move_folder_title, nodeName)
                        } else {
                            getString(R.string.backup_move_multiple_folder_title)
                        }
                    }
                } else {
                    logDebug("Not in the backup folder")
                }
            }
            ACTION_BACKUP_REMOVE -> {
                if (nodeType == BACKUP_ROOT) {
                    // Move root folder of backup to rubbish bin
                    tvTitle.text = getString(R.string.backup_remove_folder_title, nodeName)
                } else if (nodeType == BACKUP_DEVICE || nodeType == BACKUP_FOLDER || nodeType == BACKUP_FOLDER_CHILD) {
                    // Move sub folder of backup to rubbish bin
                    if (handleList != null) {
                        tvTitle.text = if (handleList.size == 1) {
                            getString(R.string.backup_remove_folder_title, nodeName)
                        } else {
                            getString(R.string.backup_remove_multiple_folder_title)
                        }
                    }
                } else {
                    logDebug("Not in the backup folder")
                }
            }
        }

        val builder = MaterialAlertDialogBuilder(activity)
            .setView(view)
            .setPositiveButton(getString(R.string.general_move), null)
            .setNegativeButton(getString(R.string.general_cancel), null)

        if (handleList == null) {
            builder.setPositiveButton(getString(R.string.general_add), null)
        } else {
            builder.setPositiveButton(getString(R.string.general_move), null)
        }
        val dialog = builder.create()
        dialog.setOnShowListener {
            val button =
                dialog.getButton(BUTTON_POSITIVE)
            button.setOnClickListener {
                val strEditText =
                    Objects.requireNonNull(editText.text)
                        .toString()
                when (actionType) {
                    ACTION_BACKUP_MOVE -> {
                        if (nodeType == BACKUP_ROOT && getString(R.string.backup_move_confirm) == strEditText) {
                            logDebug("MyBackup + showConfirmDialogWithBackup / actionExecute")
                            actionBackupNodeCallback.actionExecute(
                                handleList,
                                pNodeBackup,
                                nodeType,
                                actionType
                            )
                            //Dismiss once everything is OK.
                            dialog.dismiss()
                        } else if (nodeType == BACKUP_DEVICE || nodeType == BACKUP_FOLDER || nodeType == BACKUP_FOLDER_CHILD) {
                            if (getString(R.string.backup_disable_confirm) == strEditText) {
                                logDebug("MyBackup + showConfirmDialogWithBackup / actionExecute")
                                actionBackupNodeCallback.actionExecute(
                                    handleList,
                                    pNodeBackup,
                                    nodeType,
                                    actionType
                                )
                                //Dismiss once everything is OK.
                                dialog.dismiss()
                            } else {
                                showErrorInfo(editText, editTextLayout)
                            }
                        } else {
                            showErrorInfo(editText, editTextLayout)
                        }
                    }
                    else -> {
                        if (getString(R.string.backup_disable_confirm) == strEditText) {
                            logDebug("MyBackup + showConfirmDialogWithBackup / actionExecute")
                            actionBackupNodeCallback.actionExecute(
                                handleList,
                                pNodeBackup,
                                nodeType,
                                actionType
                            )
                            //Dismiss once everything is OK.
                            dialog.dismiss()
                        } else {
                            showErrorInfo(editText, editTextLayout)
                        }
                    }
                }
            }

            val buttonCancel =
                dialog.getButton(BUTTON_NEGATIVE)
            buttonCancel.setOnClickListener {
                logDebug("MyBackup + showConfirmDialogWithBackup / actionCancel")
                actionBackupNodeCallback.actionCancel(dialog, actionType)
                dialog.dismiss()
            }

        }
        dialog.show()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    private fun showErrorInfo(
        editText: TextInputEditText,
        editTextLayout: TextInputLayout
    ) {
        editText.requestFocus()
        editTextLayout.error =
            getString(R.string.error_backup_confirm_dont_match)
        editTextLayout.setHintTextAppearance(R.style.TextAppearance_InputHint_Error)
    }
}
