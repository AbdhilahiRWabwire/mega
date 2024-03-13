@file:OptIn(ExperimentalMaterialApi::class)

package mega.privacy.android.app.presentation.imagepreview.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.R as Rpack
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MegaNodeUtil.getInfoText
import mega.privacy.android.core.R.drawable.link_ic
import mega.privacy.android.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.core.ui.controls.text.MiddleEllipsisText
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.legacy.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionListTile
import nz.mega.sdk.MegaNode

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ImagePreviewBottomSheet(
    modalSheetState: ModalBottomSheetState,
    imageNode: ImageNode,
    isAvailableOffline: Boolean = false,
    showInfoMenu: suspend (ImageNode) -> Boolean,
    showFavouriteMenu: suspend (ImageNode) -> Boolean,
    showLabelMenu: suspend (ImageNode) -> Boolean,
    showDisputeMenu: suspend (ImageNode) -> Boolean,
    showOpenWithMenu: suspend (ImageNode) -> Boolean,
    showForwardMenu: suspend (ImageNode) -> Boolean,
    showSaveToDeviceMenu: suspend (ImageNode) -> Boolean,
    showImportMenu: suspend (ImageNode) -> Boolean,
    showGetLinkMenu: suspend (ImageNode) -> Boolean,
    showSendToChatMenu: suspend (ImageNode) -> Boolean,
    showShareMenu: suspend (ImageNode) -> Boolean,
    showRenameMenu: suspend (ImageNode) -> Boolean,
    showHideMenu: suspend (ImageNode) -> Boolean,
    showUnhideMenu: suspend (ImageNode) -> Boolean,
    showMoveMenu: suspend (ImageNode) -> Boolean,
    showCopyMenu: suspend (ImageNode) -> Boolean,
    showRestoreMenu: suspend (ImageNode) -> Boolean,
    showRemoveMenu: suspend (ImageNode) -> Boolean,
    showAvailableOfflineMenu: suspend (ImageNode) -> Boolean,
    showRemoveOfflineMenu: suspend (ImageNode) -> Boolean,
    showMoveToRubbishBin: suspend (ImageNode) -> Boolean,
    downloadImage: suspend (ImageNode) -> Flow<ImageResult>,
    getImageThumbnailPath: suspend (ImageResult?) -> String?,
    onClickInfo: () -> Unit = {},
    onClickFavourite: () -> Unit = {},
    onClickLabel: () -> Unit = {},
    onClickDispute: () -> Unit = {},
    onClickOpenWith: () -> Unit = {},
    onClickForward: () -> Unit = {},
    onClickSaveToDevice: () -> Unit = {},
    onClickImport: () -> Unit = {},
    onSwitchAvailableOffline: (checked: Boolean) -> Unit = {},
    onClickGetLink: () -> Unit = {},
    onClickRemoveLink: () -> Unit = {},
    onClickSendToChat: () -> Unit = {},
    onClickShare: () -> Unit = {},
    onClickRename: () -> Unit = {},
    onClickHide: () -> Unit = {},
    onClickUnhide: () -> Unit = {},
    onClickMove: () -> Unit = {},
    onClickCopy: () -> Unit = {},
    onClickRestore: () -> Unit = {},
    onClickRemove: () -> Unit = {},
    onClickMoveToRubbishBin: () -> Unit = {},
) {
    val context = LocalContext.current

    BottomSheet(
        modalSheetState = modalSheetState,
        sheetHeader = {
            ImagePreviewMenuActionHeader(
                imageNode = imageNode,
                downloadImage = downloadImage,
                getImageThumbnailPath = getImageThumbnailPath,
            )
        },
        sheetBody = {
            val labelColorResId = remember(imageNode) {
                MegaNodeUtil.getNodeLabelColor(imageNode.label)
            }

            val labelColorText = remember(imageNode) {
                MegaNodeUtil.getNodeLabelText(imageNode.label, context)
            }

            val isInfoMenuVisible by produceState(false, imageNode) {
                value = showInfoMenu(imageNode)
            }

            val isFavouriteMenuVisible by produceState(false, imageNode) {
                value = showFavouriteMenu(imageNode)
            }

            val isLabelMenuVisible by produceState(false, imageNode) {
                value = showLabelMenu(imageNode)
            }

            val isDisputeMenuVisible by produceState(false, imageNode) {
                value = showDisputeMenu(imageNode)
            }

            val isOpenWithMenuVisible by produceState(false, imageNode) {
                value = showOpenWithMenu(imageNode)
            }

            val isForwardMenuVisible by produceState(false, imageNode) {
                value = showForwardMenu(imageNode)
            }

            val isSaveToDeviceMenuVisible by produceState(false, imageNode) {
                value = showSaveToDeviceMenu(imageNode)
            }

            val isImportMenuVisible by produceState(false, imageNode) {
                value = showImportMenu(imageNode)
            }

            val isGetLinkMenuVisible by produceState(false, imageNode) {
                value = showGetLinkMenu(imageNode)
            }

            val isSendToChatMenuVisible by produceState(false, imageNode) {
                value = showSendToChatMenu(imageNode)
            }

            val isShareMenuVisible by produceState(false, imageNode) {
                value = showShareMenu(imageNode)
            }

            val isRenameMenuVisible by produceState(false, imageNode) {
                value = showRenameMenu(imageNode)
            }

            val isHideMenuVisible by produceState(false, imageNode) {
                value = showHideMenu(imageNode)
            }

            val isUnhideMenuVisible by produceState(false, imageNode) {
                value = showUnhideMenu(imageNode)
            }

            val isMoveMenuVisible by produceState(false, imageNode) {
                value = showMoveMenu(imageNode)
            }

            val isCopyMenuVisible by produceState(false, imageNode) {
                value = showCopyMenu(imageNode)
            }

            val isRestoreMenuVisible by produceState(false, imageNode) {
                value = showRestoreMenu(imageNode)
            }

            val isRemoveMenuVisible by produceState(false, imageNode) {
                value = showRemoveMenu(imageNode)
            }

            val isAvailableOfflineMenuVisible by produceState(false, imageNode) {
                value = showAvailableOfflineMenu(imageNode)
            }

            val isRemoveOfflineMenuVisible by produceState(false, imageNode) {
                value = showRemoveOfflineMenu(imageNode)
            }

            val isMoveToRubbishBinMenuVisible by produceState(false, imageNode) {
                value = showMoveToRubbishBin(imageNode)
            }

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                if (isInfoMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.info_ic),
                        text = stringResource(id = R.string.general_info),
                        onActionClicked = onClickInfo,
                        addSeparator = false,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_INFO),
                    )
                }
                if (isFavouriteMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(
                            id = if (imageNode.isFavourite) {
                                R.drawable.ic_remove_favourite
                            } else {
                                R.drawable.ic_add_favourite
                            }
                        ),
                        text = if (imageNode.isFavourite) {
                            stringResource(id = R.string.file_properties_unfavourite)
                        } else {
                            stringResource(id = R.string.file_properties_favourite)
                        },
                        onActionClicked = onClickFavourite,
                        addSeparator = false,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_FAVOURITE),
                    )
                }

                if (isLabelMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_label),
                        text = stringResource(id = R.string.file_properties_label),
                        onActionClicked = onClickLabel,
                        addSeparator = false,
                        trailingItem = {
                            if (imageNode.label != MegaNode.NODE_LBL_UNKNOWN) {
                                Row {
                                    Text(
                                        text = labelColorText,
                                        color = colorResource(id = labelColorResId),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterVertically)
                                            .padding(start = 4.dp)
                                            .size(10.dp)
                                            .background(
                                                shape = CircleShape,
                                                color = colorResource(id = labelColorResId),
                                            ),
                                    )
                                }
                            }
                        },
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_LABEL),
                    )
                }

                Divider(modifier = Modifier.padding(start = 72.dp))

                if (isDisputeMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_taken_down_bottom_sheet),
                        text = stringResource(id = R.string.dispute_takendown_file),
                        onActionClicked = onClickDispute,
                        addSeparator = false,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_DISPUTE),
                    )
                }

                if (isOpenWithMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_open_with),
                        text = stringResource(id = R.string.external_play),
                        onActionClicked = onClickOpenWith,
                        addSeparator = false,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_OPEN_WITH),
                    )
                }

                Divider(modifier = Modifier.padding(start = 72.dp))

                if (isForwardMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_forward),
                        text = stringResource(id = R.string.forward_menu_item),
                        onActionClicked = onClickForward,
                        addSeparator = false,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_FORWARD),
                    )
                }

                if (isSaveToDeviceMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_save_to_device),
                        text = stringResource(id = R.string.general_save_to_device),
                        onActionClicked = onClickSaveToDevice,
                        addSeparator = false,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_SAVE_TO_DEVICE),
                    )
                }

                if (isImportMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_import_to_cloud_white),
                        text = stringResource(id = R.string.general_import),
                        onActionClicked = onClickImport,
                        addSeparator = false,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_IMPORT),
                    )
                }

                if (isAvailableOfflineMenuVisible) {
                    MenuActionListTile(
                        text = stringResource(id = R.string.file_properties_available_offline),
                        icon = painterResource(id = R.drawable.ic_save_offline),
                        addSeparator = false,
                        modifier = Modifier.testTag(
                            IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_AVAILABLE_OFFLINE
                        ),
                    ) {
                        MegaSwitch(
                            checked = isAvailableOffline,
                            onCheckedChange = onSwitchAvailableOffline,
                        )
                    }
                }

                Divider(modifier = Modifier.padding(start = 72.dp))

                if (isGetLinkMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = link_ic),
                        text = if (imageNode.exportedData != null) {
                            stringResource(id = R.string.edit_link_option)
                        } else {
                            LocalContext.current.resources.getQuantityString(
                                R.plurals.get_links,
                                1,
                            )
                        },
                        onActionClicked = onClickGetLink,
                        addSeparator = false,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_GET_LINK),
                    )
                }

                if (imageNode.exportedData != null) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_remove_link),
                        text = stringResource(id = R.string.context_remove_link_menu),
                        onActionClicked = onClickRemoveLink,
                        addSeparator = false,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_REMOVE_LINK),
                    )
                }

                if (isSendToChatMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_send_to_contact),
                        text = stringResource(id = R.string.context_send_file_to_chat),
                        onActionClicked = onClickSendToChat,
                        addSeparator = false,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_SEND_TO_CHAT),
                    )
                }

                if (isShareMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_social_share_white),
                        text = stringResource(id = R.string.general_share),
                        onActionClicked = onClickShare,
                        addSeparator = false,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_SHARE),
                    )
                }

                Divider(modifier = Modifier.padding(start = 72.dp))

                if (isRenameMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_rename),
                        text = stringResource(id = R.string.context_rename),
                        onActionClicked = onClickRename,
                        addSeparator = false,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_RENAME),
                    )
                }

                if (isHideMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = Rpack.drawable.ic_menu_hide),
                        text = stringResource(id = R.string.general_hide_node),
                        onActionClicked = onClickHide,
                        addSeparator = false,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_HIDE),
                    )
                }

                if (isUnhideMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = Rpack.drawable.ic_menu_unhide),
                        text = stringResource(id = R.string.general_unhide_node),
                        onActionClicked = onClickUnhide,
                        addSeparator = false,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_UNHIDE),
                    )
                }

                if (isMoveMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_move),
                        text = stringResource(id = R.string.general_move),
                        onActionClicked = onClickMove,
                        addSeparator = false,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_MOVE),
                    )
                }

                Divider(modifier = Modifier.padding(start = 72.dp))

                if (isCopyMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_menu_copy),
                        text = stringResource(id = R.string.context_copy),
                        onActionClicked = onClickCopy,
                        addSeparator = false,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_COPY),
                    )
                }

                if (isRestoreMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_restore),
                        text = stringResource(id = R.string.context_restore),
                        onActionClicked = onClickRestore,
                        addSeparator = false,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_RESTORE),
                    )
                }

                if (isRemoveMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_remove),
                        text = stringResource(id = R.string.context_remove),
                        isDestructive = true,
                        onActionClicked = onClickRemove,
                        addSeparator = false,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_REMOVE),
                    )
                }

                if (isRemoveOfflineMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_remove),
                        text = stringResource(id = R.string.context_delete_offline),
                        onActionClicked = { onSwitchAvailableOffline(false) },
                        isDestructive = true,
                        addSeparator = false,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_REMOVE_OFFLINE),
                    )
                }

                if (isMoveToRubbishBinMenuVisible) {
                    MenuActionListTile(
                        icon = painterResource(id = R.drawable.ic_rubbish_bin),
                        text = stringResource(id = R.string.context_move_to_trash),
                        onActionClicked = onClickMoveToRubbishBin,
                        addSeparator = false,
                        isDestructive = true,
                        modifier = Modifier.testTag(
                            IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_MOVE_TO_RUBBISH_BIN
                        ),
                    )
                }
            }
        },
    )
}

@Composable
private fun ImagePreviewMenuActionHeader(
    imageNode: ImageNode,
    downloadImage: suspend (ImageNode) -> Flow<ImageResult>,
    getImageThumbnailPath: suspend (ImageResult?) -> String?,
) {
    val context = LocalContext.current

    val imageThumbnailPath by produceState<String?>(null, imageNode) {
        downloadImage(imageNode).collectLatest { imageResult ->
            value = getImageThumbnailPath(imageResult)
        }
    }

    val imageInfo = remember(imageNode) {
        MegaNode.unserialize(imageNode.serializedData)?.getInfoText(context)
    }

    Row(
        modifier = Modifier.padding(all = 16.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(imageThumbnailPath)
                .crossfade(true).build(),
            placeholder = painterResource(id = android.R.drawable.ic_menu_camera),
            error = painterResource(id = android.R.drawable.ic_menu_camera),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(40.dp, 40.dp)
        )
        Column(
            modifier = Modifier.padding(start = 16.dp)
        ) {
            MiddleEllipsisText(
                text = imageNode.name,
                color = TextColor.Primary,
                modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_HEADER_TEXT_NAME),
            )
            MiddleEllipsisText(
                text = imageInfo.orEmpty(),
                color = TextColor.Secondary,
                modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_SHEET_HEADER_TEXT_INFO),
            )
        }
    }
}