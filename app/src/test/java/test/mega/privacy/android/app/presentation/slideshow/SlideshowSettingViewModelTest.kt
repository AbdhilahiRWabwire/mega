package test.mega.privacy.android.app.presentation.slideshow

import app.cash.turbine.test
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.slideshow.SlideshowSettingViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed
import mega.privacy.android.domain.usecase.MonitorSlideshowOrderSettingUseCase
import mega.privacy.android.domain.usecase.MonitorSlideshowRepeatSettingUseCase
import mega.privacy.android.domain.usecase.MonitorSlideshowSpeedSettingUseCase
import mega.privacy.android.domain.usecase.SaveSlideshowOrderSettingUseCase
import mega.privacy.android.domain.usecase.SaveSlideshowRepeatSettingUseCase
import mega.privacy.android.domain.usecase.SaveSlideshowSpeedSettingUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.fail

class SlideshowSettingViewModelTest {
    private lateinit var underTest: SlideshowSettingViewModel

    private val monitorSlideshowOrderSettingUseCase: MonitorSlideshowOrderSettingUseCase = mock()
    private val monitorSlideshowSpeedSettingUseCase: MonitorSlideshowSpeedSettingUseCase = mock()
    private val monitorSlideshowRepeatSettingUseCase: MonitorSlideshowRepeatSettingUseCase = mock()
    private val saveSlideshowOrderSettingUseCase: SaveSlideshowOrderSettingUseCase = mock()
    private val saveSlideshowSpeedSettingUseCase: SaveSlideshowSpeedSettingUseCase = mock()
    private val saveSlideshowRepeatSettingUseCase: SaveSlideshowRepeatSettingUseCase = mock()

    @BeforeEach
    fun setUp() {
        whenever(monitorSlideshowOrderSettingUseCase.invoke())
            .thenReturn(flowOf())

        whenever(monitorSlideshowSpeedSettingUseCase.invoke())
            .thenReturn(flowOf())

        whenever(monitorSlideshowRepeatSettingUseCase.invoke())
            .thenReturn(flowOf())
    }

    private fun createSUT() = SlideshowSettingViewModel(
        monitorSlideshowOrderSettingUseCase = monitorSlideshowOrderSettingUseCase,
        monitorSlideshowSpeedSettingUseCase = monitorSlideshowSpeedSettingUseCase,
        monitorSlideshowRepeatSettingUseCase = monitorSlideshowRepeatSettingUseCase,
        saveSlideshowOrderSettingUseCase = saveSlideshowOrderSettingUseCase,
        saveSlideshowSpeedSettingUseCase = saveSlideshowSpeedSettingUseCase,
        saveSlideshowRepeatSettingUseCase = saveSlideshowRepeatSettingUseCase,
    )

    @Test
    fun `test that monitor order setting receives correct result`() = runTest {
        // given
        val expectedSetting = SlideshowOrder.Shuffle
        whenever(monitorSlideshowOrderSettingUseCase.invoke())
            .thenReturn(flowOf(expectedSetting))

        // when
        underTest = createSUT()

        // then
        underTest.state.drop(1).test {
            val actualSetting = awaitItem().order
            assertEquals(expectedSetting, actualSetting)
        }
    }

    @Test
    fun `test that monitor speed setting receives correct result`() = runTest {
        // given
        val expectedSetting = SlideshowSpeed.Normal
        whenever(monitorSlideshowSpeedSettingUseCase.invoke())
            .thenReturn(flowOf(expectedSetting))

        // when
        underTest = createSUT()

        // then
        underTest.state.drop(1).test {
            val actualSetting = awaitItem().speed
            assertEquals(expectedSetting, actualSetting)
        }
    }

    @Test
    fun `test that monitor repeat setting receives correct result`() = runTest {
        // given
        val expectedSetting = true
        whenever(monitorSlideshowRepeatSettingUseCase.invoke())
            .thenReturn(flowOf(expectedSetting))

        // when
        underTest = createSUT()

        // then
        underTest.state.drop(1).test {
            val actualSetting = awaitItem().repeat
            assertEquals(expectedSetting, actualSetting)
        }
    }

    @Test
    fun `test that save order setting works properly`() = runTest {
        // given
        whenever(saveSlideshowOrderSettingUseCase.invoke(any()))
            .thenReturn(Unit)

        // when
        underTest = createSUT()

        // then
        try {
            underTest.saveOrderSetting(order = SlideshowOrder.Shuffle)
        } catch (e: Exception) {
            fail(message = "${e.message}")
        }
    }

    @Test
    fun `test that save speed setting works properly`() = runTest {
        // given
        whenever(saveSlideshowSpeedSettingUseCase.invoke(any()))
            .thenReturn(Unit)

        // when
        underTest = createSUT()

        // then
        try {
            underTest.saveSpeedSetting(speed = SlideshowSpeed.Normal)
        } catch (e: Exception) {
            fail(message = "${e.message}")
        }
    }

    @Test
    fun `test that save repeat setting works properly`() = runTest {
        // given
        whenever(saveSlideshowRepeatSettingUseCase.invoke(any()))
            .thenReturn(Unit)

        // when
        underTest = createSUT()

        // then
        try {
            underTest.saveRepeatSetting(isRepeat = false)
        } catch (e: Exception) {
            fail(message = "${e.message}")
        }
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}
