package mega.privacy.android.shared.original.core.ui.controls.chat.messages.file

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileContainerMessageViewTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that load progress indicator is shown when loadProgress is not null`() {
        initComposeRuleContent(
            loadProgress = .5f,
        )
        composeRule.onNodeWithTag(
            FILE_MESSAGE_VIEW_PROGRESS_TEST_TAG,
            useUnmergedTree = true
        )
            .assertExists()
    }

    @Test
    fun `test that load progress indicator is not shown when loadProgress is 1`() {
        initComposeRuleContent(
            loadProgress = 1f,
        )
        composeRule.onNodeWithTag(
            FILE_MESSAGE_VIEW_PROGRESS_TEST_TAG,
            useUnmergedTree = true
        )
            .assertDoesNotExist()
    }

    @Test
    fun `test that load progress overlay is shown when loadProgress is not null`() {

        initComposeRuleContent(
            loadProgress = .5f,
        )
        composeRule.onNodeWithTag(
            FILE_MESSAGE_VIEW_OVERLAY_TEST_TAG,
            useUnmergedTree = true
        ).assertExists()
    }

    @Test
    fun `test that load progress overlay is not shown when loadProgress is 1`() {

        initComposeRuleContent(
            loadProgress = 1f,
        )
        composeRule.onNodeWithTag(
            FILE_MESSAGE_VIEW_OVERLAY_TEST_TAG,
            useUnmergedTree = true
        ).assertDoesNotExist()
    }

    private fun initComposeRuleContent(
        loadProgress: Float? = null,
    ) {
        composeRule.setContent {
            FileContainerMessageView(
                loadProgress = loadProgress,
            ) {
                Box(modifier = Modifier.size(10.dp))
            }
        }
    }
}


