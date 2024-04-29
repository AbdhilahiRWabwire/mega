package mega.privacy.android.app.presentation.shares

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.shares.incoming.IncomingSharesComposeFragment
import mega.privacy.android.app.presentation.shares.links.LinksComposeFragment
import mega.privacy.android.app.presentation.shares.outgoing.OutgoingSharesComposeFragment
import mega.privacy.android.app.presentation.shares.outgoing.OutgoingSharesFragment

/**
 * Pager adapter for shares pages
 *
 * @param activity FragmentActivity where the viewPager2 lives
 */
class SharesPageAdapter(
    private val enabledOutgoingSharesCompose: Boolean,
    private val activity: FragmentActivity,
) :
    FragmentStateAdapter(activity) {

    /**
     * The list of fragments hold by the adapter
     */
    private val fragments = mutableMapOf(
        SharesTab.INCOMING_TAB to IncomingSharesComposeFragment(),
        SharesTab.OUTGOING_TAB to if (enabledOutgoingSharesCompose) OutgoingSharesComposeFragment() else OutgoingSharesFragment(),
        SharesTab.LINKS_TAB to LinksComposeFragment()
    )

    /**
     * Returns the total number of items in the data set held by the adapter.
     * Corresponds to the number of items in SharesTab except the SharesTab.NONE
     *
     * @return the total number of items
     */
    override fun getItemCount(): Int = SharesTab.values().size - 1

    /**
     * Create a new Fragment associated with the specified position.
     *
     * @param position the given position in the adapter
     * @return the instance of the new fragment at the given position
     */
    override fun createFragment(position: Int): Fragment {
        return fragments[SharesTab.fromPosition(position)] ?: throw Exception("Invalid position")
    }

    /**
     * Refresh a fragment inside the adapter
     *
     * @param position the given position of the fragment in the adapter
     */
    fun refreshFragment(position: Int) {
        val fragment = when (SharesTab.fromPosition(position)) {
            SharesTab.INCOMING_TAB -> IncomingSharesComposeFragment()
            SharesTab.OUTGOING_TAB -> if (enabledOutgoingSharesCompose) OutgoingSharesComposeFragment() else OutgoingSharesFragment()
            SharesTab.LINKS_TAB -> LinksComposeFragment()
            else -> throw Exception("Invalid position")
        }
        fragments[SharesTab.fromPosition(position)] = fragment
        notifyItemChanged(position)
    }

    /**
     * Returns the itemId of the element in the adapter
     *
     * @param position the given position of the fragment in the adapter
     * @return a unique id that identifies the item in the adapter
     */
    override fun getItemId(position: Int): Long {
        return fragments[SharesTab.fromPosition(position)].hashCode().toLong()
    }

    /**
     * Check if the adapter contains an item based on his itemId
     *
     * @param itemId the unique id of the item
     * @return true if the item is the adapter contains the item
     */
    override fun containsItem(itemId: Long): Boolean {
        return fragments.filterValues { it.hashCode().toLong() == itemId }.isNotEmpty()
    }

    /**
     * Return the fragment at the specified position
     *
     * @param position the given position in the adapter
     * @return the fragment at the given position
     */
    fun getFragment(position: Int): Fragment? =
        activity.supportFragmentManager.findFragmentByTag(getFragmentTag(position))

    /**
     * Return the tag of a fragment hold by the adapter
     *
     * @return the tag of a fragment hold by the adapter
     */
    fun getFragmentTag(position: Int): String {
        return "f${getItemId(position)}"
    }
}