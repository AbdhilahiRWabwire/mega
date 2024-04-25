package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.domain.entity.meeting.CallRecordingEvent

/**
 * UI state for call recording
 *
 * @property callRecordingEvent [CallRecordingEvent]
 * @property isRecordingConsentAccepted True if the recording consent is accepted.
 */
data class CallRecordingUIState(
    val callRecordingEvent: CallRecordingEvent = CallRecordingEvent(),
    val isRecordingConsentAccepted: Boolean? = null,
) {
    /**
     * True if the session is being recorded.
     */
    val isSessionOnRecording = callRecordingEvent.isSessionOnRecording

    /**
     * The participant who is recording or recorded the session if any.
     */
    val participantRecording = callRecordingEvent.participantRecording
}