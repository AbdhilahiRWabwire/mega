package mega.privacy.android.app.mediaplayer.service

import com.google.android.exoplayer2.DefaultControlDispatcher
import com.google.android.exoplayer2.Player
import mega.privacy.android.app.utils.CallUtil

/**
 * A DefaultControlDispatcher which only dispatch control if there is no ongoing call.
 */
class CallAwareControlDispatcher(private var currentRepeatMode: Int) :
    DefaultControlDispatcher(0, 0) {
    override fun dispatchSeekTo(player: Player, windowIndex: Int, positionMs: Long): Boolean {
        if (CallUtil.participatingInACall()) {
            return false
        }

        return super.dispatchSeekTo(player, windowIndex, positionMs)
    }

    override fun dispatchNext(player: Player): Boolean {
        if (CallUtil.participatingInACall()) {
            return false
        }

        return super.dispatchNext(player)
    }

    override fun dispatchPrevious(player: Player): Boolean {
        if (CallUtil.participatingInACall()) {
            return false
        }

        return super.dispatchPrevious(player)
    }

    override fun dispatchSetPlayWhenReady(player: Player, playWhenReady: Boolean): Boolean {
        if (CallUtil.participatingInACall()) {
            return false
        }

        return super.dispatchSetPlayWhenReady(player, playWhenReady)
    }

    override fun dispatchSetRepeatMode(player: Player, repeatMode: Int): Boolean {
        // ExoPlayer hardcoded switch order to: off -> one -> all,
        // but we need: off -> all -> one,
        // so we need custom control.
        player.repeatMode = when (currentRepeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
            else -> Player.REPEAT_MODE_OFF
        }
        currentRepeatMode = player.repeatMode
        return true
    }
}
