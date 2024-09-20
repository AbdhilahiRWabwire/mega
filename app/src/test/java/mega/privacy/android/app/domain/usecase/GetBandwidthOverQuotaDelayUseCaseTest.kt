package mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.quota.GetBandwidthOverQuotaDelayUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class GetBandwidthOverQuotaDelayUseCaseTest {
    private lateinit var underTest: GetBandwidthOverQuotaDelayUseCase
    private val repository: NodeRepository = mock()

    @Before
    fun setUp() {
        underTest = GetBandwidthOverQuotaDelayUseCase(repository = repository)
    }

    @Test
    fun `test that get banner quota time`() = runTest {
        val timer = 1000L
        whenever(repository.getBannerQuotaTime()).thenReturn(timer)
        val testTimer = underTest()
        Truth.assertThat(testTimer).isEqualTo(timer)
    }
}