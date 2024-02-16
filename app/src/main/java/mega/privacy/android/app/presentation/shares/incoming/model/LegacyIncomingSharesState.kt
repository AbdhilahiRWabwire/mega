package mega.privacy.android.app.presentation.shares.incoming.model

import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.preference.ViewType
import nz.mega.sdk.MegaNode

/**
 * Incoming shares UI state
 *
 * @property currentViewType serves as the original View Type
 * @property incomingHandle current incoming shares handle
 * @property incomingNodeName current incoming shares name
 * @property incomingTreeDepth current incoming tree depth
 * @property incomingParentHandle parent handle of the current incoming node
 * @property nodes current list of nodes
 * @property isInvalidHandle true if parent handle is invalid
 * @property isLoading true if the nodes are loading
 * @property sortOrder current sort order
 * @property contactVerificationOn verification of contact is on or off
 * @property showContactNotVerifiedBanner checked if selected folder is shared by a verified user or not and shows warning banner
 */
data class LegacyIncomingSharesState(
    val currentViewType: ViewType = ViewType.LIST,
    val incomingHandle: Long = -1L,
    val incomingNodeName: String? = null,
    val incomingTreeDepth: Int = 0,
    val incomingParentHandle: Long? = null,
    val nodes: List<Pair<MegaNode, ShareData?>> = emptyList(),
    val isInvalidHandle: Boolean = true,
    val isLoading: Boolean = false,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val contactVerificationOn: Boolean = false,
    val showContactNotVerifiedBanner: Boolean = false,
) {

    /**
     * Check if we are at the root of the incoming shares page
     *
     * @return true if at the root of the incoming shares page
     */
    fun isFirstNavigationLevel() = incomingTreeDepth == 0
}
