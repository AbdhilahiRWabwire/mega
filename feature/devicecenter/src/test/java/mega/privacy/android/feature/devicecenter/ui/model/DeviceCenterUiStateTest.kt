package mega.privacy.android.feature.devicecenter.ui.model

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.icon.FolderIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Test class for [DeviceCenterUiState]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeviceCenterUiStateTest {

    private lateinit var underTest: DeviceCenterUiState

    private val ownDeviceFolderUINode = NonBackupDeviceFolderUINode(
        id = "ABCD-EFGH",
        name = "Camera uploads",
        icon = FolderIconType.CameraUploads,
        status = DeviceCenterUINodeStatus.UpToDate,
        rootHandle = 789012L,
        localFolderPath = "storage/emulated/0/DCIM/Camera",
    )
    private val ownDeviceUINode = OwnDeviceUINode(
        id = "1234-5678",
        name = "Own Device",
        icon = DeviceIconType.Android,
        status = DeviceCenterUINodeStatus.UpToDate,
        folders = listOf(ownDeviceFolderUINode),
    )

    @ParameterizedTest(name = "when selectedDevice is {0}, then itemsToDisplay is {1}")
    @MethodSource("provideParameters")
    fun `test that itemsToDisplay uses the correct data`(
        selectedDevice: DeviceUINode?,
        expectedItemsToDisplay: List<DeviceCenterUINode>,
    ) {
        underTest = DeviceCenterUiState(
            devices = listOf(ownDeviceUINode),
            selectedDevice = selectedDevice,
        )
        assertThat(underTest.itemsToDisplay).isEqualTo(expectedItemsToDisplay)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(null, listOf(ownDeviceUINode)),
        Arguments.of(ownDeviceUINode, listOf(ownDeviceFolderUINode)),
    )
}