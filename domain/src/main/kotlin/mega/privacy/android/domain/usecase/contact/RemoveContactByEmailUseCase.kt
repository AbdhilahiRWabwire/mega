package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.chat.GetChatRoomByUserUseCase
import mega.privacy.android.domain.usecase.call.HangChatCallUseCase
import mega.privacy.android.domain.usecase.call.IsParticipatingInChatCallUseCase
import javax.inject.Inject

/**
 * RemoveContactByEmailUseCase
 *
 * Removes all inShares of the selected User
 * Checks if user is participating in any call
 * Hangs call is user is in any call
 * Removes the contact from mega account
 */
class RemoveContactByEmailUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val hangChatCallUseCase: HangChatCallUseCase,
    private val callRepository: CallRepository,
    private val getChatRoomByUserUseCase: GetChatRoomByUserUseCase,
    private val getContactFromEmailUseCase: GetContactFromEmailUseCase,
    private val contactsRepository: ContactsRepository,
    private val isParticipatingInChatCallUseCase: IsParticipatingInChatCallUseCase,
) {
    /**
     * Invoke
     *
     * @param email contact email which should be removed
     */
    suspend operator fun invoke(email: String): Boolean {
        nodeRepository.removedInSharedNodesByEmail(email)
        val contact = getContactFromEmailUseCase(email, skipCache = false)
        if (isParticipatingInChatCallUseCase()) {
            val chatRoom = contact?.handle?.let { getChatRoomByUserUseCase(it) }
            val call = callRepository.getChatCall(chatRoom?.chatId)
            call?.callId?.let { hangChatCallUseCase(it) }
        }
        return contactsRepository.removeContact(email)
    }
}