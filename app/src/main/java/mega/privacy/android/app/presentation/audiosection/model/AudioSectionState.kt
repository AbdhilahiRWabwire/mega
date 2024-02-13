package mega.privacy.android.app.presentation.audiosection.model

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * The state for the audio section
 *
 * @property allAudios the all audio items
 * @property currentViewType the current view type
 * @property sortOrder the sort order of audio items
 * @property isPendingRefresh
 * @property progressBarShowing the progress bar showing state
 * @property searchMode the search mode state
 * @property scrollToTop the scroll to top state
 * @property selectedAudioHandles the selected audio handles
 * @property isInSelection if list is in selection mode or not
 */
data class AudioSectionState(
    val allAudios: List<AudioUIEntity> = emptyList(),
    val currentViewType: ViewType = ViewType.LIST,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val isPendingRefresh: Boolean = false,
    val progressBarShowing: Boolean = true,
    val searchMode: Boolean = false,
    val scrollToTop: Boolean = false,
    val selectedAudioHandles: List<Long> = emptyList(),
    val isInSelection: Boolean = false,
)
