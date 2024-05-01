package mega.privacy.android.feature.sync.ui.synclist.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
import mega.privacy.android.feature.sync.data.service.SyncBackgroundService
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.GetSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.RefreshSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.RemoveFolderPairUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.ResumeSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetUserPausedSyncUseCase
import mega.privacy.android.feature.sync.ui.mapper.sync.SyncUiItemMapper
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.RemoveFolderClicked
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.PauseRunClicked
import javax.inject.Inject

@HiltViewModel
internal class SyncFoldersViewModel @Inject constructor(
    private val syncUiItemMapper: SyncUiItemMapper,
    private val removeFolderPairUseCase: RemoveFolderPairUseCase,
    private val monitorSyncsUseCase: MonitorSyncsUseCase,
    private val resumeSyncUseCase: ResumeSyncUseCase,
    private val pauseSyncUseCase: PauseSyncUseCase,
    private val getSyncStalledIssuesUseCase: GetSyncStalledIssuesUseCase,
    private val setUserPausedSyncsUseCase: SetUserPausedSyncUseCase,
    private val refreshSyncUseCase: RefreshSyncUseCase,
    private val monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SyncFoldersState(emptyList()))
    val state: StateFlow<SyncFoldersState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitorSyncsUseCase()
                .map(syncUiItemMapper::invoke)
                .map { syncs ->
                    val stalledIssues = getSyncStalledIssuesUseCase()
                    syncs.map { sync ->
                        sync.copy(hasStalledIssues = stalledIssues.any {
                            it.localPaths.firstOrNull()?.contains(sync.deviceStoragePath)
                                ?: (it.nodeNames.first().contains(sync.megaStoragePath))
                        })
                    }
                }
                .collect { syncs ->
                    _state.update {
                        it.copy(
                            syncUiItems = syncs,
                            isRefreshing = false
                        )
                    }
                }
        }

        viewModelScope.launch {
            monitorBatteryInfoUseCase().collect { batteryInfo ->
                _state.update { state ->
                    state.copy(isLowBatteryLevel = batteryInfo.level < SyncBackgroundService.LOW_BATTERY_LEVEL && !batteryInfo.isCharging)
                }
            }
        }
    }

    fun handleAction(action: SyncFoldersAction) {
        when (action) {
            is SyncFoldersAction.CardExpanded -> {
                val syncUiItem = action.syncUiItem
                val expanded = action.expanded

                _state.update { state ->
                    state.copy(
                        syncUiItems = _state.value.syncUiItems.map {
                            if (it.id == syncUiItem.id) {
                                it.copy(expanded = expanded)
                            } else {
                                it
                            }
                        }
                    )
                }
            }

            is RemoveFolderClicked -> {
                viewModelScope.launch {
                    removeFolderPairUseCase(action.folderPairId)
                }
            }

            is PauseRunClicked -> {
                viewModelScope.launch {
                    if (action.syncUiItem.status != SyncStatus.PAUSED) {
                        pauseSyncUseCase(action.syncUiItem.id)
                        setUserPausedSyncsUseCase(action.syncUiItem.id, true)
                    } else {
                        resumeSyncUseCase(action.syncUiItem.id)
                        setUserPausedSyncsUseCase(action.syncUiItem.id, false)
                    }
                }
            }
        }
    }

    fun onSyncRefresh() {
        viewModelScope.launch {
            refreshSyncUseCase()
        }
    }
}
