package mega.privacy.android.feature.sync.ui.synclist.folders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.CardExpanded
import mega.privacy.android.feature.sync.ui.views.SyncItemView
import mega.privacy.android.feature.sync.ui.views.SyncListNoItemsPlaceHolder

@Composable
internal fun SyncFoldersScreen(
    syncUiItems: List<SyncUiItem>,
    cardExpanded: (CardExpanded) -> Unit,
    pauseRunClicked: (SyncUiItem) -> Unit,
    removeFolderClicked: (folderPairId: Long) -> Unit,
    addFolderClicked: () -> Unit,
    issuesInfoClicked: () -> Unit,
    isLowBatteryLevel: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = rememberLazyListState(), modifier = modifier
                .fillMaxSize()
        ) {
            if (syncUiItems.isEmpty()) {
                item {
                    SyncListNoItemsPlaceHolder(
                        placeholderText = "No Syncs",
                        placeholderIcon = R.drawable.no_syncs_placeholder,
                        modifier = Modifier
                            .fillParentMaxHeight(0.8f)
                            .fillParentMaxWidth()
                    )
                }
            } else {
                items(count = syncUiItems.size, key = {
                    syncUiItems[it].id
                }) { itemIndex ->
                    SyncItemView(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        syncUiItems = syncUiItems,
                        itemIndex = itemIndex,
                        cardExpanded = { syncUiItem, expanded ->
                            cardExpanded(CardExpanded(syncUiItem, expanded))
                        },
                        pauseRunClicked = pauseRunClicked,
                        removeFolderClicked = removeFolderClicked,
                        issuesInfoClicked = issuesInfoClicked,
                        isLowBatteryLevel = isLowBatteryLevel,
                        errorRes = syncUiItems[itemIndex].error
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = { addFolderClicked() },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomEnd)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add folder pair")
        }
    }
}

@CombinedThemePreviews
@Composable
private fun SyncFoldersScreenSyncingPreview() {
    SyncFoldersScreen(
        listOf(
            SyncUiItem(
                1,
                "Folder pair name",
                SyncStatus.SYNCING,
                false,
                "/path/to/local/folder",
                "/path/to/mega/folder",
                R.string.sync_two_way,
                false
            )
        ),
        cardExpanded = {},
        pauseRunClicked = {},
        removeFolderClicked = {},
        addFolderClicked = {},
        issuesInfoClicked = {},
        isLowBatteryLevel = false,
    )
}

@CombinedThemePreviews
@Composable
private fun SyncFoldersScreenSyncingWithStalledIssuesPreview() {
    SyncFoldersScreen(
        listOf(
            SyncUiItem(
                1,
                "Folder pair name",
                SyncStatus.SYNCING,
                true,
                "/path/to/local/folder",
                "/path/to/mega/folder",
                R.string.sync_two_way,
                false
            )
        ),
        cardExpanded = {},
        pauseRunClicked = {},
        removeFolderClicked = {},
        addFolderClicked = {},
        issuesInfoClicked = {},
        isLowBatteryLevel = false,
    )
}
