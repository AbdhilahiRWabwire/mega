package mega.privacy.android.domain.usecase.chat.message.forward

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.chat.message.AttachVoiceClipMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.forward.ForwardNodeAttachmentUseCase.Companion.API_ENOENT
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.reset
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ForwardVoiceClipUseCaseTest {

    private lateinit var underTest: ForwardVoiceClipUseCase

    private val attachVoiceClipMessageUseCase = mock<AttachVoiceClipMessageUseCase>()

    private val targetChatId = 789L
    private val node = mock<FileNode>()
    private val message = mock<VoiceClipMessage> {
//        on { fileNode } doReturn node
    }

    @BeforeEach
    fun setup() {
        underTest = ForwardVoiceClipUseCase(
            attachVoiceClipMessageUseCase = attachVoiceClipMessageUseCase
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(attachVoiceClipMessageUseCase)
    }

    @Test
    fun `test that empty is returned if message is not a voice clip`() = runTest {
        val message = mock<NormalMessage>()
        underTest.invoke(listOf(targetChatId), message)
        assertThat(underTest.invoke(listOf(targetChatId), message)).isEmpty()
    }

//    @Test
//    fun `test that general error is returned if attach request throws a general exception`() =
//        runTest {
//            whenever(attachVoiceClipMessageUseCase(targetChatId, node)).thenAnswer {
//                throw MegaException(errorCode = -1, null)
//            }
//            assertThat(underTest.invoke(listOf(targetChatId), message))
//                .isEqualTo(listOf(ForwardResult.GeneralError))
//        }
//
//    @Test
//    fun `test that not available error is returned if attach request throws an API_ENOENT exception`() =
//        runTest {
//            whenever(attachVoiceClipMessageUseCase(targetChatId, node)).thenAnswer {
//                throw MegaException(errorCode = API_ENOENT, null)
//            }
//            assertThat(underTest.invoke(listOf(targetChatId), message))
//                .isEqualTo(listOf(ForwardResult.ErrorNotAvailable))
//        }

    @Test
    fun `test that attach voice clip message use case is invoked and success is returned`() =
        runTest {
//            whenever(attachVoiceClipMessageUseCase(targetChatId, node)).thenReturn(Unit)
//            underTest.invoke(listOf(targetChatId), message)
//            verify(attachVoiceClipMessageUseCase).invoke(targetChatId, node)
            assertThat(underTest.invoke(listOf(targetChatId), message))
                .isEqualTo(listOf(ForwardResult.Success))
        }
}