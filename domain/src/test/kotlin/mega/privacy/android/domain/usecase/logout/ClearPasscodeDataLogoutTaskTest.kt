package mega.privacy.android.domain.usecase.logout

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.security.PasscodeRepository
import mega.privacy.android.domain.usecase.passcode.DisablePasscodeUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class ClearPasscodeDataLogoutTaskTest {
    private lateinit var underTest: ClearPasscodeDataLogoutTask

    private val passcodeRepository = mock<PasscodeRepository>()

    private val disablePasscodeUseCase = mock<DisablePasscodeUseCase>()

    @BeforeEach
    internal fun setUp() {
        underTest = ClearPasscodeDataLogoutTask(
            passcodeRepository = passcodeRepository,
            disablePasscodeUseCase = disablePasscodeUseCase,
        )
    }

    @Test
    internal fun `test that passcode attempts are set to 0`() = runTest {
        underTest()

        verify(passcodeRepository).setFailedAttempts(0)
    }

    @Test
    internal fun `test that passcode enabled state is set to false`() = runTest{
        underTest()

        verify(disablePasscodeUseCase).invoke()
    }
}