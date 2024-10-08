package mega.privacy.android.domain.entity.call

/**
 * Chat session changes
 */
enum class ChatSessionChanges {
    /**
     * No changes
     */
    NoChanges,

    /**
     * Status changes
     */
    Status,

    /**
     * Remote AV flags
     */
    RemoteAvFlags,

    /**
     * Session on high resolution
     */
    SessionOnHiRes,

    /**
     * Session on Low resolution
     */
    SessionOnLowRes,

    /**
     * Session on hold
     */
    SessionOnHold,

    /**
     * Audio level changes
     */
    AudioLevel,

    /**
     * Permission changes
     */
    Permissions,

    /**
     * Session on recording
     */
    SessionOnRecording,
}