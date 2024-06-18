package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMonitorUserUpdatesTest {
    private lateinit var underTest: MonitorUserUpdates

    private val currentUserId = UserId(1L)

    private val accountRepository = mock<AccountRepository> {
        onBlocking { getLoggedInUserId() }.thenReturn(currentUserId)
    }

    @Before
    fun setUp() {
        underTest = MonitorUserUpdates(
            accountRepository = accountRepository,
        )
    }

    @Test
    fun `test that value that matches current user is returned`() = runTest {
        val expected = UserChanges.Alias
        whenever(accountRepository.monitorUserUpdates()).thenReturn(
            flowOf(
                UserUpdate(
                    changes = mapOf(currentUserId to listOf(expected)),
                    emailMap = emptyMap()
                )
            )
        )

        underTest().test {
            assertThat(awaitItem()).isEqualTo(expected)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `test that a value that does not match the current user is not returned`() = runTest {
        val expected = UserChanges.Alias
        whenever(accountRepository.monitorUserUpdates()).thenReturn(
            flowOf(
                UserUpdate(
                    changes = mapOf(UserId(currentUserId.id + 1) to listOf(expected)),
                    emailMap = emptyMap()
                )
            )
        )

        underTest().test {
            awaitComplete()
        }
    }

    @Test
    fun `test that multiple items in a single event are all returned`() = runTest {
        val expected = listOf(
            UserChanges.Alias,
            UserChanges.CameraUploadsFolder,
            UserChanges.CookieSettings,
        )
        whenever(accountRepository.monitorUserUpdates()).thenReturn(
            flowOf(
                UserUpdate(
                    changes = mapOf(currentUserId to expected),
                    emailMap = emptyMap()
                )
            )
        )

        underTest().test {
            expected.forEach {
                assertThat(awaitItem()).isEqualTo(it)
            }
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `test that multiple items from multiple events are all returned`() = runTest {
        val expected1 = listOf(
            UserChanges.Alias,
            UserChanges.CameraUploadsFolder,
            UserChanges.CookieSettings,
        )
        val expected2 = listOf(
            UserChanges.DisableVersions,
            UserChanges.Avatar,
            UserChanges.CookieSettings,
        )

        whenever(accountRepository.monitorUserUpdates()).thenReturn(
            flowOf(
                UserUpdate(changes = mapOf(currentUserId to expected1), emailMap = emptyMap()),
                UserUpdate(changes = mapOf(currentUserId to expected2), emailMap = emptyMap()),
            )
        )

        underTest().test {
            (expected1 + expected2).forEach {
                assertThat(awaitItem()).isEqualTo(it)
            }
            cancelAndConsumeRemainingEvents()
        }
    }
}