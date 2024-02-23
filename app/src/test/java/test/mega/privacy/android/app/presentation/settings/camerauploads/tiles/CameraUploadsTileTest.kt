package test.mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.settings.camerauploads.tiles.CAMERA_UPLOADS_TILE
import mega.privacy.android.app.settings.camerauploads.tiles.CAMERA_UPLOADS_TILE_SWITCH
import mega.privacy.android.app.settings.camerauploads.tiles.CameraUploadsTile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for [CameraUploadsTile]
 */
@RunWith(AndroidJUnit4::class)
internal class CameraUploadsTileTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the tile is shown`() {
        composeTestRule.setContent {
            CameraUploadsTile(
                isChecked = true,
                onCheckedChange = {},
            )
        }
        composeTestRule.onNodeWithTag(CAMERA_UPLOADS_TILE).assertIsDisplayed()
    }

    @Test
    fun `test that the switch is checked when camera uploads is enabled`() {
        composeTestRule.setContent {
            CameraUploadsTile(
                isChecked = true,
                onCheckedChange = {},
            )
        }
        composeTestRule.onNodeWithTag(CAMERA_UPLOADS_TILE_SWITCH).assertIsOn()
    }

    @Test
    fun `test that the switch is not checked when camera uploads is disabled`() {
        composeTestRule.setContent {
            CameraUploadsTile(
                isChecked = false,
                onCheckedChange = {},
            )
        }
        composeTestRule.onNodeWithTag(CAMERA_UPLOADS_TILE_SWITCH).assertIsOff()
    }

    @Test
    fun `test that on checked change is fired when the switch is clicked`() {
        val onCheckedChange = mock<(Boolean) -> Unit>()
        composeTestRule.setContent {
            CameraUploadsTile(
                isChecked = false,
                onCheckedChange = onCheckedChange,
            )
        }
        composeTestRule.onNodeWithTag(CAMERA_UPLOADS_TILE_SWITCH).performClick()
        verify(onCheckedChange).invoke(any())
    }
}