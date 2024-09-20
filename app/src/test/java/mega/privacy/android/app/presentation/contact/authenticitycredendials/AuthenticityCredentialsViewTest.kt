package mega.privacy.android.app.presentation.contact.authenticitycredendials

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.authenticitycredendials.model.AuthenticityCredentialsState
import mega.privacy.android.app.presentation.contact.authenticitycredendials.view.AuthenticityCredentialsView
import mega.privacy.android.domain.entity.contacts.AccountCredentials
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import mega.privacy.android.app.fromId
import mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class AuthenticityCredentialsViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val userEmail = "test@email.com"
    private val userCredentials = listOf(
        "AG1F",
        "U3FS",
        "KAJ7",
        "5AFS",
        "YAW1",
        "ZVVW",
        "9SD4",
        "09DR",
        "SA1S",
        "OR2S",
    )
    private val userName = "Test Name"
    private val contactCredentials = AccountCredentials.ContactCredentials(
        credentials = userCredentials,
        name = userName,
        email = userEmail
    )
    private val myCredentials = listOf(
        "KDF8",
        "ASDI",
        "9S32",
        "ASH1",
        "ASD0",
        "ASV1",
        "L131",
        "3AS3",
        "AS31",
        "ASDF",
    )

    @Test
    fun test_that_toolbar_title_is_show() {
        initComposeRuleContent()
        composeTestRule.onNodeWithText(R.string.contact_approve_credentials_toolbar_title)
            .assertExists()
    }

    @Test
    fun test_that_contact_credentials_label_is_show() {
        initComposeRuleContent()
        val label = userName
        composeTestRule.onNodeWithText(label).assertExists()
    }

    @Test
    fun test_that_contact_email_is_show() {
        initComposeRuleContent()
        composeTestRule.onNodeWithText(userEmail).assertExists()
    }

    @Test
    fun test_thant_contact_credentials_is_shown() {
        initComposeRuleContent()
        userCredentials.forEach { credential ->
            composeTestRule.onNodeWithText(credential).assertExists()
        }
    }

    @Test
    fun test_that_verify_button_is_show() {
        initComposeRuleContent()
        composeTestRule.onNodeWithText(R.string.contact_verify_credentials_mark_as_verified_text)
            .assertExists()
        composeTestRule.onNodeWithText(R.string.action_reset).assertDoesNotExist()
    }

    @Test
    fun test_that_verify_button_performs_action() {
        val onButtonClicked = mock<() -> Unit>()
        initComposeRuleContent(onButtonClicked = onButtonClicked)
        composeTestRule.onNodeWithText(R.string.contact_verify_credentials_mark_as_verified_text)
            .performClick()

        verify(onButtonClicked).invoke()
    }

    @Test
    fun test_that_reset_button_is_show() {
        initComposeRuleContent(
            AuthenticityCredentialsState(
                contactCredentials = contactCredentials,
                areCredentialsVerified = true,
                myAccountCredentials = AccountCredentials.MyAccountCredentials(myCredentials),
            )
        )
        composeTestRule.onNodeWithText(R.string.contact_verify_credentials_mark_as_verified_text)
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(R.string.action_reset).assertExists()
    }

    @Test
    fun test_that_explanation_label_is_show() {
        initComposeRuleContent()
        composeTestRule.onNodeWithText(R.string.contact_approve_credentials_contact_verify_description)
            .assertExists()
    }

    @Test
    fun test_that_your_credentials_label_is_shown() {
        initComposeRuleContent()
        val label = fromId(R.string.label_your_credentials)
        composeTestRule.onNodeWithText(label).assertExists()
    }

    @Test
    fun test_that_my_credentials_is_shown() {
        initComposeRuleContent()
        myCredentials.forEach { credential ->
            composeTestRule.onNodeWithText(credential).assertExists()
        }
    }

    @Test
    fun test_that_error_is_displayed_if_present() {
        val error = R.string.check_internet_connection_error
        initComposeRuleContent(
            AuthenticityCredentialsState(
                contactCredentials = contactCredentials,
                myAccountCredentials = AccountCredentials.MyAccountCredentials(myCredentials),
                error = error,
            )
        )

        composeTestRule.onNodeWithText(error).assertExists()
    }

    @Test
    fun test_that_contact_verification_text_views_exists() {
        initComposeRuleContent(
            AuthenticityCredentialsState(
                contactCredentials = contactCredentials,
                myAccountCredentials = AccountCredentials.MyAccountCredentials(myCredentials),
                showContactVerificationBanner = true,
            )
        )
        composeTestRule.onNodeWithTag("CONTACT_VERIFICATION_BANNER_VIEW").assertExists()
        composeTestRule.onNodeWithText(R.string.contact_approve_credentials_secure_share_description)
            .assertExists()
        composeTestRule.onNodeWithText(R.string.contact_approve_credentials_toolbar_title)
            .assertExists()
    }

    private fun initComposeRuleContent(
        state: AuthenticityCredentialsState = AuthenticityCredentialsState(
            contactCredentials = contactCredentials,
            myAccountCredentials = AccountCredentials.MyAccountCredentials(myCredentials),
        ),
        onButtonClicked: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AuthenticityCredentialsView(state = state,
                onButtonClicked = onButtonClicked,
                onBackPressed = {},
                onScrollChange = {},
                onErrorShown = {})
        }
    }
}