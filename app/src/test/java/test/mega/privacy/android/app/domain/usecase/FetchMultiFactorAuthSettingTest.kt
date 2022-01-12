package test.mega.privacy.android.app.domain.usecase


import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.repository.AccountRepository
import mega.privacy.android.app.domain.usecase.DefaultFetchMultiFactorAuthSetting
import mega.privacy.android.app.domain.usecase.FetchMultiFactorAuthSetting
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FetchMultiFactorAuthSettingTest {
    private lateinit var underTest: FetchMultiFactorAuthSetting
    private val accountRepository = mock<AccountRepository>()

    @Before
    fun setUp() {
        underTest = DefaultFetchMultiFactorAuthSetting(accountRepository = accountRepository)
    }


    @Test
    fun `test initial value`() = runTest{
        whenever(accountRepository.isMultiFactorAuthEnabled()).thenReturn(true)
        whenever(accountRepository.monitorMultiFactorAuthChanges()).thenReturn(flowOf())

        underTest().test {
            assertThat(awaitItem()).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `test updates are returned`() = runTest{
        whenever(accountRepository.isMultiFactorAuthEnabled()).thenReturn(true)
        whenever(accountRepository.monitorMultiFactorAuthChanges()).thenReturn(flowOf(false, true))

        underTest().test {
            assertThat(awaitItem()).isTrue()
            assertThat(awaitItem()).isFalse()
            assertThat(awaitItem()).isTrue()
            awaitComplete()
        }
    }
}