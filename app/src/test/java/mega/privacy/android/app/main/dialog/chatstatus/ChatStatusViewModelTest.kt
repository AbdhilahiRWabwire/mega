package mega.privacy.android.app.main.dialog.chatstatus

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.main.dialog.chatstatus.ChatStatusViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.usecase.chat.GetCurrentUserStatusUseCase
import mega.privacy.android.domain.usecase.chat.SetCurrentUserStatusUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatStatusViewModelTest {
    private lateinit var underTest: ChatStatusViewModel
    private val getCurrentUserStatusUseCase: GetCurrentUserStatusUseCase = mock()
    private val setCurrentUserStatusUseCase: SetCurrentUserStatusUseCase = mock()

    @BeforeEach
    fun resetMocks() {
        wheneverBlocking { getCurrentUserStatusUseCase() }.thenReturn(UserChatStatus.Invalid)
        reset(
            setCurrentUserStatusUseCase,
        )
    }

    private fun initTestClass() {
        underTest = ChatStatusViewModel(
            getCurrentUserStatusUseCase = getCurrentUserStatusUseCase,
            setCurrentUserStatusUseCase = setCurrentUserStatusUseCase
        )
    }

    @ParameterizedTest(name = "test that result getting update when calling setCurrentUserStatusUseCase {0} successfully")
    @EnumSource(UserChatStatus::class)
    fun `test that result getting update when calling setCurrentUserStatusUseCase successfully`(
        status: UserChatStatus,
    ) = runTest {
        initTestClass()
        underTest.state.test {
            Truth.assertThat(awaitItem().result).isNull()
        }
        whenever(setCurrentUserStatusUseCase(status)).thenReturn(Unit)
        underTest.setUserStatus(status)
        underTest.state.test {
            Truth.assertThat(awaitItem().result?.isSuccess).isTrue()
        }
    }

    @ParameterizedTest(name = "test that result getting update when calling setCurrentUserStatusUseCase {0} failed")
    @EnumSource(UserChatStatus::class)
    fun `test that result getting update when calling setCurrentUserStatusUseCase failed`(
        status: UserChatStatus,
    ) = runTest {
        initTestClass()
        underTest.state.test {
            Truth.assertThat(awaitItem().result).isNull()
        }
        whenever(setCurrentUserStatusUseCase(status)).thenThrow(RuntimeException::class.java)
        underTest.setUserStatus(status)
        underTest.state.test {
            Truth.assertThat(awaitItem().result?.isFailure).isTrue()
        }
    }

    @ParameterizedTest(name = "test that status update correctly when getCurrentUserStatusUseCase returns {0}")
    @EnumSource(UserChatStatus::class)
    fun `test that status update correctly when getCurrentUserStatusUseCase returns`(status: UserChatStatus) =
        runTest {
            whenever(getCurrentUserStatusUseCase()).thenReturn(status)
            initTestClass()
            underTest.state.test {
                Truth.assertThat(awaitItem().status).isEqualTo(status)
            }
        }
}