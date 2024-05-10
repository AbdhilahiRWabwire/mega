@file:OptIn(
    ExperimentalFoundationApi::class, ExperimentalMaterialApi::class,
    ExperimentalComposeUiApi::class
)

package mega.privacy.android.app.presentation.imagepreview.view

import mega.privacy.android.icon.pack.R as iconPackR
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.BottomAppBar
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel
import mega.privacy.android.app.presentation.slideshow.view.PhotoBox
import mega.privacy.android.app.presentation.slideshow.view.PhotoState
import mega.privacy.android.app.presentation.slideshow.view.rememberPhotoState
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.controls.text.MiddleEllipsisText
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.extensions.black_white
import mega.privacy.android.core.ui.theme.extensions.white_alpha_070_grey_alpha_070
import mega.privacy.android.core.ui.theme.teal_200
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode

@Composable
internal fun ImagePreviewScreen(
    viewModel: ImagePreviewViewModel = viewModel(),
    onClickBack: () -> Unit,
    onClickVideoPlay: (ImageNode) -> Unit,
    onClickSlideshow: () -> Unit,
    onClickInfo: (ImageNode) -> Unit,
    onClickFavourite: (ImageNode) -> Unit = {},
    onClickLabel: (ImageNode) -> Unit = {},
    onClickOpenWith: (ImageNode) -> Unit = {},
    onClickSaveToDevice: (ImageNode) -> Unit = {},
    onClickImport: (ImageNode) -> Unit = {},
    onSwitchAvailableOffline: ((checked: Boolean, ImageNode) -> Unit)? = null,
    onClickGetLink: (ImageNode) -> Unit = {},
    onClickSendTo: (ImageNode) -> Unit = {},
    onClickShare: (ImageNode) -> Unit = {},
    onClickRename: (ImageNode) -> Unit = {},
    onClickHide: (ImageNode, AccountDetail?, Boolean?) -> Unit = { _, _, _ -> },
    onClickHideHelp: () -> Unit = {},
    onClickUnhide: (ImageNode) -> Unit = {},
    onClickMove: (ImageNode) -> Unit = {},
    onClickCopy: (ImageNode) -> Unit = {},
    onClickRestore: (ImageNode) -> Unit = {},
    onClickRemove: (ImageNode) -> Unit = {},
    onClickMoveToRubbishBin: (ImageNode) -> Unit = {},
) {
    val viewState by viewModel.state.collectAsStateWithLifecycle()
    val imageNodes = viewState.imageNodes

    if (viewState.isInitialized && imageNodes.isEmpty()) {
        LaunchedEffect(Unit) {
            onClickBack()
        }
    }

    val isHiddenNodesEnabled by produceState(initialValue = false) {
        value = viewModel.isHiddenNodesEnabled()
    }

    val currentImageNodeIndex = viewState.currentImageNodeIndex
    val accountDetail = viewState.accountDetail
    val isHiddenNodesOnboarded = viewState.isHiddenNodesOnboarded
    viewState.currentImageNode?.let { currentImageNode ->
        val isCurrentImageNodeAvailableOffline = viewState.isCurrentImageNodeAvailableOffline
        var showRemoveLinkDialog by rememberSaveable { mutableStateOf(false) }
        var showMoveToRubbishBinDialog by rememberSaveable { mutableStateOf(false) }
        var showRemoveDialog by rememberSaveable { mutableStateOf(false) }

        val inFullScreenMode = viewState.inFullScreenMode
        val systemUiController = rememberSystemUiController()
        LaunchedEffect(systemUiController, inFullScreenMode) {
            systemUiController.setStatusBarColor(
                color = if (inFullScreenMode) Color.Black else Color.Transparent,
                darkIcons = inFullScreenMode
            )
        }

        val scaffoldState = rememberScaffoldState()
        val isLight = MaterialTheme.colors.isLight
        val context = LocalContext.current
        val photoState = rememberPhotoState()
        val pagerState = rememberPagerState(
            initialPage = currentImageNodeIndex,
            initialPageOffsetFraction = 0f,
        ) {
            imageNodes.size
        }

        val coroutineScope = rememberCoroutineScope()
        val modalSheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            skipHalfExpanded = false,
        )

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collect { page ->
                viewState.imageNodes.getOrNull(page)?.let {
                    viewModel.setCurrentImageNodeIndex(page)
                    viewModel.setCurrentImageNode(it)
                    viewModel.setCurrentImageNodeAvailableOffline(it)
                }
            }
        }

        if (viewState.transferMessage.isNotEmpty()) {
            LaunchedEffect(Unit) {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = viewState.transferMessage,
                )
                viewModel.clearTransferMessage()
            }
        }

        if (viewState.resultMessage.isNotEmpty()) {
            LaunchedEffect(Unit) {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = viewState.resultMessage,
                )
                viewModel.clearResultMessage()
            }
        }

        if (viewState.showDeletedMessage) {
            LaunchedEffect(Unit) {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = context.getString(R.string.context_correctly_removed),
                )
                viewModel.hideDeletedMessage()
            }
        }

        if (showRemoveLinkDialog) {
            MegaAlertDialog(
                text = pluralStringResource(
                    id = R.plurals.remove_links_warning_text,
                    count = 1
                ),
                confirmButtonText = stringResource(id = R.string.general_remove),
                cancelButtonText = stringResource(id = R.string.general_cancel),
                onConfirm = {
                    viewModel.disableExport(currentImageNode)
                    hideBottomSheet(coroutineScope, modalSheetState)
                    showRemoveLinkDialog = false
                },
                onDismiss = {
                    showRemoveLinkDialog = false
                },
            )
        }

        if (showMoveToRubbishBinDialog) {
            MegaAlertDialog(
                text = stringResource(id = R.string.confirmation_move_to_rubbish),
                confirmButtonText = stringResource(id = R.string.general_move),
                cancelButtonText = stringResource(id = R.string.general_cancel),
                onConfirm = {
                    onClickMoveToRubbishBin(currentImageNode)
                    hideBottomSheet(coroutineScope, modalSheetState)
                    showMoveToRubbishBinDialog = false
                },
                onDismiss = {
                    showMoveToRubbishBinDialog = false
                }
            )
        }

        if (showRemoveDialog) {
            MegaAlertDialog(
                text = stringResource(id = R.string.confirmation_delete_from_mega),
                confirmButtonText = stringResource(id = R.string.general_remove),
                cancelButtonText = stringResource(id = R.string.general_cancel),
                onConfirm = {
                    onClickRemove(currentImageNode)
                    showRemoveDialog = false
                },
                onDismiss = {
                    showRemoveDialog = false
                }
            )
        }

        Scaffold(
            modifier = Modifier
                .semantics { testTagsAsResourceId = true },
            scaffoldState = scaffoldState,
            snackbarHost = { snackBarHostState ->
                SnackbarHost(
                    hostState = snackBarHostState,
                    snackbar = { snackBarData ->
                        Snackbar(
                            snackbarData = snackBarData,
                            actionOnNewLine = true,
                            backgroundColor = black.takeIf { isLight } ?: white,
                            actionColor = teal_200.takeIf { isLight } ?: teal_300,
                        )
                    }
                )
            },
            content = { innerPadding ->
                ImagePreviewContent(
                    modifier = Modifier
                        .background(
                            color = Color.Black.takeIf { inFullScreenMode }
                                ?: MaterialTheme.colors.surface,
                        )
                        .padding(innerPadding),
                    pagerState = pagerState,
                    imageNodes = imageNodes,
                    currentImageNodeIndex = currentImageNodeIndex,
                    currentImageNode = currentImageNode,
                    photoState = photoState,
                    downloadImage = viewModel::monitorImageResult,
                    getImagePath = viewModel::getHighestResolutionImagePath,
                    getErrorImagePath = viewModel::getFallbackImagePath,
                    onImageTap = { viewModel.switchFullScreenMode() },
                    onClickVideoPlay = onClickVideoPlay,
                    topAppBar = { imageNode ->
                        AnimatedVisibility(
                            visible = !inFullScreenMode,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            ImagePreviewTopBar(
                                imageNode = imageNode,
                                showSlideshowMenu = viewModel::isSlideshowMenuVisible,
                                showForwardMenu = viewModel::isForwardMenuVisible,
                                showSaveToDeviceMenu = viewModel::isSaveToDeviceMenuVisible,
                                showManageLinkMenu = viewModel::isGetLinkMenuVisible,
                                showSendToMenu = viewModel::isSendToChatMenuVisible,
                                showMoreMenu = viewModel::isMoreMenuVisible,
                                onClickBack = onClickBack,
                                onClickSlideshow = onClickSlideshow,
                                onClickForward = { onClickSendTo(imageNode) },
                                onClickSaveToDevice = { onClickSaveToDevice(imageNode) },
                                onClickGetLink = { onClickGetLink(imageNode) },
                                onClickSendTo = { onClickSendTo(imageNode) },
                                onClickMore = {
                                    coroutineScope.launch {
                                        modalSheetState.show()
                                    }
                                },
                            )
                        }
                    },
                    bottomAppBar = { currentImageNode, index ->
                        AnimatedVisibility(
                            visible = !inFullScreenMode,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            val photoIndexText = stringResource(
                                R.string.wizard_steps_indicator,
                                index + 1,
                                imageNodes.size
                            ).takeIf { imageNodes.size > 1 }

                            ImagePreviewBottomBar(
                                imageName = currentImageNode.name,
                                imageIndex = photoIndexText.orEmpty(),
                            )
                        }
                    }
                )

                ImagePreviewBottomSheet(
                    modalSheetState = modalSheetState,
                    imageNode = currentImageNode,
                    isAvailableOffline = isCurrentImageNodeAvailableOffline,
                    accountDetail = accountDetail,
                    isHiddenNodesEnabled = isHiddenNodesEnabled,
                    isHiddenNodesOnboarded = isHiddenNodesOnboarded,
                    showInfoMenu = viewModel::isInfoMenuVisible,
                    showFavouriteMenu = viewModel::isFavouriteMenuVisible,
                    showLabelMenu = viewModel::isLabelMenuVisible,
                    showDisputeMenu = viewModel::isDisputeMenuVisible,
                    showOpenWithMenu = viewModel::isOpenWithMenuVisible,
                    showForwardMenu = viewModel::isForwardMenuVisible,
                    showSaveToDeviceMenu = viewModel::isSaveToDeviceMenuVisible,
                    showImportMenu = viewModel::isImportMenuVisible,
                    showGetLinkMenu = viewModel::isGetLinkMenuVisible,
                    showSendToChatMenu = viewModel::isSendToChatMenuVisible,
                    showShareMenu = viewModel::isShareMenuVisible,
                    showRenameMenu = viewModel::isRenameMenuVisible,
                    showHideMenu = viewModel::isHideMenuVisible,
                    showUnhideMenu = viewModel::isUnhideMenuVisible,
                    forceHideHiddenMenus = viewModel::forceHideHiddenMenus,
                    showMoveMenu = viewModel::isMoveMenuVisible,
                    showCopyMenu = viewModel::isCopyMenuVisible,
                    showRestoreMenu = viewModel::isRestoreMenuVisible,
                    showRemoveMenu = viewModel::isRemoveMenuVisible,
                    showAvailableOfflineMenu = viewModel::isAvailableOfflineMenuVisible,
                    showRemoveOfflineMenu = viewModel::isRemoveOfflineMenuVisible,
                    showMoveToRubbishBin = viewModel::isMoveToRubbishBinMenuVisible,
                    downloadImage = viewModel::monitorImageResult,
                    getImageThumbnailPath = viewModel::getLowestResolutionImagePath,
                    onClickInfo = {
                        onClickInfo(currentImageNode)
                        hideBottomSheet(coroutineScope, modalSheetState)
                    },
                    onClickFavourite = {
                        onClickFavourite(currentImageNode)
                        hideBottomSheet(coroutineScope, modalSheetState)
                    },
                    onClickLabel = {
                        onClickLabel(currentImageNode)
                        hideBottomSheet(coroutineScope, modalSheetState)
                    },
                    onClickDispute = {},
                    onClickOpenWith = {
                        onClickOpenWith(currentImageNode)
                        hideBottomSheet(coroutineScope, modalSheetState)
                    },
                    onClickForward = {
                        onClickSendTo(currentImageNode)
                        hideBottomSheet(coroutineScope, modalSheetState)
                    },
                    onClickSaveToDevice = {
                        onClickSaveToDevice(currentImageNode)
                        hideBottomSheet(coroutineScope, modalSheetState)
                    },
                    onClickImport = {
                        onClickImport(currentImageNode)
                        hideBottomSheet(coroutineScope, modalSheetState)
                    },
                    onSwitchAvailableOffline = { checked ->
                        onSwitchAvailableOffline?.invoke(checked, currentImageNode)
                        hideBottomSheet(coroutineScope, modalSheetState)
                    },
                    onClickGetLink = {
                        onClickGetLink(currentImageNode)
                        hideBottomSheet(coroutineScope, modalSheetState)
                    },
                    onClickRemoveLink = {
                        if (!currentImageNode.isTakenDown) {
                            showRemoveLinkDialog = true
                        }
                        hideBottomSheet(coroutineScope, modalSheetState)
                    },
                    onClickSendToChat = {
                        onClickSendTo(currentImageNode)
                        hideBottomSheet(coroutineScope, modalSheetState)
                    },
                    onClickShare = {
                        onClickShare(currentImageNode)
                        hideBottomSheet(coroutineScope, modalSheetState)
                    },
                    onClickRename = {
                        onClickRename(currentImageNode)
                        hideBottomSheet(coroutineScope, modalSheetState)
                    },
                    onClickHide = {
                        onClickHide(currentImageNode, accountDetail, isHiddenNodesOnboarded)
                        hideBottomSheet(coroutineScope, modalSheetState)
                    },
                    onClickHideHelp = {
                        onClickHideHelp()
                        hideBottomSheet(coroutineScope, modalSheetState)
                    },
                    onClickUnhide = {
                        onClickUnhide(currentImageNode)
                        hideBottomSheet(coroutineScope, modalSheetState)
                    },
                    onClickMove = {
                        onClickMove(currentImageNode)
                        hideBottomSheet(coroutineScope, modalSheetState)
                    },
                    onClickCopy = {
                        onClickCopy(currentImageNode)
                        hideBottomSheet(coroutineScope, modalSheetState)
                    },
                    onClickRestore = {
                        onClickRestore(currentImageNode)
                        hideBottomSheet(coroutineScope, modalSheetState)
                    },
                    onClickRemove = {
                        showRemoveDialog = true
                        hideBottomSheet(coroutineScope, modalSheetState)
                    },
                    onClickMoveToRubbishBin = {
                        showMoveToRubbishBinDialog = true
                        hideBottomSheet(coroutineScope, modalSheetState)
                    },
                )
            },
        )
        StartTransferComponent(
            event = viewState.downloadEvent,
            onConsumeEvent = viewModel::consumeDownloadEvent,
            snackBarHostState = scaffoldState.snackbarHostState
        )
    }
}

private fun hideBottomSheet(
    coroutineScope: CoroutineScope,
    modalSheetState: ModalBottomSheetState,
) {
    coroutineScope.launch {
        modalSheetState.hide()
    }
}

@Composable
private fun ImagePreviewContent(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    imageNodes: List<ImageNode>,
    currentImageNodeIndex: Int,
    currentImageNode: ImageNode,
    photoState: PhotoState,
    onImageTap: () -> Unit,
    topAppBar: @Composable (ImageNode) -> Unit,
    bottomAppBar: @Composable (ImageNode, Int) -> Unit,
    downloadImage: suspend (ImageNode) -> Flow<ImageResult>,
    getImagePath: suspend (ImageResult?) -> String?,
    getErrorImagePath: suspend (ImageResult?) -> String?,
    onClickVideoPlay: (ImageNode) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = pagerState,
            beyondBoundsPageCount = minOf(5, imageNodes.size),
            key = { imageNodes.getOrNull(it)?.id?.longValue ?: -1L },
        ) { index ->

            val imageNode = imageNodes.getOrNull(index)
            if (imageNode != null) {
                val imageResultTriple by produceState<Triple<ImageResult?, String?, String?>>(
                    initialValue = Triple(null, null, null)
                ) {
                    downloadImage(imageNode).collectLatest { imageResult ->
                        value = Triple(
                            imageResult,
                            getImagePath(imageResult),
                            getErrorImagePath(imageResult)
                        )
                    }
                }

                val (imageResult, imagePath, errorImagePath) = imageResultTriple

                Box(modifier = Modifier.fillMaxSize()) {
                    val isVideo = imageNode.type is VideoFileTypeInfo
                    ImageContent(
                        fullSizePath = imageNode.run {
                            fullSizePath.takeIf {
                                imageNode.serializedData?.contains(
                                    "local"
                                ) == true
                            }
                        } ?: imagePath,
                        errorImagePath = imageNode.run {
                            fullSizePath.takeIf {
                                imageNode.serializedData?.contains(
                                    "local"
                                ) == true
                            }
                        } ?: errorImagePath,
                        photoState = photoState,
                        onImageTap = onImageTap,
                        enableZoom = !isVideo
                    )
                    if (isVideo) {
                        IconButton(
                            modifier = Modifier.align(Alignment.Center),
                            onClick = { onClickVideoPlay(imageNode) }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_play),
                                contentDescription = "Image Preview play video",
                                tint = Color.White,
                            )
                        }
                    }

                    val progress = imageResult?.getProgressPercentage() ?: 0L
                    if (progress in 1 until 100) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.BottomEnd),
                            progress = progress.toFloat() / 100,
                            color = MaterialTheme.colors.secondary,
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.TopCenter),
        ) {
            topAppBar(currentImageNode)
        }

        Box(
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.BottomCenter),
        ) {
            bottomAppBar(currentImageNode, currentImageNodeIndex)
        }
    }
}

@Composable
private fun ImageContent(
    fullSizePath: String?,
    errorImagePath: String?,
    photoState: PhotoState,
    onImageTap: () -> Unit,
    enableZoom: Boolean,
) {
    PhotoBox(
        modifier = Modifier.fillMaxSize(),
        state = photoState,
        enableZoom = enableZoom,
        onTap = {
            onImageTap()
        }
    ) {
        var imagePath by remember(fullSizePath) {
            mutableStateOf(fullSizePath)
        }

        val request = ImageRequest.Builder(LocalContext.current)
            .data(imagePath)
            .listener(
                onError = { _, _ ->
                    // when some image full size picture decoder throw exception, use preview/thumbnail instead
                    // detail see package coil.decode [BitmapFactoryDecoder] 79 line
                    imagePath = errorImagePath
                }
            )
            .crossfade(true)
            .build()
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ImagePreviewTopBar(
    modifier: Modifier = Modifier,
    imageNode: ImageNode,
    showSlideshowMenu: suspend (ImageNode) -> Boolean,
    showForwardMenu: suspend (ImageNode) -> Boolean,
    showSaveToDeviceMenu: suspend (ImageNode) -> Boolean,
    showManageLinkMenu: suspend (ImageNode) -> Boolean,
    showSendToMenu: suspend (ImageNode) -> Boolean,
    showMoreMenu: suspend (ImageNode) -> Boolean,
    onClickBack: () -> Unit,
    onClickSlideshow: () -> Unit,
    onClickForward: () -> Unit,
    onClickSaveToDevice: () -> Unit,
    onClickGetLink: () -> Unit,
    onClickSendTo: () -> Unit,
    onClickMore: () -> Unit,
) {
    TopAppBar(
        title = {},
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.white_alpha_070_grey_alpha_070,
        navigationIcon = {
            IconButton(onClick = onClickBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back_white),
                    contentDescription = "Image Preview Back",
                    tint = MaterialTheme.colors.black_white,
                    modifier = Modifier.testTag(IMAGE_PREVIEW_APP_BAR_BACK),
                )
            }
        },
        actions = {
            val isSlideshowMenuVisible by produceState(false, imageNode) {
                value = showSlideshowMenu(imageNode)
            }

            val isForwardMenuVisible by produceState(false, imageNode) {
                value = showForwardMenu(imageNode)
            }

            val isSaveToDeviceMenuVisible by produceState(false, imageNode) {
                value = showSaveToDeviceMenu(imageNode)
            }

            val isManageLinkMenuVisible by produceState(false, imageNode) {
                value = showManageLinkMenu(imageNode)
            }

            val isSendToMenuVisible by produceState(false, imageNode) {
                value = showSendToMenu(imageNode)
            }

            val isMoreMenuVisible by produceState(false, imageNode) {
                value = showMoreMenu(imageNode)
            }

            if (isSlideshowMenuVisible) {
                IconButton(onClick = onClickSlideshow) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_slideshow),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_APP_BAR_SLIDESHOW),
                    )
                }
            }

            if (isForwardMenuVisible) {
                IconButton(onClick = onClickForward) {
                    Icon(
                        painter = painterResource(id = iconPackR.drawable.ic_corner_up_right_medium_regular_outline),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_APP_BAR_FORWARD),
                    )
                }
            }

            if (isSaveToDeviceMenuVisible) {
                IconButton(onClick = onClickSaveToDevice) {
                    Icon(
                        painter = painterResource(id = iconPackR.drawable.ic_download_medium_regular_outline),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_APP_BAR_SAVE_TO_DEVICE),
                    )
                }
            }

            if (isManageLinkMenuVisible) {
                IconButton(onClick = onClickGetLink) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_link),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_APP_BAR_MANAGE_LINK),
                    )
                }
            }

            if (isSendToMenuVisible) {
                IconButton(onClick = onClickSendTo) {
                    Icon(
                        painter = painterResource(id = iconPackR.drawable.ic_message_arrow_up_medium_regular_outline),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_APP_BAR_SEND_TO),
                    )
                }
            }

            if (isMoreMenuVisible) {
                IconButton(onClick = onClickMore) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_dots_vertical_white),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_APP_BAR_MORE),
                    )
                }
            }
        },
        elevation = 0.dp,
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ImagePreviewBottomBar(
    modifier: Modifier = Modifier,
    imageName: String = "",
    imageIndex: String = "",
) {
    BottomAppBar(
        backgroundColor = MaterialTheme.colors.white_alpha_070_grey_alpha_070,
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MiddleEllipsisText(
                text = imageName,
                color = TextColor.Secondary,
                modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_BAR_TEXT_IMAGE_NAME),
            )
            MiddleEllipsisText(
                text = imageIndex,
                color = TextColor.Secondary,
                modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_BAR_TEXT_IMAGE_COUNT),
            )
        }
    }
}
