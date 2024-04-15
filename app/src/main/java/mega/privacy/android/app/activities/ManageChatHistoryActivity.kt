package mega.privacy.android.app.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.NumberPicker.OnScrollListener
import android.widget.NumberPicker.OnValueChangeListener
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.ActivityManageChatHistoryBinding
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.ClearChatConfirmationDialog
import mega.privacy.android.app.presentation.meeting.managechathistory.ManageChatHistoryViewModel
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ChatHistoryRetentionOption
import mega.privacy.android.app.presentation.meeting.managechathistory.view.screen.CHAT_HISTORY_RETENTION_TIME_CONFIRMATION_TAG
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants.CHAT_ID
import mega.privacy.android.app.utils.Constants.DISABLED_RETENTION_TIME
import mega.privacy.android.app.utils.Constants.EMAIL
import mega.privacy.android.app.utils.Constants.SECONDS_IN_DAY
import mega.privacy.android.app.utils.Constants.SECONDS_IN_HOUR
import mega.privacy.android.app.utils.Constants.SECONDS_IN_MONTH_30
import mega.privacy.android.app.utils.Constants.SECONDS_IN_WEEK
import mega.privacy.android.app.utils.Constants.SECONDS_IN_YEAR
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialogWithRadioButtons
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.theme.MegaAppTheme
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ManageChatHistoryActivity : PasscodeActivity(), View.OnClickListener {
    companion object {
        private const val OPTION_HOURS = 0
        private const val OPTION_DAYS = 1
        private const val OPTION_MONTHS = 3
        private const val OPTION_WEEKS = 2
        private const val OPTION_YEARS = 4

        private const val MINIMUM_VALUE_NUMBER_PICKER = 1
        private const val DAYS_IN_A_MONTH_VALUE = 30
        private const val MAXIMUM_VALUE_NUMBER_PICKER_HOURS = 24
        private const val MAXIMUM_VALUE_NUMBER_PICKER_DAYS = 31
        private const val MAXIMUM_VALUE_NUMBER_PICKER_WEEKS = 4
        private const val MAXIMUM_VALUE_NUMBER_PICKER_MONTHS = 12
        private const val MINIMUM_VALUE_TEXT_PICKER = 0
        private const val MAXIMUM_VALUE_TEXT_PICKER = 4
    }

    /**
     * Current theme
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    internal val viewModel: ManageChatHistoryViewModel by viewModels()

    private lateinit var binding: ActivityManageChatHistoryBinding

    private val onBackPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            retryConnectionsAndSignalPresence()
            finish()
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent == null || intent.extras == null) {
            Timber.e("Cannot init view, Intent is null")
            finish()
        }

        viewModel.initializeChatRoom(
            chatId = intent.extras?.getLong(CHAT_ID),
            email = intent.extras?.getString(EMAIL)
        )

        collectFlows()

        binding = ActivityManageChatHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.composeView.setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val chatRoomUiState by viewModel.chatRoomUiState.collectAsStateWithLifecycle()

            MegaAppTheme(isDark = themeMode.isDarkMode()) {
                if (uiState.shouldShowClearChatConfirmation) {
                    chatRoomUiState?.apply {
                        ClearChatConfirmationDialog(
                            isMeeting = isMeeting,
                            onConfirm = {
                                viewModel.apply {
                                    dismissClearChatConfirmation()
                                    clearChatHistory(chatId)
                                }
                            },
                            onDismiss = viewModel::dismissClearChatConfirmation
                        )
                    }
                }

                if (uiState.shouldShowHistoryRetentionConfirmation) {
                    ConfirmationDialogWithRadioButtons(
                        modifier = Modifier
                            .semantics { testTagsAsResourceId = true }
                            .testTag(CHAT_HISTORY_RETENTION_TIME_CONFIRMATION_TAG),
                        titleText = stringResource(id = R.string.title_properties_history_retention),
                        subTitleText = stringResource(id = R.string.subtitle_properties_manage_chat),
                        radioOptions = ChatHistoryRetentionOption.entries,
                        initialSelectedOption = uiState.selectedHistoryRetentionTimeOption,
                        optionDescriptionMapper = @Composable {
                            stringResource(id = it.stringId)
                        },
                        onOptionSelected = viewModel::updateHistoryRetentionTimeConfirmation,
                        confirmButtonText = stringResource(id = uiState.confirmButtonStringId),
                        isConfirmButtonEnable = { uiState.isConfirmButtonEnable },
                        onConfirmRequest = {
                            viewModel.apply {
                                onNewRetentionTimeConfirmed(it)
                                dismissHistoryRetentionConfirmation()
                            }
                        },
                        cancelButtonText = stringResource(id = R.string.general_cancel),
                        onDismissRequest = viewModel::dismissHistoryRetentionConfirmation,
                    )
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, onBackPressCallback)
    }

    private fun collectFlows() {
        collectFlow(viewModel.uiState) { uiState ->
            if (uiState.shouldNavigateUp) {
                finish()
                viewModel.onNavigatedUp()
            }

            if (uiState.shouldShowCustomTimePicker) {
                showPickers(viewModel.retentionTimeUiState.value)
                viewModel.onCustomTimePickerSet()
            }
        }

        collectFlow(viewModel.retentionTimeUiState) { retentionTime ->
            updateRetentionTimeUI(retentionTime)
        }

        collectFlow(viewModel.chatRoomUiState) { chatRoom ->
            setupUI(chatRoom)
        }
    }

    private fun setupUI(chat: ChatRoom?) {
        setSupportActionBar(binding.manageChatToolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setHomeButtonEnabled(true)
        actionBar?.title = if (chat?.isMeeting == true) {
            getString(R.string.meetings_manage_history_view_title)
        } else {
            getString(R.string.title_properties_manage_chat)
        }

        binding.historyRetentionSwitch.isClickable = false
        binding.historyRetentionSwitch.isChecked = false
        binding.pickerLayout.visibility = View.GONE
        binding.separator.visibility = View.GONE

        if (chat == null) {
            Timber.d("The chat does not exist")
            binding.historyRetentionSwitchLayout.setOnClickListener(null)
            binding.clearChatHistoryLayout.setOnClickListener(null)
            binding.retentionTimeTextLayout.setOnClickListener(null)
            binding.retentionTimeSubtitle.text =
                getString(R.string.subtitle_properties_history_retention)
            binding.retentionTime.visibility = View.GONE
        } else {
            Timber.d("The chat exists")
            binding.historyRetentionSwitchLayout.setOnClickListener(this)
            binding.clearChatHistoryLayout.setOnClickListener(this)
            binding.clearChatHistoryLayoutTitle.text = if (chat.isMeeting) {
                getString(R.string.meetings_manage_history_clear)
            } else {
                getString(R.string.title_properties_clear_chat_history)
            }

            val seconds = chat.retentionTime
            updateRetentionTimeUI(seconds)

            binding.numberPicker.setOnScrollListener(onScrollListenerPickerNumber)
            binding.numberPicker.setOnValueChangedListener(onValueChangeListenerPickerNumber)
            binding.textPicker.setOnScrollListener(onScrollListenerPickerText)
            binding.textPicker.setOnValueChangedListener(onValueChangeListenerPickerText)
            binding.pickerButton.setOnClickListener(this)
        }
    }

    private var onValueChangeListenerPickerNumber =
        OnValueChangeListener { _, oldValue, newValue ->
            updateTextPicker(oldValue, newValue)
        }

    private var onValueChangeListenerPickerText =
        OnValueChangeListener { textPicker, _, _ ->
            updateNumberPicker(textPicker.value)
        }

    private var onScrollListenerPickerNumber =
        OnScrollListener { _, scrollState ->
            if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                updateOptionsAccordingly()
            }
        }

    private var onScrollListenerPickerText =
        OnScrollListener { _, scrollState ->
            if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                updateOptionsAccordingly()
            }
        }

    /**
     * Method that controls and shows the initial UI of the picket elements.
     *
     * @param seconds The time the retention time is enabled.
     */
    fun showPickers(seconds: Long) {
        Timber.d("Show the pickers")
        binding.pickerLayout.visibility = View.VISIBLE
        binding.separator.visibility = View.VISIBLE

        binding.numberPicker.wrapSelectorWheel = true
        binding.textPicker.wrapSelectorWheel = true

        binding.textPicker.minimumWidth = MAXIMUM_VALUE_TEXT_PICKER
        binding.numberPicker.minValue = MINIMUM_VALUE_NUMBER_PICKER
        binding.textPicker.minValue = MINIMUM_VALUE_TEXT_PICKER
        binding.textPicker.maxValue = MAXIMUM_VALUE_TEXT_PICKER

        if (seconds == DISABLED_RETENTION_TIME) {
            updatePickersValues(
                MINIMUM_VALUE_TEXT_PICKER,
                MAXIMUM_VALUE_NUMBER_PICKER_HOURS,
                MINIMUM_VALUE_NUMBER_PICKER
            )
        } else {
            checkPickersValues(seconds)
        }
    }

    /**
     * Method for filling the text picker array from the value of the picker number value.
     *
     * @param value The current value of number picker.
     */
    private fun fillPickerText(value: Int) {
        binding.textPicker.displayedValues = null
        val arrayString: Array<String> = arrayOf(
            resources.getQuantityString(
                R.plurals.retention_time_picker_hours,
                value
            ).lowercase(Locale.getDefault()),
            resources.getQuantityString(
                R.plurals.retention_time_picker_days,
                value
            ).lowercase(Locale.getDefault()),
            resources.getQuantityString(
                R.plurals.retention_time_picker_weeks,
                value
            ).lowercase(Locale.getDefault()),
            resources.getQuantityString(
                R.plurals.retention_time_picker_months,
                value
            ).lowercase(Locale.getDefault()),
            getString(R.string.retention_time_picker_year)
                .lowercase(Locale.getDefault())
        )
        binding.textPicker.displayedValues = arrayString
    }

    /**
     * Updates the initial values of the pickers.
     *
     * @param textValue The current value of text picker
     * @param maximumValue The maximum value of numbers picker
     * @param numberValue The current value of number picker
     */
    private fun updatePickersValues(textValue: Int, maximumValue: Int, numberValue: Int) {
        if (maximumValue < numberValue) {
            binding.textPicker.value = OPTION_HOURS
            binding.numberPicker.maxValue = MAXIMUM_VALUE_NUMBER_PICKER_HOURS
            binding.numberPicker.value = MINIMUM_VALUE_NUMBER_PICKER

        } else {
            binding.textPicker.value = textValue
            binding.numberPicker.maxValue = maximumValue
            binding.numberPicker.value = numberValue
        }

        fillPickerText(binding.numberPicker.value)
    }

    /**
     * Controls the initial values of the pickers.
     *
     * @param seconds The retention time in seconds.
     */
    private fun checkPickersValues(seconds: Long) {
        val numberYears = seconds / SECONDS_IN_YEAR
        val years = seconds - numberYears * SECONDS_IN_YEAR

        if (years == 0L) {
            updatePickersValues(OPTION_YEARS, MINIMUM_VALUE_NUMBER_PICKER, numberYears.toInt())
            return
        }

        val numberMonths = seconds / SECONDS_IN_MONTH_30
        val months = seconds - numberMonths * SECONDS_IN_MONTH_30

        if (months == 0L) {
            updatePickersValues(
                OPTION_MONTHS,
                MAXIMUM_VALUE_NUMBER_PICKER_MONTHS,
                numberMonths.toInt()
            )
            return
        }

        val numberWeeks = seconds / SECONDS_IN_WEEK
        val weeks = seconds - numberWeeks * SECONDS_IN_WEEK

        if (weeks == 0L) {
            updatePickersValues(
                OPTION_WEEKS,
                MAXIMUM_VALUE_NUMBER_PICKER_WEEKS,
                numberWeeks.toInt()
            )
            return
        }

        val numberDays = seconds / SECONDS_IN_DAY
        val days = seconds - numberDays * SECONDS_IN_DAY

        if (days == 0L) {
            updatePickersValues(OPTION_DAYS, MAXIMUM_VALUE_NUMBER_PICKER_DAYS, numberDays.toInt())
            return
        }

        val numberHours = seconds / SECONDS_IN_HOUR
        val hours = seconds - numberHours * SECONDS_IN_HOUR

        if (hours == 0L) {
            updatePickersValues(
                OPTION_HOURS,
                MAXIMUM_VALUE_NUMBER_PICKER_HOURS,
                numberHours.toInt()
            )
        }
    }

    /**
     * Updates the values of the text picker according to the current value of the number picker.
     *
     * @param oldValue the previous value of the number picker
     * @param newValue the current value of the number picker
     */
    private fun updateTextPicker(oldValue: Int, newValue: Int) {
        if ((oldValue == 1 && newValue == 1) || (oldValue > 1 && newValue > 1))
            return

        if ((oldValue == 1 && newValue > 1) || (newValue == 1 && oldValue > 1)) {
            fillPickerText(newValue)
            binding.textPicker.minimumWidth = MAXIMUM_VALUE_TEXT_PICKER
        }
    }

    /**
     * Method that transforms the chosen option into the most correct form:
     * - If the option selected is 24 hours, it becomes 1 day.
     * - If the selected option is 31 days, it becomes 1 month.
     * - If the selected option is 4 weeks, it becomes 1 month.
     * - If the selected option is 12 months, it becomes 1 year.
     */
    private fun updateOptionsAccordingly() {
        if (binding.textPicker.value == OPTION_HOURS &&
            binding.numberPicker.value == MAXIMUM_VALUE_NUMBER_PICKER_HOURS
        ) {
            updatePickersValues(
                OPTION_DAYS,
                getMaximumValueOfNumberPicker(OPTION_DAYS),
                MINIMUM_VALUE_NUMBER_PICKER
            )
            return
        }

        if (binding.textPicker.value == OPTION_DAYS &&
            binding.numberPicker.value == DAYS_IN_A_MONTH_VALUE
        ) {
            updatePickersValues(
                OPTION_MONTHS,
                getMaximumValueOfNumberPicker(OPTION_MONTHS),
                MINIMUM_VALUE_NUMBER_PICKER
            )
            return
        }

        if (binding.textPicker.value == OPTION_WEEKS &&
            binding.numberPicker.value == MAXIMUM_VALUE_NUMBER_PICKER_WEEKS
        ) {
            updatePickersValues(
                OPTION_MONTHS,
                getMaximumValueOfNumberPicker(OPTION_MONTHS),
                MINIMUM_VALUE_NUMBER_PICKER
            )
            return
        }

        if (binding.textPicker.value == OPTION_MONTHS &&
            binding.numberPicker.value == MAXIMUM_VALUE_NUMBER_PICKER_MONTHS
        ) {
            updatePickersValues(
                OPTION_YEARS,
                getMaximumValueOfNumberPicker(OPTION_YEARS),
                MINIMUM_VALUE_NUMBER_PICKER
            )
            return
        }
    }

    /**
     *
     * Method for getting the maximum value of the picker number from a value.
     * @param value the value
     */
    private fun getMaximumValueOfNumberPicker(value: Int): Int {
        when (value) {
            OPTION_HOURS -> {
                return MAXIMUM_VALUE_NUMBER_PICKER_HOURS
            }

            OPTION_DAYS -> {
                return MAXIMUM_VALUE_NUMBER_PICKER_DAYS
            }

            OPTION_WEEKS -> {
                return MAXIMUM_VALUE_NUMBER_PICKER_WEEKS
            }

            OPTION_MONTHS -> {
                return MAXIMUM_VALUE_NUMBER_PICKER_MONTHS
            }

            OPTION_YEARS -> {
                return MINIMUM_VALUE_NUMBER_PICKER
            }

            else -> {
                return 0
            }
        }
    }

    /**
     * Method that updates the values of the number picker according to the current value of the text picker.
     *
     * @param value the current value of the text picker
     */
    private fun updateNumberPicker(value: Int) {
        val maximumValue = getMaximumValueOfNumberPicker(value)

        if (binding.numberPicker.value > maximumValue) {
            updateTextPicker(binding.numberPicker.value, MINIMUM_VALUE_NUMBER_PICKER)
            binding.numberPicker.value = MINIMUM_VALUE_NUMBER_PICKER
        }

        binding.numberPicker.maxValue = maximumValue
    }

    /**
     * Method for updating the UI when the retention time is updated.
     *
     * @param seconds The retention time in seconds
     */
    private fun updateRetentionTimeUI(seconds: Long) {
        val timeFormatted = ChatUtil.transformSecondsInString(seconds)
        if (TextUtil.isTextEmpty(timeFormatted)) {
            binding.retentionTimeTextLayout.setOnClickListener(null)
            binding.historyRetentionSwitch.isChecked = false
            binding.retentionTimeSubtitle.text =
                getString(R.string.subtitle_properties_history_retention)
            binding.retentionTime.visibility = View.GONE
        } else {
            binding.retentionTimeTextLayout.setOnClickListener(this)
            binding.historyRetentionSwitch.isChecked = true
            binding.retentionTimeSubtitle.text = getString(R.string.subtitle_properties_manage_chat)
            binding.retentionTime.text = timeFormatted
            binding.retentionTime.visibility = View.VISIBLE
        }

        binding.pickerLayout.visibility = View.GONE
        binding.separator.visibility = View.GONE
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.clear_chat_history_layout -> {
                viewModel.showClearChatConfirmation()
            }

            R.id.history_retention_switch_layout -> {
                if (binding.historyRetentionSwitch.isChecked) {
                    viewModel.setChatRetentionTime(period = DISABLED_RETENTION_TIME)
                } else {
                    viewModel.showHistoryRetentionConfirmation()
                }
            }

            R.id.retention_time_text_layout -> {
                viewModel.showHistoryRetentionConfirmation()
            }

            R.id.picker_button -> {
                binding.pickerLayout.visibility = View.GONE
                binding.separator.visibility = View.GONE
                var secondInOption = 0

                when (binding.textPicker.value) {
                    OPTION_HOURS -> {
                        secondInOption = SECONDS_IN_HOUR
                    }

                    OPTION_DAYS -> {
                        secondInOption = SECONDS_IN_DAY
                    }

                    OPTION_WEEKS -> {
                        secondInOption = SECONDS_IN_WEEK
                    }

                    OPTION_MONTHS -> {
                        secondInOption = SECONDS_IN_MONTH_30
                    }

                    OPTION_YEARS -> {
                        secondInOption = SECONDS_IN_YEAR
                    }
                }

                val totalSeconds = binding.numberPicker.value * secondInOption
                viewModel.setChatRetentionTime(period = totalSeconds.toLong())
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)
            onBackPressedDispatcher.onBackPressed()

        return super.onOptionsItemSelected(item)
    }
}
