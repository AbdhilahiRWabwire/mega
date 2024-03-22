package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import nz.mega.sdk.MegaChatCall
import javax.inject.Inject

/**
 * Mapper to convert chat call changes to List of [ChatCallChanges]
 */
internal class ChatCallChangesMapper @Inject constructor() {
    operator fun invoke(changes: Int) =
        callChanges.filter { (it.key and changes) != 0 }.values.toList()

    companion object {
        internal val callChanges = mapOf(
            MegaChatCall.CHANGE_TYPE_NO_CHANGES to ChatCallChanges.NoChanges,
            MegaChatCall.CHANGE_TYPE_STATUS to ChatCallChanges.Status,
            MegaChatCall.CHANGE_TYPE_LOCAL_AVFLAGS to ChatCallChanges.LocalAVFlags,
            MegaChatCall.CHANGE_TYPE_RINGING_STATUS to ChatCallChanges.RingingStatus,
            MegaChatCall.CHANGE_TYPE_CALL_COMPOSITION to ChatCallChanges.CallComposition,
            MegaChatCall.CHANGE_TYPE_CALL_ON_HOLD to ChatCallChanges.OnHold,
            MegaChatCall.CHANGE_TYPE_CALL_SPEAK to ChatCallChanges.Speaker,
            MegaChatCall.CHANGE_TYPE_AUDIO_LEVEL to ChatCallChanges.AudioLevel,
            MegaChatCall.CHANGE_TYPE_NETWORK_QUALITY to ChatCallChanges.NetworkQuality,
            MegaChatCall.CHANGE_TYPE_OUTGOING_RINGING_STOP to ChatCallChanges.OutgoingRingingStop,
            MegaChatCall.CHANGE_TYPE_OWN_PERMISSIONS to ChatCallChanges.OwnPermissions,
            MegaChatCall.CHANGE_TYPE_GENERIC_NOTIFICATION to ChatCallChanges.GenericNotification,
            MegaChatCall.CHANGE_TYPE_WR_ALLOW to ChatCallChanges.WaitingRoomAllow,
            MegaChatCall.CHANGE_TYPE_WR_DENY to ChatCallChanges.WaitingRoomDeny,
            MegaChatCall.CHANGE_TYPE_WR_COMPOSITION to ChatCallChanges.WaitingRoomComposition,
            MegaChatCall.CHANGE_TYPE_WR_USERS_ENTERED to ChatCallChanges.WaitingRoomUsersEntered,
            MegaChatCall.CHANGE_TYPE_WR_USERS_LEAVE to ChatCallChanges.WaitingRoomUsersLeave,
            MegaChatCall.CHANGE_TYPE_WR_USERS_ALLOW to ChatCallChanges.WaitingRoomUsersAllow,
            MegaChatCall.CHANGE_TYPE_WR_USERS_DENY to ChatCallChanges.WaitingRoomUsersDeny,
            MegaChatCall.CHANGE_TYPE_WR_PUSHED_FROM_CALL to ChatCallChanges.WaitingRoomPushedFromCall,
            MegaChatCall.CHANGE_TYPE_SPEAK_REQUESTED to ChatCallChanges.SpeakRequested,
            MegaChatCall.CHANGE_TYPE_CALL_WILL_END to ChatCallChanges.CallWillEnd,
            MegaChatCall.CHANGE_TYPE_CALL_LIMITS_UPDATED to ChatCallChanges.CallLimitsUpdated
        )
    }
}
