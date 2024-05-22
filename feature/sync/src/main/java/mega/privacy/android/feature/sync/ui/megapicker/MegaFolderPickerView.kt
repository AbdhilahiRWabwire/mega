package mega.privacy.android.feature.sync.ui.megapicker

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.extension.getIcon
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.legacy.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.shared.theme.MegaAppTheme

@Composable
internal fun MegaFolderPickerView(
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    nodesList: List<TypedNodeUiModel>,
    sortOrder: String,
    showSortOrder: Boolean,
    showChangeViewType: Boolean,
    listState: LazyListState,
    onFolderClick: (TypedNode) -> Unit,
    fileTypeIconMapper: FileTypeIconMapper,
    modifier: Modifier = Modifier,
) {
    LazyColumn(state = listState, modifier = modifier) {
        item(
            key = "header"
        ) {
            HeaderViewItem(
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClick = onChangeViewTypeClick,
                onEnterMediaDiscoveryClick = {},
                sortOrder = sortOrder,
                isListView = true,
                showSortOrder = showSortOrder,
                showChangeViewType = showChangeViewType
            )
        }
        items(count = nodesList.size,
            key = {
                nodesList[it].node.id.longValue
            }) {
            val nodeEntity = nodesList[it].node

            val icon = when (nodeEntity) {
                is FolderNode -> nodeEntity.getIcon()
                is FileNode -> fileTypeIconMapper(nodeEntity.type.extension)
                else -> iconPackR.drawable.ic_generic_medium_solid
            }
            NodeListViewItem(
                isSelected = false,
                folderInfo = (nodeEntity as? FolderNode)
                    ?.folderInfo(),
                icon = icon,
                thumbnailData = ThumbnailRequest(nodeEntity.id),
                fileSize = (nodeEntity as? FileNode)
                    ?.let { node -> formatFileSize(node.size, LocalContext.current) },
                modifiedDate = (nodeEntity as? FileNode)
                    ?.let { node ->
                        formatModifiedDate(
                            java.util.Locale(
                                Locale.current.language, Locale.current.region
                            ),
                            node.modificationTime
                        )
                    },
                name = nodeEntity.name,
                showMenuButton = false,
                isTakenDown = false,
                isFavourite = false,
                isSharedWithPublicLink = false,
                onClick = { onFolderClick(nodeEntity) },
                isEnabled = nodeEntity is FolderNode && nodesList[it].isDisabled.not(),
            )
            Divider(
                modifier = Modifier
                    .fillMaxWidth(),
                color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                thickness = 1.dp
            )
        }
    }
}

@Composable
@CombinedThemePreviews
private fun PreviewMegaFolderPickerView() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        MegaFolderPickerView(
            nodesList = SampleNodeDataProvider.values,
            sortOrder = "Name",
            onSortOrderClick = {},
            onChangeViewTypeClick = { },
            showSortOrder = true,
            listState = LazyListState(),
            modifier = Modifier,
            showChangeViewType = true,
            onFolderClick = {},
            fileTypeIconMapper = FileTypeIconMapper()
        )
    }
}

@Composable
private fun FolderNode.folderInfo(): String {
    return if (childFolderCount == 0 && childFileCount == 0) {
        stringResource(R.string.sync_file_browser_empty_folder)
    } else if (childFolderCount == 0 && childFileCount > 0) {
        pluralStringResource(R.plurals.num_files_with_parameter, childFileCount, childFileCount)
    } else if (childFileCount == 0 && childFolderCount > 0) {
        pluralStringResource(
            R.plurals.num_folders_with_parameter,
            childFolderCount,
            childFolderCount
        )
    } else {
        pluralStringResource(
            R.plurals.num_folders_num_files,
            childFolderCount,
            childFolderCount
        ) + pluralStringResource(
            R.plurals.num_folders_num_files_2,
            childFileCount,
            childFileCount
        )
    }
}