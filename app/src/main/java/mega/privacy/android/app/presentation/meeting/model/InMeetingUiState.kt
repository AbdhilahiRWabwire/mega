package mega.privacy.android.app.presentation.meeting.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.AnotherCallType
import mega.privacy.android.domain.entity.meeting.CallOnHoldType
import mega.privacy.android.domain.entity.meeting.CallUIStatusType
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.SubtitleCallType

/**
 * In meeting UI state
 *
 * @property error                                  String resource id for showing an error.
 * @property isOpenInvite                           True if it's enabled, false if not.
 * @property callUIStatus                           [CallUIStatusType]
 * @property call                                   [ChatCall]
 * @property currentChatId                          Chat Id
 * @property previousState                          [ChatCallStatus]
 * @property isSpeakerSelectionAutomatic            True, if is speaker selection automatic. False, if it's manual.
 * @property haveConnection                         True, have connection. False, if not.
 * @property showCallDuration                       True, should show call duration. False, if not.
 * @property isPublicChat                           True if it's public chat. False, if not
 * @property chatTitle                              Chat title
 * @property updateCallSubtitle                     If should update the call subtitle
 * @property updateAnotherCallBannerType            Update the banner of another call.
 * @property anotherChatTitle                       Chat title of another call.
 * @property updateModeratorsName                   Update moderator's name
 * @property updateNumParticipants                  Update the num of participants
 * @property isOneToOneCall                         True, if it's one to one call. False, if it's a group call or a meeting.
 * @property showMeetingInfoFragment                True to show meeting info fragment or False otherwise
 * @property snackbarMessage                        Message to show in Snackbar.
 * @property snackbarInSpeakerViewMessage           Message to show in Snackbar in speaker view.
 * @property addScreensSharedParticipantsList       List of [Participant] to add the screen shared in the carousel
 * @property removeScreensSharedParticipantsList    List of [Participant] to remove the screen shared in the carousel
 * @property isMeeting                              True if it's meetings. False, if not.
 * @property updateListUi                           True, List was sorted and need to be updated. False, if not.
 * @property showEndMeetingAsHostBottomPanel        True, show bottom sheet when a host leaves the call. False otherwise
 * @property showEndMeetingAsOnlyHostBottomPanel    True, show bottom sheet when the only host leaves the call. False otherwise
 * @property joinedAsGuest                          True, joined as guest. False, otherwise.
 * @property shouldFinish                           True, if the activity should finish. False, if not.
 * @property minutesToEndMeeting                    Minutes to end the meeting
 * @property showMeetingEndWarningDialog            True, show the dialog to warn the user that the meeting is going to end. False otherwise
 * @property isRaiseToSpeakFeatureFlagEnabled       True, if Raise to speak feature flag enabled. False, otherwise.
 * @property anotherCall                            Another call in progress or on hold.
 * @property showCallOptionsBottomSheet             True, if should be shown the call options bottom panel. False, otherwise
 * @property myUserHandle                           My user handle
 * @property isEphemeralAccount                     True, if it's ephemeral account. False, if not.
 * @property showRaisedHandSnackbar                 Show raised hand snackbar
 * @property showOnlyMeEndCallTime                  Show only me end call remaining time
 * @property participantsChanges                    Message to show when a participant changes
 * @property userIdsWithChangesInRaisedHand         User identifiers with changes in the raised hand
 */
data class InMeetingUiState(
    val error: Int? = null,
    val isOpenInvite: Boolean? = null,
    var callUIStatus: CallUIStatusType = CallUIStatusType.None,
    val call: ChatCall? = null,
    val currentChatId: Long = -1L,
    val previousState: ChatCallStatus = ChatCallStatus.Initial,
    val isSpeakerSelectionAutomatic: Boolean = true,
    val haveConnection: Boolean = false,
    val showCallDuration: Boolean = false,
    val chatTitle: String = " ",
    val isPublicChat: Boolean = false,
    val updateCallSubtitle: SubtitleCallType = SubtitleCallType.Connecting,
    val updateAnotherCallBannerType: AnotherCallType = AnotherCallType.NotCall,
    val anotherChatTitle: String = " ",
    val updateModeratorsName: String = " ",
    val updateNumParticipants: Int = 1,
    val isOneToOneCall: Boolean = true,
    val showMeetingInfoFragment: Boolean = false,
    val snackbarMessage: StateEventWithContent<String> = consumed(),
    val snackbarInSpeakerViewMessage: StateEventWithContent<String> = consumed(),
    val addScreensSharedParticipantsList: List<Participant>? = null,
    val removeScreensSharedParticipantsList: List<Participant>? = null,
    val isMeeting: Boolean = false,
    val updateListUi: Boolean = false,
    val showEndMeetingAsHostBottomPanel: Boolean = false,
    val showEndMeetingAsOnlyHostBottomPanel: Boolean = false,
    val joinedAsGuest: Boolean = false,
    val shouldFinish: Boolean = false,
    val minutesToEndMeeting: Int? = null,
    val showMeetingEndWarningDialog: Boolean = false,
    val isRaiseToSpeakFeatureFlagEnabled: Boolean = false,
    val anotherCall: ChatCall? = null,
    val showCallOptionsBottomSheet: Boolean = false,
    val myUserHandle: Long? = null,
    val showRaisedHandSnackbar: Boolean = false,
    val isEphemeralAccount: Boolean? = null,
    val showOnlyMeEndCallTime: Long? = null,
    val participantsChanges: ParticipantsChange? = null,
    val userIdsWithChangesInRaisedHand: List<Long> = emptyList(),
) {
    /**
     * Is call on hold
     */
    val isCallOnHold
        get():Boolean = call?.isOnHold == true

    /**
     * Monitor if is my hand raised to speak
     */
    val isMyHandRaisedToSpeak
        get():Boolean = myUserHandle?.let {
            call?.usersRaiseHands?.get(
                it
            )
        } ?: false

    /**
     * Check if the participant's hand is raised
     */
    fun isParticipantHandRaisedToSpeak(peerId: Long) = call?.usersRaiseHands?.get(peerId) ?: false

    /**
     * Get number of users with hand raised
     */
    val numUsersWithHandRaised
        get():Int = call?.raisedHandsList?.size ?: 0

    /**
     * Number of other participants with raised hand
     */
    fun getNumOfOtherParticipantsWithHandRaised() =
        if (isMyHandRaisedToSpeak) numUsersWithHandRaised - 1 else numUsersWithHandRaised

    /**
     * Get the button to be displayed depending on the type of call on hold you have
     */
    val getButtonTypeToShow
        get():CallOnHoldType = when {
            anotherCall != null -> CallOnHoldType.SwapCalls
            !isCallOnHold -> CallOnHoldType.PutCallOnHold
            else -> CallOnHoldType.ResumeCall
        }
}

/**
 * Data class to represent a change in the participants list
 */
data class ParticipantsChange(
    /**
     * Text to show
     */
    val text: String,
    /**
     * Type of change
     */
    val type: ParticipantsChangeType,
)

/**
 * Enum class to represent the type of change in the participants list
 */
enum class ParticipantsChangeType {
    /**
     * A participant joined the call
     */
    Join,

    /**
     * A participant left the call
     */
    Left
}
