package mega.privacy.android.domain.usecase.chat.message.reactions

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddReactionUseCaseTest {

    private lateinit var underTest: AddReactionUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()

    @BeforeEach
    fun setup() {
        underTest = AddReactionUseCase(chatMessageRepository)
    }

    @AfterEach
    fun resetMocks() {
        reset(chatMessageRepository)
    }

    @Test
    fun `test that add reaction use case invokes addReaction in repository`() = runTest {
        val chatId = 1L
        val msgId = 2L
        val reaction = "reaction"
        whenever(chatMessageRepository.addReaction(chatId, msgId, reaction)).thenReturn(Unit)
        underTest.invoke(chatId, msgId, reaction)
        verify(chatMessageRepository).addReaction(chatId, msgId, reaction)
        verifyNoMoreInteractions(chatMessageRepository)
    }
}