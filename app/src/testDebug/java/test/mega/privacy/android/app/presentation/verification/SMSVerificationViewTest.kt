package mega.privacy.android.app.presentation.verification

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.verification.model.SMSVerificationUIState
import mega.privacy.android.app.presentation.verification.view.LOGOUT_BUTTON_TEST_TAG
import mega.privacy.android.app.presentation.verification.view.NEXT_BUTTON_TEST_TAG
import mega.privacy.android.app.presentation.verification.view.NOT_NOW_BUTTON_TEST_TAG
import mega.privacy.android.app.presentation.verification.view.SMSVerificationView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import mega.privacy.android.app.onNodeWithText

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SMSVerificationViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private val headerText = "A"
    private val infoText = "B"
    private val countryCodeText = "C"
    private val phoneNumber = "012345678"

    @Test
    @Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
    fun `test that default views are showed with initial state`() {
        val state = getDefaultState()
        composeTestRule.setContent {
            SMSVerificationView(
                state = state,
                {}, {}, {}, {}, {}, {}, {}
            )
        }
        with(composeTestRule) {
            onNodeWithText(state.headerText).assertIsDisplayed()
            onNodeWithText(state.infoText).assertIsDisplayed()
            onNodeWithText(state.countryCodeText).assertIsDisplayed()
            onNodeWithText(R.string.verify_account_phone_number_placeholder).assertIsDisplayed()
            onNodeWithTag(NEXT_BUTTON_TEST_TAG).assertIsDisplayed()
            onNodeWithTag(NEXT_BUTTON_TEST_TAG).assertIsEnabled()
        }
    }

    @Test
    fun `test that warning is shown when country code is not valid`() {
        val state = getDefaultState().copy(countryCodeText = "")
        composeTestRule.setContent {
            SMSVerificationView(
                state = state,
                {}, {}, {}, {}, {}, {}, {}
            )
        }
        with(composeTestRule) {
            onNodeWithText(R.string.verify_account_invalid_country_code).assertIsDisplayed()
        }
    }

    @Test
    @Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
    fun `test that warning is shown when phone number is not valid`() {
        val state = getDefaultState().copy(countryCodeText = "", isPhoneNumberValid = false)
        composeTestRule.setContent {
            SMSVerificationView(
                state = state,
                {}, {}, {}, {}, {}, {}, {}
            )
        }
        with(composeTestRule) {
            onNodeWithText(R.string.verify_account_phone_number_placeholder)
                .assertIsDisplayed()
            onNodeWithText(R.string.verify_account_invalid_phone_number)
                .assertIsDisplayed()
        }
    }

    @Test
    @Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
    fun `test that warning is shown when sending sms code returns error`() {
        val errorText = "You have reached your daily limit"
        val state = getDefaultState().copy(
            countryCodeText = "",
            isPhoneNumberValid = true,
            phoneNumberErrorText = errorText
        )
        composeTestRule.setContent {
            SMSVerificationView(
                state = state,
                {}, {}, {}, {}, {}, {}, {}
            )
        }
        with(composeTestRule) {
            onNodeWithText(errorText)
                .assertIsDisplayed()
        }
    }

    @Test
    @Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
    fun `test that not now button is displayed when user is not locked`() {
        val state = getDefaultState().copy(
            countryCodeText = "",
            isPhoneNumberValid = false,
            isUserLocked = false
        )
        composeTestRule.setContent {
            SMSVerificationView(
                state = state,
                {}, {}, {}, {}, {}, {}, {}
            )
        }
        composeTestRule.onNodeWithTag(NOT_NOW_BUTTON_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
    fun `test that next button is disabled when isNextEnabled is false`() {
        val state = getDefaultState().copy(
            countryCodeText = "",
            isPhoneNumberValid = false,
            isUserLocked = false,
            isNextEnabled = false
        )
        composeTestRule.setContent {
            SMSVerificationView(
                state = state,
                {}, {}, {}, {}, {}, {}, {}
            )
        }
        composeTestRule.onNodeWithTag(NEXT_BUTTON_TEST_TAG).assertIsNotEnabled()
    }

    @Test
    @Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
    fun `test that logout text button is displayed when user is locked`() {
        val state = getDefaultState().copy(
            countryCodeText = "",
            isPhoneNumberValid = false,
            isUserLocked = true
        )
        composeTestRule.setContent {
            SMSVerificationView(
                state = state,
                {}, {}, {}, {}, {}, {}, {}
            )
        }
        composeTestRule.onNodeWithTag(LOGOUT_BUTTON_TEST_TAG)
            .assertIsDisplayed()
    }

    private fun getDefaultState() = SMSVerificationUIState(
        headerText = headerText,
        infoText = infoText,
        countryCodeText = countryCodeText,
        phoneNumber = phoneNumber
    )
}
