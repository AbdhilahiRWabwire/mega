package mega.privacy.android.app.getLink

import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.resources.R as sharedR
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.DatePicker
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialStyledDatePickerDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.contract.ChatExplorerActivityContract
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.databinding.FragmentGetLinkBinding
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.MegaApiUtils.getMegaNodeFolderInfo
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.ThumbnailUtils.getRoundedBitmap
import mega.privacy.android.app.utils.Util.*
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Fragment of [GetLinkActivity] to get or manage a link of a node.
 */
@AndroidEntryPoint
class GetLinkFragment : Fragment(), DatePickerDialog.OnDateSetListener, Scrollable {

    companion object {
        private const val SHARE = 0
        private const val SEND_TO_CHAT = 1
        private const val INVALID_EXPIRATION_TIME = -1L
        private const val LAST_MINUTE = "2359"
        private const val POST_CHECK_SCROLL = 300L
    }

    private val viewModel: GetLinkViewModel by activityViewModels()

    private lateinit var binding: FragmentGetLinkBinding
    private lateinit var chatLauncher: ActivityResultLauncher<Unit?>

    private var passwordVisible = false

    private val handle: Long? by lazy {
        val handle = activity?.intent?.getLongExtra(HANDLE, INVALID_HANDLE)
        handle?.takeIf { it != INVALID_HANDLE }
    }

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatLauncher = registerForActivityResult(ChatExplorerActivityContract()) { data ->
            if (data != null) {
                if (viewModel.shouldShowShareKeyOrPasswordDialog()) {
                    showShareKeyOrPasswordDialog(SEND_TO_CHAT, data)
                } else {
                    viewModel.sendToChat(data, shouldAttachKeyOrPassword = false) { intent ->
                        handleActivityResult(intent)
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentGetLinkBinding.inflate(layoutInflater)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initialize()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun initialize() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)) {
                checkSensitiveItems()
            } else {
                setupView()
                setupObservers()
            }
        }
    }

    private fun initNode() {
        val handle = handle ?: run {
            activity?.finish()
            return
        }
        viewModel.initNode(handle)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }

            R.id.action_share -> {
                when {
                    viewModel.shouldShowShareKeyOrPasswordDialog() ->
                        showShareKeyOrPasswordDialog(SHARE)

                    else -> viewModel.shareLink { intent -> startActivity(intent) }
                }
            }

            R.id.action_chat -> {
                chatLauncher.launch(Unit)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.activity_get_link, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun setupView() {
        binding.scrollViewGetLink.setOnScrollChangeListener { _, _, _, _, _ ->
            checkScroll()
        }

        setThumbnail()

        val node = viewModel.getNode()
        binding.nodeName.text = node?.name
        binding.nodeInfo.text =
            if (node?.isFolder == true) getMegaNodeFolderInfo(node, requireContext())
            else getSizeString(node?.size ?: 0, requireContext())

        binding.learnMoreTextButton.setOnClickListener {
            checkIfShouldHidePassword()
            findNavController().navigate(R.id.learn_more)
        }

        binding.passwordProtectionSetToggle.setOnClickListener { toggleClick() }

        binding.resetPasswordButton.setOnClickListener {
            checkIfShouldHidePassword()
            findNavController().navigate(GetLinkFragmentDirections.setPassword(true))
        }

        binding.removePasswordButton.setOnClickListener { removePasswordClick() }

        binding.keyLayout.isVisible = false
        binding.keySeparator.isVisible = false

        binding.copyLinkButton.setOnClickListener {
            checkIfShouldHidePassword()
            viewModel.copyLink()
        }

        binding.copyKeyIcon.setOnClickListener {
            viewModel.copyLinkKey { copyInfo ->
                copyToClipboard(copyInfo)
            }
        }

        binding.copyLinkIcon.setOnClickListener {
            binding.copyLinkButton.performClick()
        }

        binding.copyKeyIcon.isVisible = false

        binding.copyPasswordButton.setOnClickListener {
            checkIfShouldHidePassword()
            viewModel.copyLinkPassword { copyInfo -> copyToClipboard(copyInfo) }
        }

        binding.expiryDateSwitch.isVisible = viewModel.isPro()

        if (viewModel.isPro()) {
            binding.expiryDateProOnlyText.isVisible = false
            binding.passwordProtectionProOnlyText.isVisible = false
        } else {
            binding.expiryDateProOnlyText.isVisible = true
            binding.passwordProtectionProOnlyText.isVisible = true
        }

        if (node?.isExported == false) {
            viewModel.export(isFirstTime = true)
        }

        binding.scrollViewGetLink.postDelayed(::checkScroll, POST_CHECK_SCROLL)
    }

    private fun setupObservers() {
        viewModel.getLink().observe(viewLifecycleOwner, ::updateLink)
        viewModel.getExpiryDate().observe(viewLifecycleOwner, ::updateExpiryDate)

        viewLifecycleOwner.collectFlow(viewModel.linkCopied) {
            it?.let { pair ->
                copyToClipboard(pair)
                viewModel.resetLink()
            }
        }
        viewLifecycleOwner.collectFlow(viewModel.state) { uiState ->
            binding.keyText.text = uiState.key
            updatePassword(uiState.password)
        }
    }

    private fun checkSensitiveItems() {
        val handle = handle ?: run {
            activity?.finish()
            return
        }

        viewModel.hasSensitiveItemsFlow
            .filterNotNull()
            .onEach { hasSensitiveItems ->
                if (hasSensitiveItems) {
                    showSharingSensitiveItemsWarningDialog()
                } else {
                    initNode()

                    setupView()
                    setupObservers()
                }
            }.launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.checkSensitiveItem(handle)
    }

    override fun onResume() {
        super.onResume()

        updateSendDecryptedKeySeparatelyLayouts()
    }

    /**
     * Disables the password protection.
     */
    private fun removePasswordClick() {
        checkIfShouldHidePassword()
        viewModel.removeLinkWithPassword()
    }

    /**
     * Updates the entire view depending on the enabled options.
     *
     * @param text Text to show as link text.
     */
    private fun updateLink(text: String) {
        binding.linkText.text = text
        val node = viewModel.getNode()
        val alpha = if (node?.isExported == true) ALPHA_VIEW_ENABLED else ALPHA_VIEW_DISABLED
        binding.decryptedKeyLayout.alpha = alpha
        binding.expiryDateLayout.alpha = alpha
        binding.passwordProtectionLayout.alpha = alpha
        if (node?.isExported == true) {
            binding.decryptedKeyLayout.setOnClickListener { sendDecryptedKeySeparatelyClick(false) }
            binding.decryptedKeySwitch.apply {
                setOnClickListener { sendDecryptedKeySeparatelyClick(true) }
                isEnabled = true
                setOnCheckedChangeListener { _, isChecked ->
                    viewModel.updateSendDecryptedKeySeparatelyEnabled(isChecked)
                }
            }

            binding.expiryDateLayout.setOnClickListener { setExpiryDateClick(false) }
            binding.expiryDateSwitch.setOnClickListener { setExpiryDateClick(true) }
            binding.expiryDateSwitch.isEnabled = true
            binding.copyLinkButton.isVisible = true
        } else {
            binding.decryptedKeyLayout.setOnClickListener(null)
            binding.decryptedKeySwitch.setOnClickListener(null)
            binding.decryptedKeySwitch.setOnCheckedChangeListener(null)
            binding.decryptedKeySwitch.isEnabled = false

            binding.expiryDateLayout.setOnClickListener(null)
            binding.expiryDateSwitch.setOnClickListener(null)
            binding.expiryDateSwitch.isEnabled = false

            binding.keySeparator.isVisible = false

            binding.copyLinkButton.isVisible = false
            binding.copyKeyIcon.isVisible = false
        }
    }

    /**
     * Updates the expiry date views.
     *
     * @param date Text to show as expiry date.
     */
    private fun updateExpiryDate(date: String) {
        if (date.isNotEmpty()) {
            binding.expiryDateSwitch.isChecked = true
            binding.expiryDateSetText.isVisible = true
            binding.expiryDateSetText.text = date
            onSetExpiryDateClicked()
        } else {
            binding.expiryDateSwitch.isChecked = false
            binding.expiryDateSetText.isVisible = false
            binding.expiryDateSetText.text = null
        }
    }

    /**
     * Updates the UI of password protection option.
     *
     * @param password The password if set, null otherwise.
     */
    private fun updatePassword(password: String?) {
        val isPasswordSet = !isTextEmpty(password)
        val visibility = if (isPasswordSet) VISIBLE else GONE

        if (isPasswordSet) {
            if (binding.decryptedKeySwitch.isChecked) {
                sendDecryptedKeySeparatelyClick(false)
            }
            binding.passwordProtectionSetText.transformationMethod = PasswordTransformationMethod()
            binding.getLinkAccessSubtitle.text =
                getString(R.string.cloud_drive_subtitle_link_access_password)
        } else {
            binding.getLinkAccessSubtitle.text =
                resources.getQuantityText(R.plurals.cloud_drive_subtitle_links_access_user, 1)
        }

        binding.passwordProtectionArrow.isVisible = isPasswordSet.not() && viewModel.isPro()
        binding.passwordProtectionSetText.visibility = visibility
        binding.passwordProtectionSetText.text = if (isPasswordSet) password else null
        binding.passwordProtectionSetToggle.visibility = visibility

        binding.resetPasswordButton.visibility = visibility
        binding.removePasswordButton.visibility = visibility

        binding.copyPasswordButton.visibility = visibility

        if (isPasswordSet || viewModel.getNode()?.isExported == false) {
            binding.passwordProtectionLayout.setOnClickListener(null)
        } else {
            binding.passwordProtectionLayout.setOnClickListener { setPasswordProtectionClick() }
        }
        if (isPasswordSet) {
            (requireActivity() as SnackbarShower).showSnackbar(getString(R.string.general_link_updated_copy_again))
        }
    }

    /**
     * Sets the thumbnail of the node to which is getting or managing the link.
     */
    private fun setThumbnail() {
        var thumb: Bitmap? = null
        val node = viewModel.getNode()

        if (node?.isFolder == true) {
            binding.nodeThumbnail.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    IconPackR.drawable.ic_folder_medium_solid,
                    null
                )
            )

            return
        } else if (node?.hasThumbnail() == true) {
            thumb = ThumbnailUtils.getThumbnailFromCache(node)
            if (thumb == null) {
                thumb = ThumbnailUtils.getThumbnailFromFolder(node, context)
            }
        }

        if (thumb != null) {
            binding.nodeThumbnail.setImageBitmap(
                getRoundedBitmap(
                    context,
                    thumb,
                    dp2px(THUMB_CORNER_RADIUS_DP)
                )
            )
        } else {
            binding.nodeThumbnail.setImageResource(typeForName(node?.name).iconResourceId)
        }
    }

    /**
     * Manages the click of send decrypted key separately option.
     *
     * @param isSwitchClick True if the click was in the switch, false if it was in other part of the view.
     */
    private fun sendDecryptedKeySeparatelyClick(isSwitchClick: Boolean) {
        if (!isSwitchClick) {
            binding.decryptedKeySwitch.isChecked = !binding.decryptedKeySwitch.isChecked
        }

        if (binding.decryptedKeySwitch.isChecked
            && !viewModel.getLinkWithPassword().isNullOrEmpty()
        ) {
            removePasswordClick()
        }

        updateSendDecryptedKeySeparatelyLayouts()
    }

    /**
     * Updates the UI of the send decrypted key separately option.
     */
    private fun updateSendDecryptedKeySeparatelyLayouts() {
        val visibility = if (binding.decryptedKeySwitch.isChecked) VISIBLE else GONE

        binding.keyLayout.visibility = visibility
        binding.keySeparator.visibility = visibility
        binding.copyKeyIcon.visibility = visibility
    }

    /**
     * Manages the click of set expiry date option.
     *
     * @param isSwitchClick True if the click was in the switch, false if it was in other part of the view.
     */
    private fun setExpiryDateClick(isSwitchClick: Boolean) {
        onSetExpiryDateClicked()
        checkIfShouldHidePassword()
        val node = viewModel.getNode()
        val isPro = viewModel.isPro()
        val hasExpiration = (node?.expirationTime ?: 0) > 0

        if (!isPro || (isSwitchClick && hasExpiration.not())) {
            binding.expiryDateSwitch.isChecked = false
        }

        if (!isPro) {
            showUpgradeToProWarning()
            return
        }

        if (isSwitchClick && hasExpiration) {
            binding.expiryDateSetText.apply {
                isVisible = false
                text = null
            }

            viewModel.export()
        } else {
            showDatePicker()
        }
    }

    /**
     * Shows a warning for free users to upgrade to Pro.
     */
    private fun showUpgradeToProWarning() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.upgrade_pro))
            .setMessage(getString(R.string.link_upgrade_pro_explanation) + "\n")
            .setCancelable(false)
            .setPositiveButton(getString(R.string.button_plans_almost_full_warning)) { _, _ ->
                (requireActivity() as BaseActivity).apply {
                    navigateToUpgradeAccount()
                    finish()
                }
            }
            .setNegativeButton(
                getString(R.string.verify_account_not_now_button)
            ) { _, _ ->
            }
            .create()
            .show()
    }

    /**
     * Shows data picker to choose an expiry date.
     */
    @SuppressLint("RestrictedApi")
    private fun showDatePicker() {
        val node = viewModel.getNode()
        val calendar =
            if (node?.expirationTime == INVALID_EXPIRATION_TIME) Calendar.getInstance()
            else calculateDateFromTimestamp(node?.expirationTime ?: 0)

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = MaterialStyledDatePickerDialog(
            requireContext(),
            R.style.Widget_Mega_DatePickerDialog,
            this, year, month, day
        )
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    /**
     * Manages the click on set password protection option.
     */
    private fun setPasswordProtectionClick() {
        if (viewModel.isPro()) {
            checkIfShouldHidePassword()
            findNavController().navigate(GetLinkFragmentDirections.setPassword(false))
        } else {
            showUpgradeToProWarning()
        }
    }

    /**
     * Checks if should hide the password each time the user clicks on some option.
     */
    private fun checkIfShouldHidePassword() {
        if (passwordVisible) {
            toggleClick()
        }
    }

    /**
     * Manages the click on password toggle by showing or hiding the password.
     */
    private fun toggleClick() {
        if (passwordVisible) {
            binding.passwordProtectionSetText.transformationMethod = PasswordTransformationMethod()
            binding.passwordProtectionSetToggle.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.grey_012_white_038),
                PorterDuff.Mode.SRC_IN
            )
        } else {
            binding.passwordProtectionSetText.transformationMethod = null
            binding.passwordProtectionSetToggle.setColorFilter(
                ColorUtils.getThemeColor(
                    requireContext(),
                    com.google.android.material.R.attr.colorSecondary
                ),
                PorterDuff.Mode.SRC_IN
            )
        }

        passwordVisible = !passwordVisible
    }

    /**
     * Copies a link, decryption key or password into clipboard and shows a snackbar.
     *
     * @param copyInfo First is the content to copy, second the text to show as confirmation.
     */
    private fun copyToClipboard(copyInfo: Pair<String, String>) {
        TextUtil.copyToClipboard(requireActivity(), copyInfo.first)
        (requireActivity() as SnackbarShower).showSnackbar(copyInfo.second)
    }

    /**
     * Shows a warning before share link when the user has the Send decryption key separately or
     * the password protection enabled, asking if they want to share also the key or the password.
     *
     * @param type Indicates if the share is send to chat or share outside the app.
     * @param data Intent containing the info to share to chat or null if is sharing outside the app.
     */
    private fun showShareKeyOrPasswordDialog(type: Int, data: Intent? = null) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(
                getString(
                    if (viewModel.isPasswordSet()) R.string.share_password_warning
                    else R.string.share_key_warning
                ) + "\n"
            )
            .setCancelable(false)
            .setPositiveButton(
                getString(
                    if (viewModel.isPasswordSet()) R.string.button_share_password
                    else R.string.button_share_key
                )
            ) { _, _ ->
                if (type == SHARE) {
                    viewModel.shareLinkAndKeyOrPassword { intent -> startActivity(intent) }
                } else if (type == SEND_TO_CHAT) {
                    viewModel.sendLinkToChat(data, true) { intent ->
                        handleActivityResult(intent)
                    }
                }
            }
            .setNegativeButton(getString(R.string.general_dismiss)) { _, _ ->
                if (type == SHARE) {
                    viewModel.shareCompleteLink { intent -> startActivity(intent) }
                } else if (type == SEND_TO_CHAT) {
                    viewModel.sendLinkToChat(data, false) { intent ->
                        handleActivityResult(intent)
                    }
                }
            }
            .create()
            .show()
    }

    /**
     * Finishes the send to chat action.
     *
     * @param data Intent containing the info to send.
     */
    private fun handleActivityResult(data: Intent?) {
        MegaAttacher(requireActivity() as ActivityLauncher).handleActivityResult(
            REQUEST_CODE_SELECT_CHAT,
            RESULT_OK,
            data,
            requireActivity() as SnackbarShower
        )
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        val cal = Calendar.getInstance()
        cal.set(year, month, dayOfMonth)

        val date = cal.time
        val dfTimestamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dateString = dfTimestamp.format(date) + LAST_MINUTE
        val timestamp = calculateTimestamp(dateString)

        viewModel.exportWithTimestamp(timestamp)
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized) {
            return
        }

        val withElevation = binding.scrollViewGetLink.canScrollVertically(SCROLLING_UP_DIRECTION)
        viewModel.setElevation(withElevation)
    }

    private fun showSharingSensitiveItemsWarningDialog() {
        val context = context ?: return
        MaterialAlertDialogBuilder(context)
            .setTitle(getString(sharedR.string.hidden_item))
            .setMessage(getString(sharedR.string.share_hidden_item_link_description))
            .setCancelable(false)
            .setPositiveButton(R.string.button_continue) { _, _ ->
                initNode()

                setupView()
                setupObservers()
            }
            .setNegativeButton(R.string.general_cancel) { _, _ -> activity?.finish() }
            .show()
    }

    /**
     * Track when expiry date to node is set
     */
    private fun onSetExpiryDateClicked() {
    }
}