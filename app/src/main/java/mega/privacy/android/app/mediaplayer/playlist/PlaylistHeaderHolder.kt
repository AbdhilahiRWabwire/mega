package mega.privacy.android.app.mediaplayer.playlist

import mega.privacy.android.app.databinding.ItemPlaylistHeaderBinding

/**
 * ViewHolder for Previous, Playing, Next headers.
 */
class PlaylistHeaderHolder(private val binding: ItemPlaylistHeaderBinding) :
    PlaylistViewHolder(binding) {
    override fun bind(item: PlaylistItem, itemOperation: PlaylistItemOperation) {
        binding.highlight = item.type == PlaylistItem.TYPE_NEXT_HEADER
        binding.name = item.nodeName
    }
}
