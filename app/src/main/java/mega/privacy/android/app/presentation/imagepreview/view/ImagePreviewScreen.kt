@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)

package mega.privacy.android.app.presentation.imagepreview.view

import com.google.android.exoplayer2.ui.R as RExoPlayer
import android.content.Context
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.BottomAppBar
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel
import mega.privacy.android.app.presentation.slideshow.view.PhotoBox
import mega.privacy.android.app.presentation.slideshow.view.PhotoState
import mega.privacy.android.app.presentation.slideshow.view.rememberPhotoState
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.extensions.black_white
import mega.privacy.android.core.ui.theme.extensions.white_alpha_070_grey_alpha_070
import mega.privacy.android.core.ui.theme.teal_200
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode

@Composable
fun ImagePreviewScreen(
    viewModel: ImagePreviewViewModel = viewModel(),
    onClickBack: () -> Unit,
    onClickSaveToDevice: (ImageNode) -> Unit,
    onClickGetLink: (ImageNode) -> Unit,
    onClickSendTo: (ImageNode) -> Unit,
    onClickVideoPlay: (ImageNode) -> Unit,
    onClickSlideshow: () -> Unit,
) {
    val viewState by viewModel.state.collectAsStateWithLifecycle()
    val imageNodes = viewState.imageNodes
    if (imageNodes.isNotEmpty()) {
        val currentImageNodeId = viewState.currentImageNodeId
        val initialPage = remember {
            imageNodes.withIndex().first {
                currentImageNodeId.longValue == it.value.id.longValue
            }.index
        }
        val inFullScreenMode = viewState.inFullScreenMode
        val scaffoldState = rememberScaffoldState()
        val isLight = MaterialTheme.colors.isLight
        val photoState = rememberPhotoState()
        val pagerState = rememberPagerState(
            initialPage = initialPage,
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
            snapshotFlow { pagerState.currentPage }.collect { page ->
                imageNodes.getOrNull(page)?.id?.let { viewModel.setCurrentImageNodeId(it) }
            }
        }

        if (viewState.transferMessage.isNotEmpty()) {
            LaunchedEffect(Unit) {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = viewState.transferMessage,
                )
            }
        }
        Scaffold(
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
                    innerPadding = innerPadding,
                    getInfoText = viewModel::getInfoText,
                    modalSheetState = modalSheetState,
                    pagerState = pagerState,
                    imageNodes = imageNodes,
                    photoState = photoState,
                    downloadImage = viewModel::monitorImageResult,
                    onImageTap = { viewModel.switchFullScreenMode() },
                    onClickVideoPlay = onClickVideoPlay,
                    topAppBar = { imageNode ->
                        AnimatedVisibility(
                            visible = !inFullScreenMode,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            ImagePreviewTopBar(
                                showSlideshowMenu = viewState.showSlideshowOption
                                        && viewModel.isSlideshowOptionVisible(imageNode),
                                showSaveToDeviceMenu = viewModel.isSaveToDeviceOptionVisible(
                                    imageNode
                                ),
                                showManageLinkMenu = viewModel.isGetLinkOptionVisible(imageNode),
                                showSendToMenu = viewModel.isSendToOptionVisible(imageNode),
                                onClickBack = onClickBack,
                                onClickSlideshow = onClickSlideshow,
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
                            )

                            ImagePreviewBottomBar(
                                imageName = currentImageNode.name,
                                imageIndex = photoIndexText,
                            )
                        }
                    }
                )
            },
        )
    }
}

@Composable
private fun ImagePreviewContent(
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues,
    pagerState: PagerState,
    imageNodes: List<ImageNode>,
    photoState: PhotoState,
    onImageTap: () -> Unit,
    topAppBar: @Composable (ImageNode) -> Unit,
    bottomAppBar: @Composable (ImageNode, Int) -> Unit,
    downloadImage: suspend (ImageNode) -> Flow<ImageResult>,
    getInfoText: (ImageNode, Context) -> String,
    modalSheetState: ModalBottomSheetState,
    onClickVideoPlay: (ImageNode) -> Unit,
) {
    val context = LocalContext.current
    Box(modifier = modifier.background(color = Color.Black)) {
        HorizontalPager(
            modifier = Modifier
                .fillMaxSize(),
            state = pagerState,
            beyondBoundsPageCount = 5,
            key = { imageNodes.getOrNull(it)?.id?.longValue ?: -1L }
        ) { index ->
            val currentImageNode = imageNodes[index]
            val currentImageNodeInfo = remember(index) {
                getInfoText(
                    currentImageNode,
                    context,
                )
            }

            val imageResult by produceState<ImageResult?>(initialValue = null) {
                downloadImage(currentImageNode).collectLatest { imageResult ->
                    value = imageResult
                }
            }

            ImagePreviewBottomSheet(
                modalSheetState = modalSheetState,
                imageThumbnailPath = imageResult?.getLowestResolutionAvailableUri(),
                imageName = currentImageNode.name,
                imageInfo = currentImageNodeInfo,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ImageContent(
                            fullSizePath = imageResult?.getHighestResolutionAvailableUri(),
                            photoState = photoState,
                            onImageTap = onImageTap
                        )
                        if (currentImageNode.type is VideoFileTypeInfo) {
                            IconButton(
                                modifier = Modifier.align(Alignment.Center),
                                onClick = { onClickVideoPlay(currentImageNode) }
                            ) {
                                Icon(
                                    painter = painterResource(id = RExoPlayer.drawable.exo_icon_play),
                                    contentDescription = "Image Preview play video",
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .align(Alignment.TopCenter)
                    ) {
                        topAppBar(currentImageNode)
                    }
                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .align(Alignment.BottomCenter)
                    ) {
                        bottomAppBar(currentImageNode, index)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageContent(
    fullSizePath: String?,
    photoState: PhotoState,
    onImageTap: () -> Unit,
) {
    PhotoBox(
        modifier = Modifier.fillMaxSize(),
        state = photoState,
        onTap = {
            onImageTap()
        }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(fullSizePath)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ImagePreviewTopBar(
    modifier: Modifier = Modifier,
    showSlideshowMenu: Boolean,
    showSaveToDeviceMenu: Boolean,
    showManageLinkMenu: Boolean,
    showSendToMenu: Boolean,
    onClickBack: () -> Unit,
    onClickSlideshow: () -> Unit,
    onClickSaveToDevice: () -> Unit,
    onClickGetLink: () -> Unit,
    onClickSendTo: () -> Unit,
    onClickMore: () -> Unit,
) {
    TopAppBar(
        title = {},
        backgroundColor = MaterialTheme.colors.white_alpha_070_grey_alpha_070,
        navigationIcon = {
            IconButton(onClick = onClickBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back_white),
                    contentDescription = "Image Preview Back",
                    tint = MaterialTheme.colors.black_white,
                )
            }
        },
        actions = {
            if (showSlideshowMenu) {
                IconButton(onClick = onClickSlideshow) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_slideshow),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                    )
                }
            }

            if (showSaveToDeviceMenu) {
                IconButton(onClick = onClickSaveToDevice) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_download_white),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                    )
                }
            }

            if (showManageLinkMenu) {
                IconButton(onClick = onClickGetLink) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_link),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                    )
                }
            }

            if (showSendToMenu) {
                IconButton(onClick = onClickSendTo) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_send_to_contact),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                    )
                }
            }

            IconButton(onClick = onClickMore) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_dots_vertical_white),
                    contentDescription = null,
                    tint = MaterialTheme.colors.black_white,
                )
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
            Text(imageName)
            Text(imageIndex)
        }
    }
}