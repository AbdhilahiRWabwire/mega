package mega.privacy.android.core.ui.controls.chat

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.chat.messages.reaction.TEST_TAG_REACTIONS_VIEW
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.core.ui.controls.chat.messages.reaction.reactionsList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ChatMessageContainerTest {
    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    private val onForwardClicked = mock<() -> Unit>()

    @Test
    fun `test that forward icon show correctly`() {
        initComposeRuleContent(
            isMine = false,
            showForwardIcon = true,
            time = null,
        )
        composeRule.onNodeWithTag(TEST_TAG_FORWARD_ICON).assertExists()
    }

    @Test
    fun `test that forward icon does not show`() {
        initComposeRuleContent(
            isMine = false,
            showForwardIcon = false,
            time = null,
        )
        composeRule.onNodeWithTag(TEST_TAG_FORWARD_ICON).assertDoesNotExist()
    }

    @Test
    fun `test that time show correctly`() {
        val time = "12:00"
        initComposeRuleContent(
            isMine = false,
            showForwardIcon = false,
            time = time,
        )
        composeRule.onNodeWithText(time).assertExists()
    }

    @Test
    fun `test that error text shows correctly when send error`() {
        initComposeRuleContent(
            isMine = false,
            showForwardIcon = false,
            time = null,
            isSendError = true,
        )
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.manual_retry_alert)
        ).assertExists()
    }

    @Test
    fun `test that error text does not show when send successfully`() {
        initComposeRuleContent(
            isMine = false,
            showForwardIcon = false,
            time = null,
            isSendError = false,
        )
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.manual_retry_alert)
        ).assertDoesNotExist()
    }

    @Test
    fun `test that reactions are shown if list is not empty`() {
        initComposeRuleContent(
            isMine = false,
            showForwardIcon = false,
            time = null,
            isSendError = false,
            reactions = reactionsList,
        )
        composeRule.onNodeWithTag(TEST_TAG_REACTIONS_VIEW).assertIsDisplayed()
    }

    @Test
    fun `test that reactions are not shown if list is empty`() {
        initComposeRuleContent(
            isMine = false,
            showForwardIcon = false,
            time = null,
            isSendError = false,
            reactions = emptyList(),
        )
        composeRule.onNodeWithTag(TEST_TAG_REACTIONS_VIEW).assertDoesNotExist()
    }

    @Test
    fun `test that forward click is invoked if icon is clicked`() {
        initComposeRuleContent(
            isMine = false,
            showForwardIcon = true,
            time = null,
        )
        composeRule.onNodeWithTag(TEST_TAG_FORWARD_ICON).performClick()
        verify(onForwardClicked).invoke()
    }

    private fun initComposeRuleContent(
        isMine: Boolean,
        showForwardIcon: Boolean,
        time: String?,
        isSendError: Boolean = false,
        reactions: List<UIReaction> = emptyList(),
    ) {
        composeRule.setContent {
            ChatMessageContainer(
                isMine = isMine,
                showForwardIcon = showForwardIcon,
                reactions = reactions,
                onMoreReactionsClick = {},
                onReactionClick = {},
                onForwardClicked = onForwardClicked,
                time = time,
                isSendError = isSendError,
                avatarOrIcon = {},
                content = {},
                onReactionLongClick = {},
            )
        }
    }
}