package mega.privacy.android.app.utils

import androidx.recyclerview.widget.DiffUtil

/**
 * Simple DiffUtil.ItemCallback to compare Long items representing node handles.
 */
class HandleDiffCallback : DiffUtil.ItemCallback<Long>() {
    override fun areItemsTheSame(oldItem: Long, newItem: Long) =
        oldItem == newItem

    override fun areContentsTheSame(oldItem: Long, newItem: Long) =
        oldItem == newItem
}
