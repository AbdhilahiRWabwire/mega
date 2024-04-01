package mega.privacy.android.app.presentation.manager.model

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.chat.ChatLinkContent
import mega.privacy.android.domain.entity.meeting.UsersCallLimitReminders
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
import mega.privacy.android.domain.entity.node.RestoreNodeResult


/**
 * Manager state
 *
 * @property isFirstNavigationLevel
 * @property sharesTab
 * @property isFirstLogin
 * @property nodeUpdateReceived
 * @property pendingActionsCount
 * @property shouldAlertUserAboutSecurityUpgrade
 * @property showSyncSection
 * @property show2FADialog
 * @property enabledFlags
 * @property isPushNotificationSettingsUpdatedEvent
 * @property titleChatArchivedEvent
 * @property restoreNodeResult
 * @property nodeNameCollisionResult
 * @property moveRequestResult
 * @property message
 * @property chatLinkContent                        Result of check link request
 * @property androidSyncServiceEnabled              Indicates if need to enable android sync service
 * @property userRootBackupsFolderHandle            The User's Root Backups Folder Handle
 * @property isSessionOnRecording                   True if a host is recording or False otherwise.
 * @property showRecordingConsentDialog             True if should show the recording consent dialog or False otherwise.
 * @property isRecordingConsentAccepted             True if recording consent dialog has been already accepted or False otherwise.
 * @property callInProgressChatId                   Chat ID of the current call in progress.
 * @property deviceCenterPreviousBottomNavigationItem  A potentially nullable Integer that holds the
 * @property callEndedDueToFreePlanLimits               State event to show the force free plan limit participants dialog.
 * @property isCallUnlimitedProPlanFeatureFlagEnabled   True, if Call Unlimited Pro Plan feature flag enabled. False, otherwise.
 * @property usersCallLimitReminders   [UsersCallLimitReminders]
 * previous Bottom Navigation item before accessing Device Center
 */
data class ManagerState(
    val isFirstNavigationLevel: Boolean = true,
    val sharesTab: SharesTab = SharesTab.INCOMING_TAB,
    val isFirstLogin: Boolean = false,
    val nodeUpdateReceived: Boolean = false,
    val pendingActionsCount: Int = 0,
    val shouldAlertUserAboutSecurityUpgrade: Boolean = false,
    val showSyncSection: Boolean = false,
    val show2FADialog: Boolean = false,
    val enabledFlags: Set<Feature> = emptySet(),
    val isPushNotificationSettingsUpdatedEvent: Boolean = false,
    val titleChatArchivedEvent: String? = null,
    val restoreNodeResult: Result<RestoreNodeResult>? = null,
    val nodeNameCollisionResult: NodeNameCollisionResult? = null,
    val moveRequestResult: Result<MoveRequestResult>? = null,
    val message: String? = null,
    val chatLinkContent: Result<ChatLinkContent>? = null,
    val androidSyncServiceEnabled: Boolean = false,
    val userRootBackupsFolderHandle: NodeId = NodeId(-1L),
    val isSessionOnRecording: Boolean = false,
    val showRecordingConsentDialog: Boolean = false,
    val isRecordingConsentAccepted: Boolean = false,
    val callInProgressChatId: Long = -1L,
    val deviceCenterPreviousBottomNavigationItem: Int? = null,
    val callEndedDueToFreePlanLimits: Boolean = false,
    val isCallUnlimitedProPlanFeatureFlagEnabled: Boolean = false,
    val usersCallLimitReminders: UsersCallLimitReminders = UsersCallLimitReminders.Enabled
)
