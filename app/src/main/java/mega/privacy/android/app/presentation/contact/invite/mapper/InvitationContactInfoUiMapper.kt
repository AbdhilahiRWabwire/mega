package mega.privacy.android.app.presentation.contact.invite.mapper

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.main.InvitationContactInfo
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_PHONE_CONTACT
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_PHONE_CONTACT_HEADER
import mega.privacy.android.app.presentation.contact.invite.InviteContactViewModel.Companion.ID_PHONE_CONTACTS_HEADER
import mega.privacy.android.domain.entity.contacts.LocalContact
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import timber.log.Timber
import javax.inject.Inject

/**
 * A mapper class to map [LocalContact] domain entity to [InvitationContactInfo] model.
 */
class InvitationContactInfoUiMapper @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    /**
     * Invocation method.
     *
     * @param localContacts List of local contacts that will be mapped.
     * @return List of [InvitationContactInfo].
     */
    suspend operator fun invoke(localContacts: List<LocalContact>): List<InvitationContactInfo> =
        withContext(defaultDispatcher) {
            buildList {
                Timber.d("Mapping the local contacts into invitation contact info")
                add(
                    InvitationContactInfo(
                        id = ID_PHONE_CONTACTS_HEADER,
                        type = TYPE_PHONE_CONTACT_HEADER
                    )
                )

                localContacts.forEach {
                    val phoneNumberList = it.phoneNumbers + it.emails
                    if (phoneNumberList.isNotEmpty()) {
                        add(
                            InvitationContactInfo(
                                id = it.id,
                                name = it.name,
                                type = TYPE_PHONE_CONTACT,
                                filteredContactInfos = phoneNumberList,
                                displayInfo = phoneNumberList[0],
                                photoUri = it.photoUri?.value
                            )
                        )
                    }
                }
            }
        }
}
