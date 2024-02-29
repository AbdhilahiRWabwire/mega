package mega.privacy.android.domain.entity.contacts

import mega.privacy.android.domain.entity.user.UserVisibility

/**
 * Data class of a MEGA user.
 * This class contains comprehensive information about a Mega User.
 * Refer to [User] if you only need basic information.
 *
 *
 * @property handle                 User identifier.
 * @property email                  User email.
 * @property contactData            [ContactData].
 * @property defaultAvatarColor     User default avatar color.
 * @property visibility             [UserVisibility].
 * @property timestamp              Time when the user was included in the contact list.
 * @property areCredentialsVerified True if user credentials are verified, false otherwise.
 * @property status                 [UserChatStatus].
 * @property lastSeen               User last seen.
 */
data class ContactItem(
    val handle: Long,
    val email: String,
    val contactData: ContactData,
    val defaultAvatarColor: String?,
    val visibility: UserVisibility,
    val timestamp: Long,
    val areCredentialsVerified: Boolean,
    val status: UserChatStatus,
    val lastSeen: Int? = null,
)
