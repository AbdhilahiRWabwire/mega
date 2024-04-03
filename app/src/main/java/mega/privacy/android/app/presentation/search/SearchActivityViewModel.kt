package mega.privacy.android.app.presentation.search

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.extensions.updateItemAt
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.search.mapper.DateFilterOptionStringMapper
import mega.privacy.android.app.presentation.search.mapper.EmptySearchViewMapper
import mega.privacy.android.app.presentation.search.mapper.SearchFilterMapper
import mega.privacy.android.app.presentation.search.mapper.TypeFilterOptionStringMapper
import mega.privacy.android.app.presentation.search.mapper.TypeFilterToSearchMapper
import mega.privacy.android.app.presentation.search.model.FilterOptionEntity
import mega.privacy.android.app.presentation.search.model.SearchActivityState
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.app.presentation.search.navigation.DATE_ADDED
import mega.privacy.android.app.presentation.search.navigation.DATE_MODIFIED
import mega.privacy.android.app.presentation.search.navigation.TYPE
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodeSourceType.OTHER
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.TypeFilterOption
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.search.GetSearchCategoriesUseCase
import mega.privacy.android.domain.usecase.search.SearchNodesUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * SearchActivity View Model
 * @property getFeatureFlagValueUseCase [GetFeatureFlagValueUseCase]
 * @property monitorNodeUpdatesUseCase [MonitorNodeUpdatesUseCase]
 * @property searchNodesUseCase [SearchNodesUseCase]
 * @property getSearchCategoriesUseCase [GetSearchCategoriesUseCase]
 * @property searchFilterMapper [SearchFilterMapper]
 * @property typeFilterToSearchMapper [TypeFilterToSearchMapper]
 * @property emptySearchViewMapper [EmptySearchViewMapper]
 * @property cancelCancelTokenUseCase [CancelCancelTokenUseCase]
 * @property setViewType [SetViewType]
 * @property monitorViewType [MonitorViewType]
 * @property getCloudSortOrder [GetCloudSortOrder]
 * @property monitorOfflineNodeUpdatesUseCase [MonitorOfflineNodeUpdatesUseCase]
 */
@HiltViewModel
class SearchActivityViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val searchNodesUseCase: SearchNodesUseCase,
    private val getSearchCategoriesUseCase: GetSearchCategoriesUseCase,
    private val searchFilterMapper: SearchFilterMapper,
    private val typeFilterToSearchMapper: TypeFilterToSearchMapper,
    private val typeFilterOptionStringMapper: TypeFilterOptionStringMapper,
    private val dateFilterOptionStringMapper: DateFilterOptionStringMapper,
    private val emptySearchViewMapper: EmptySearchViewMapper,
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase,
    private val setViewType: SetViewType,
    private val monitorViewType: MonitorViewType,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    stateHandle: SavedStateHandle,
) : ViewModel() {
    /**
     * private UI state
     */
    private val _state = MutableStateFlow(SearchActivityState())

    /**
     * public UI State
     */
    val state: StateFlow<SearchActivityState> = _state
    private var searchJob: Job? = null

    private val isFirstLevel = stateHandle.get<Boolean>(SearchActivity.IS_FIRST_LEVEL) ?: false
    private val nodeSourceType =
        stateHandle.get<NodeSourceType>(SearchActivity.SEARCH_TYPE) ?: OTHER
    private val parentHandle =
        stateHandle.get<Long>(SearchActivity.PARENT_HANDLE) ?: MegaApiJava.INVALID_HANDLE

    init {
        checkDropdownChipsFlag()
        monitorNodeUpdatesForSearch()
        initializeSearch()
        checkViewType()
    }

    private fun checkDropdownChipsFlag() {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(AppFeatures.DropdownChips)
            }.onSuccess { flag ->
                _state.update {
                    it.copy(dropdownChipsEnabled = flag)
                }
            }.onFailure {
                Timber.e("Get feature flag failed $it")
            }
        }
    }

    private fun initializeSearch() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    nodeSourceType = nodeSourceType,
                    typeSelectedFilterOption = null,
                    dateModifiedSelectedFilterOption = null,
                    dateAddedSelectedFilterOption = null,
                )
            }
            runCatching {
                getSearchCategoriesUseCase().map { searchFilterMapper(it) }
                    .filterNot { it.filter == SearchCategory.ALL }
            }.onSuccess { filters ->
                _state.update {
                    it.copy(filters = filters, selectedFilter = null)
                }
            }.onFailure {
                Timber.e("Get search categories failed $it")
            }
        }
    }


    private fun monitorNodeUpdatesForSearch() {
        viewModelScope.launch {
            merge(monitorNodeUpdatesUseCase(), monitorOfflineNodeUpdatesUseCase()).conflate()
                .collectLatest {
                    performSearch()
                }
        }
    }

    /**
     * Perform search by entering query or change in search type
     */
    private fun performSearch() {
        searchJob?.cancel()
        _state.update { it.copy(isSearching = true) }
        searchJob = viewModelScope.launch {
            runCatching {
                cancelCancelTokenUseCase()
                if (state.value.dropdownChipsEnabled == true) {
                    searchNodesUseCase(
                        query = state.value.searchQuery,
                        parentHandle = parentHandle,
                        nodeSourceType = nodeSourceType,
                        isFirstLevel = isFirstLevel,
                        searchCategory = typeFilterToSearchMapper(state.value.typeSelectedFilterOption),
                        modificationDate = state.value.dateModifiedSelectedFilterOption,
                        creationDate = state.value.dateAddedSelectedFilterOption,
                    )
                } else {
                    searchNodesUseCase(
                        query = state.value.searchQuery,
                        parentHandle = parentHandle,
                        nodeSourceType = nodeSourceType,
                        isFirstLevel = isFirstLevel,
                        searchCategory = state.value.selectedFilter?.filter ?: SearchCategory.ALL
                    )
                }
            }.onSuccess {
                onSearchSuccess(it)
            }.onFailure { ex ->
                onSearchFailure(ex)
            }
        }
    }

    private fun onSearchFailure(ex: Throwable) {
        Timber.e(ex)
        if (ex is CancellationException) {
            return
        }
        val emptyState = getEmptySearchState()
        _state.update {
            it.copy(
                searchItemList = emptyList(),
                isSearching = false,
                emptyState = emptyState
            )
        }
    }

    private suspend fun onSearchSuccess(searchResults: List<TypedNode>?) {
        if (searchResults.isNullOrEmpty()) {
            val emptyState = getEmptySearchState()
            _state.update {
                it.copy(
                    searchItemList = emptyList(),
                    isSearching = false,
                    emptyState = emptyState
                )
            }
        } else {
            val nodeUIItems = searchResults.distinctBy { it.id.longValue }.map { typedNode ->
                NodeUIItem(node = typedNode, isSelected = false)
            }
            _state.update { state ->
                val cloudSortOrder = getCloudSortOrder()
                state.copy(
                    searchItemList = nodeUIItems,
                    isSearching = false,
                    sortOrder = cloudSortOrder
                )
            }
        }
    }

    private fun getEmptySearchState() =
        if (state.value.dropdownChipsEnabled == true) {
            emptySearchViewMapper(
                isSearchChipEnabled = true,
                category = typeFilterToSearchMapper(state.value.typeSelectedFilterOption),
                searchQuery = state.value.searchQuery
            )
        } else {
            emptySearchViewMapper(
                isSearchChipEnabled = true,
                category = state.value.selectedFilter?.filter,
                searchQuery = state.value.searchQuery
            )
        }

    /**
     * Update search filter on selection
     *
     * @param selectedChip
     */
    fun updateFilter(selectedChip: SearchFilter?) {
        _state.update {
            it.copy(
                selectedFilter = selectedChip.takeIf { selectedChip?.filter != state.value.selectedFilter?.filter },
                resetScroll = triggered
            )
        }
        viewModelScope.launch { performSearch() }
    }

    /**
     * Update search query on typing
     *
     * @param query search text
     */
    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        viewModelScope.launch { performSearch() }
    }

    /**
     * This method will handle Item click event from NodesView and will update
     * [state] accordingly if items already selected/unselected, update check count else get MegaNode
     * and navigate to appropriate activity
     *
     * @param nodeUIItem [NodeUIItem]
     */
    fun onItemClicked(nodeUIItem: NodeUIItem<TypedNode>) {
        val index =
            _state.value.searchItemList.indexOfFirst { it.node.id.longValue == nodeUIItem.id.longValue }
        if (_state.value.selectedNodes.isNotEmpty()) {
            updateNodeSelection(nodeUIItem = nodeUIItem, index = index)
        }
    }

    /**
     * Clear selection
     */
    fun clearSelection() {
        val searchResultsUpdated = _state.value.searchItemList.asSequence().map {
            it.copy(isSelected = false)
        }
        _state.update {
            it.copy(
                searchItemList = searchResultsUpdated.toList(),
                selectedNodes = emptySet()
            )
        }
    }

    /**
     * Select ALl
     */
    fun selectAll() = viewModelScope.launch {
        val searchResultsUpdated = _state.value.searchItemList.asSequence().map {
            it.copy(isSelected = true)
        }
        val selectedNodes = _state.value.searchItemList.asSequence().map {
            it.node
        }.toSet()
        _state.update {
            it.copy(
                searchItemList = searchResultsUpdated.toList(),
                selectedNodes = selectedNodes,
            )
        }
    }

    /**
     * This will update [NodeUIItem] list based on and update it on to the UI
     * @param nodeUIItem [NodeUIItem] to be updated
     * @param index Index of [NodeUIItem] in [state]
     */
    private fun updateNodeSelection(nodeUIItem: NodeUIItem<TypedNode>, index: Int) =
        viewModelScope.launch {
            nodeUIItem.isSelected = !nodeUIItem.isSelected
            val selectedNode = state.value.selectedNodes.toMutableSet()
            if (state.value.selectedNodes.contains(nodeUIItem.node)) {
                selectedNode.remove(nodeUIItem.node)
            } else {
                selectedNode.add(nodeUIItem.node)
            }
            val newNodesList =
                _state.value.searchItemList.updateItemAt(index = index, item = nodeUIItem)
            _state.update {
                it.copy(
                    searchItemList = newNodesList,
                    optionsItemInfo = null,
                    selectedNodes = selectedNode,
                )
            }
        }

    /**
     * This method will handle Long click on a NodesView and check the selected item
     *
     * @param nodeUIItem [NodeUIItem]
     */
    fun onLongItemClicked(nodeUIItem: NodeUIItem<TypedNode>) {
        val index =
            _state.value.searchItemList.indexOfFirst { it.node.id.longValue == nodeUIItem.id.longValue }
        updateNodeSelection(nodeUIItem = nodeUIItem, index = index)
    }

    /**
     * Updates the type filter with the selected option
     */
    fun setTypeSelectedFilterOption(typeFilterOption: TypeFilterOption?) {
        _state.update {
            it.copy(
                typeSelectedFilterOption = typeFilterOption,
                resetScroll = triggered
            )
        }
        viewModelScope.launch { performSearch() }
    }

    /**
     * Updates the date modified filter with the selected option
     */
    fun setDateModifiedSelectedFilterOption(dateFilterOption: DateFilterOption?) {
        _state.update {
            it.copy(
                dateModifiedSelectedFilterOption = dateFilterOption,
                resetScroll = triggered
            )
        }
        viewModelScope.launch { performSearch() }
    }

    /**
     * Updates the date added filter with the selected option
     */
    fun setDateAddedSelectedFilterOption(dateFilterOption: DateFilterOption?) {
        _state.update {
            it.copy(
                dateAddedSelectedFilterOption = dateFilterOption,
                resetScroll = triggered
            )
        }
        viewModelScope.launch { performSearch() }
    }

    /**
     * This method will toggle view type
     */
    fun onChangeViewTypeClicked() {
        viewModelScope.launch {
            when (_state.value.currentViewType) {
                ViewType.LIST -> setViewType(ViewType.GRID)
                ViewType.GRID -> setViewType(ViewType.LIST)
            }
        }
    }

    /**
     * This method will monitor view type and update it on state
     */
    private fun checkViewType() {
        viewModelScope.launch {
            monitorViewType().collect { viewType ->
                _state.update { it.copy(currentViewType = viewType) }
            }
        }
    }

    /**
     * When we change sort order from UI
     */
    fun onSortOrderChanged() = performSearch()

    /**
     * Show error message on UI
     */
    fun showShowErrorMessage(@StringRes errorMessageResId: Int) {
        _state.update { it.copy(errorMessageId = errorMessageResId) }
    }

    /**
     * Remove error message
     */
    fun errorMessageShown() {
        _state.update { it.copy(errorMessageId = null) }
    }

    /**
     * Get filter options based on filter
     *
     * @param filter filter
     */
    fun getFilterOptions(filter: String): List<FilterOptionEntity> = when (filter) {
        TYPE ->
            TypeFilterOption.entries.map { option ->
                FilterOptionEntity(
                    option.ordinal,
                    typeFilterOptionStringMapper(option),
                    option == state.value.typeSelectedFilterOption
                )
            }

        DATE_MODIFIED ->
            DateFilterOption.entries.map { option ->
                FilterOptionEntity(
                    option.ordinal,
                    dateFilterOptionStringMapper(option),
                    option == state.value.dateModifiedSelectedFilterOption
                )
            }

        DATE_ADDED -> DateFilterOption.entries.map { option ->
            FilterOptionEntity(
                option.ordinal,
                dateFilterOptionStringMapper(option),
                option == state.value.dateAddedSelectedFilterOption
            )
        }

        else -> emptyList()
    }

    /**
     * Update filter entity
     */
    fun updateFilterEntity(filter: String, filterOption: FilterOptionEntity) {
        when (filter) {
            TYPE -> {
                val typeOption = TypeFilterOption.entries.getOrNull(filterOption.id)
                    ?.takeIf { it.ordinal != state.value.typeSelectedFilterOption?.ordinal }
                setTypeSelectedFilterOption(typeOption)
            }

            DATE_MODIFIED -> {
                val dateModifiedOption = DateFilterOption.entries.getOrNull(filterOption.id)
                    ?.takeIf { it.ordinal != state.value.dateModifiedSelectedFilterOption?.ordinal }

                setDateModifiedSelectedFilterOption(dateModifiedOption)
            }

            DATE_ADDED -> {
                val dateAddedOption = DateFilterOption.entries.getOrNull(filterOption.id)
                    ?.takeIf { it.ordinal != state.value.dateAddedSelectedFilterOption?.ordinal }

                setDateAddedSelectedFilterOption(dateAddedOption)
            }
        }
    }

    /**
     * Clear reset scroll
     */
    fun clearResetScroll() {
        _state.update {
            it.copy(resetScroll = consumed)
        }
    }
}
