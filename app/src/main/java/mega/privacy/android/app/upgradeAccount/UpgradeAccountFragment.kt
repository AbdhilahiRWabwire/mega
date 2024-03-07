package mega.privacy.android.app.upgradeAccount

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.presentation.billing.BillingViewModel
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.app.upgradeAccount.model.UpgradePayment
import mega.privacy.android.app.upgradeAccount.payment.PaymentActivity
import mega.privacy.android.app.upgradeAccount.view.UpgradeAccountView
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.billing.PaymentUtils
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.shared.theme.MegaAppTheme
import nz.mega.sdk.MegaApiAndroid
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class UpgradeAccountFragment : Fragment() {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var myAccountInfo: MyAccountInfo

    private val upgradeAccountViewModel by activityViewModels<UpgradeAccountViewModel>()

    private val billingViewModel by activityViewModels<BillingViewModel>()

    internal lateinit var upgradeAccountActivity: UpgradeAccountActivity


    @Inject
    lateinit var getFeatureFlagUseCase: GetFeatureFlagValueUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        upgradeAccountActivity = activity as UpgradeAccountActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent { UpgradeAccountBody() }
        setupObservers()
        viewLifecycleOwner.collectFlow(billingViewModel.billingUpdateEvent) {
            if (it is BillingEvent.OnPurchaseUpdate) {
                onPurchasesUpdated(it.purchases)
                billingViewModel.markHandleBillingEvent()
            }
        }
    }


    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun UpgradeAccountBody() {
        val uiState by upgradeAccountViewModel.state.collectAsStateWithLifecycle()
        val mode by getThemeMode()
            .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        MegaAppTheme(isDark = mode.isDarkMode()) {
            UpgradeAccountView(
                modifier = Modifier.semantics {
                    testTagsAsResourceId = true
                },
                state = uiState,
                onBackPressed = upgradeAccountActivity.onBackPressedDispatcher::onBackPressed,
                onBuyClicked = {
                    val chosenPlan = convertAccountTypeToInt(uiState.chosenPlan)
                    upgradeAccountViewModel.currentPaymentCheck(chosenPlan)
                    if (uiState.currentPayment == UpgradePayment()) {
                        startPurchase(uiState.isMonthlySelected, chosenPlan)
                    }
                },
                onPlayStoreLinkClicked = this::redirectToPlayStoreSubscription,
                onPricingPageClicked = this::redirectToPricingPage,
                onChoosingMonthlyYearlyPlan = upgradeAccountViewModel::onSelectingMonthlyPlan,
                onChoosingPlanType = {
                    with(upgradeAccountViewModel) {
                        if (isBillingAvailable()) {
                            onSelectingPlanType(it)
                        } else {
                            Timber.w("Billing not available")
                            setBillingWarningVisibility(true)
                        }
                    }
                },
                showBillingWarning = { upgradeAccountViewModel.setBillingWarningVisibility(true) },
                hideBillingWarning = { upgradeAccountViewModel.setBillingWarningVisibility(false) },
                onDialogConfirmButtonClicked = {
                    upgradeAccountViewModel.setShowBuyNewSubscriptionDialog(
                        showBuyNewSubscriptionDialog = false
                    )
                    startPurchase(
                        uiState.isMonthlySelected,
                        convertAccountTypeToInt(uiState.chosenPlan)
                    )
                },
                onDialogDismissButtonClicked = {
                    upgradeAccountViewModel.setShowBuyNewSubscriptionDialog(
                        showBuyNewSubscriptionDialog = false
                    )
                },
                showUpgradeWarningBanner = uiState.isCrossAccountMatch.not()
            )
        }
    }

    private fun setupObservers() {
        upgradeAccountViewModel.onUpgradeClick().observe(viewLifecycleOwner) { upgradeType ->
            startActivity(
                Intent(context, PaymentActivity::class.java)
                    .putExtra(PaymentActivity.UPGRADE_TYPE, upgradeType)
            )
        }
    }

    /**
     * Shows the selected payment plan.
     *
     * @param accountType Selected payment plan.
     */
    private fun onPlanClicked(accountType: AccountType) {
        with(upgradeAccountViewModel) {
            if (!isBillingAvailable()) {
                Timber.w("Billing not available")
                setBillingWarningVisibility(true)
                return
            }
            val upgradeType = convertAccountTypeToInt(accountType)
            currentPaymentCheck(upgradeType)
        }
    }

    private fun onCustomLabelClick(currentSubscriptionPlan: AccountType) {
        val accountTypeInt = convertAccountTypeToInt(currentSubscriptionPlan)
        AlertsAndWarnings.askForCustomizedPlan(
            requireContext(),
            megaApi.myEmail,
            accountTypeInt
        )
    }

    private fun startPurchase(
        isMonthlySelected: Boolean,
        chosenPlan: Int,
    ) {
        billingViewModel.startPurchase(
            upgradeAccountActivity,
            upgradeAccountViewModel.getProductId(
                isMonthlySelected,
                chosenPlan
            )
        )
    }

    private fun onDialogPositiveButtonClicked(upgradeType: Int) {
        upgradeAccountViewModel.setShowBuyNewSubscriptionDialog(
            showBuyNewSubscriptionDialog = false
        )
        startActivity(
            Intent(context, PaymentActivity::class.java)
                .putExtra(PaymentActivity.UPGRADE_TYPE, upgradeType)
        )
    }

    private fun convertAccountTypeToInt(accountType: AccountType): Int {
        return when (accountType) {
            AccountType.PRO_LITE -> Constants.PRO_LITE
            AccountType.PRO_I -> Constants.PRO_I
            AccountType.PRO_II -> Constants.PRO_II
            AccountType.PRO_III -> Constants.PRO_III
            else -> Constants.INVALID_VALUE
        }
    }

    private fun redirectToPlayStoreSubscription(link: String) {
        val uriUrl = Uri.parse(link)
        val launchBrowser = Intent(ACTION_VIEW, uriUrl)
        runCatching {
            startActivity(launchBrowser)
        }.onFailure {
            Timber.e("Failed to open play store subscription page with error: ${it.message}")
        }
    }

    private fun redirectToPricingPage(link: String) {
        val uriUrl = Uri.parse(link)
        val launchBrowser = Intent(requireContext(), WebViewActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .setData(uriUrl)
        startActivity(launchBrowser)
    }

    private fun onPurchasesUpdated(
        purchases: List<MegaPurchase>,
    ) {
        if (purchases.isNotEmpty()) {
            val purchase = purchases.first()
            //payment may take time to process, we will not give privilege until it has been fully processed
            val sku = purchase.sku
            if (billingViewModel.isPurchased(purchase)) {
                //payment has been processed
                Timber.d(
                    "Purchase $sku successfully, subscription type is: "
                            + PaymentUtils.getSubscriptionType(
                        sku,
                        upgradeAccountActivity
                    ) + ", subscription renewal type is: "
                            + PaymentUtils.getSubscriptionRenewalType(sku, upgradeAccountActivity)
                )
                RatingHandlerImpl(upgradeAccountActivity).updateTransactionFlag(true)
            } else {
                //payment is being processed or in unknown state
                Timber.d("Purchase %s is being processed or in unknown state.", sku)
            }
        } else {
            //down grade case
            Timber.d("Downgrade, the new subscription takes effect when the old one expires.")
        }

        if (myAccountInfo.isUpgradeFromAccount()) {
            val intent = Intent(upgradeAccountActivity, MyAccountActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            upgradeAccountActivity.startActivity(intent)
        } else {
            upgradeAccountActivity.onBackPressedDispatcher.onBackPressed()
        }
    }

    companion object {
        const val PRIVACY_POLICY_URL = "https://mega.nz/privacy"
    }
}