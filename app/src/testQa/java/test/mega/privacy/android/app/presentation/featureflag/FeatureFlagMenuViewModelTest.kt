package mega.privacy.android.app.presentation.featureflag

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetAllFeatureFlags
import mega.privacy.android.app.domain.usecase.SetFeatureFlag
import mega.privacy.android.app.presentation.featureflag.FeatureFlagMenuViewModel
import mega.privacy.android.app.presentation.featureflag.model.toFeatureFlag
import mega.privacy.android.domain.entity.Feature
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class FeatureFlagMenuViewModelTest {
    private lateinit var underTest: FeatureFlagMenuViewModel
    private val setFeatureFlag = mock<SetFeatureFlag>()
    private val getAllFeatureFlags = mock<GetAllFeatureFlags> {
        on { invoke() }.thenReturn(emptyFlow())
    }
    private val scheduler = TestCoroutineScheduler()
    private val standardDispatcher = StandardTestDispatcher(scheduler)

    @Before
    fun setUp() {
        Dispatchers.setMain(standardDispatcher)
        underTest =
            FeatureFlagMenuViewModel(
                setFeatureFlag = setFeatureFlag,
                getAllFeatureFlags = getAllFeatureFlags,
                featureFlagMapper = ::toFeatureFlag,
            )
    }


    @Test
    fun `test that initial state is returned as empty`() {
        runTest {
            underTest.state.test {
                val initial = awaitItem()
                assertTrue(initial.featureFlags.isEmpty())
            }
        }
    }

    @Test
    fun `test that features list is not empty`() {
        val feature = mock<Feature> { on { name }.thenReturn("featureName") }
        val map = mapOf(feature to false)
        runTest {
            whenever(getAllFeatureFlags()).thenReturn(flowOf(map))
            underTest.state.map {
                assertEquals(it.featureFlags[0].featureName, "featureName")
                assertEquals(it.featureFlags[0].isEnabled, false)
            }
        }
    }

    @Test
    fun `test that insert feature flag use case gets called`() {
        runTest {
            underTest.setFeatureEnabled("featureName", false)
            scheduler.advanceUntilIdle()
            verify(setFeatureFlag, times(1))("featureName", false)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}