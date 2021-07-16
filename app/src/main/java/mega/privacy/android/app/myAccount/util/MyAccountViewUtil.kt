package mega.privacy.android.app.myAccount.util

import androidx.core.view.isVisible
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.MyAccountPaymentInfoContainerBinding
import mega.privacy.android.app.databinding.MyAccountUsageContainerBinding
import mega.privacy.android.app.myAccount.MyAccountViewModel
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.TimeUtils
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava

object MyAccountViewUtil {

    private val gettingInfo = getString(R.string.recovering_info)

    /**
     * Updates the views related to usage of storage and transfers
     * for all type of accounts except business ones.
     *
     * @param viewModel MyAccountViewModel to check the data.
     */
    fun MyAccountUsageContainerBinding.update(viewModel: MyAccountViewModel) {
        storageProgressBar.isVisible = true
        businessStorageImage.isVisible = false
        transferProgressBar.isVisible = true
        businessTransferImage.isVisible = false

        if (viewModel.getUsedStorage().isEmpty()) {
            storageProgressPercentage.isVisible = false
            storageProgressBar.progress = 0
            storageProgress.text = gettingInfo
        } else {
            storageProgressPercentage.apply {
                isVisible = true
                text = getString(
                    R.string.used_storage_transfer_percentage,
                    viewModel.getUsedStoragePercentage()
                )
            }

            storageProgressBar.progress =
                viewModel.getUsedStoragePercentage()
            storageProgress.text = getString(
                R.string.used_storage_transfer,
                viewModel.getUsedStorage(),
                viewModel.getTotalStorage()
            )
        }

        transferLayout.isVisible = !viewModel.isFreeAccount()

        if (viewModel.getUsedTransfer().isEmpty()) {
            transferProgressPercentage.isVisible = false
            transferProgressBar.progress = 0
            transferProgress.text = gettingInfo
        } else {
            transferProgressPercentage.apply {
                isVisible = true
                text = getString(
                    R.string.used_storage_transfer_percentage,
                    viewModel.getUsedTransferPercentage()
                )
            }

            transferProgressBar.progress =
                viewModel.getUsedTransferPercentage()
            transferProgress.text = getString(
                R.string.used_storage_transfer,
                viewModel.getUsedTransfer(),
                viewModel.getTotalTransfer()
            )
        }
    }

    /**
     * Updates the views related to usage of storage and transfers
     * for only business accounts.
     *
     * @param viewModel MyAccountViewModel to check the data.
     */
    fun MyAccountUsageContainerBinding.businessUpdate(viewModel: MyAccountViewModel) {
        storageProgressPercentage.isVisible = false
        storageProgressBar.isVisible = false
        businessStorageImage.isVisible = true

        storageProgress.text =
            if (viewModel.getUsedStorage().isEmpty()) gettingInfo
            else viewModel.getUsedStorage()

        transferProgressPercentage.isVisible = false
        transferProgressBar.isVisible = false
        businessTransferImage.isVisible = true

        transferProgress.text =
            if (viewModel.getUsedTransfer().isEmpty()) gettingInfo
            else viewModel.getUsedTransfer()
    }

    /**
     * Updates the views related to payments for all type of accounts except business ones.
     *
     * @param viewModel MyAccountViewModel to check the data.
     */
    fun MyAccountPaymentInfoContainerBinding.update(viewModel: MyAccountViewModel): Boolean {
        businessStatusText.isVisible = false
        val renewable = viewModel.hasRenewableSubscription()

        return if (renewable || viewModel.hasExpirableSubscription()) {
            setRenewOrExpiryDate(viewModel, renewable)
            true
        } else false
    }

    /**
     * Updates the views related to payments for only business accounts.
     *
     * @param megaApi MegaApiAndroid to check business status.
     * @param viewModel MyAccountViewModel to check the data.
     */
    fun MyAccountPaymentInfoContainerBinding.businessUpdate(
        megaApi: MegaApiAndroid,
        viewModel: MyAccountViewModel
    ) {
        val businessStatus = megaApi.businessStatus
        val renewable = viewModel.hasRenewableSubscription()
        val expirable = viewModel.hasExpirableSubscription()

        when {
            businessStatus == MegaApiJava.BUSINESS_STATUS_EXPIRED -> setBusinessAlert(true)
            businessStatus == MegaApiJava.BUSINESS_STATUS_GRACE_PERIOD -> setBusinessAlert(false)
            renewable || expirable -> setRenewOrExpiryDate(viewModel, renewable)
        }
    }

    /**
     * Updates the views related to payments for all type of accounts.
     *
     * @param viewModel MyAccountViewModel to check the data.
     * @param renewable True if the subscriptions is renewable, false otherwise.
     */
    private fun MyAccountPaymentInfoContainerBinding.setRenewOrExpiryDate(
        viewModel: MyAccountViewModel,
        renewable: Boolean
    ) {
        businessStatusText.isVisible = false

        renewExpiryText.apply {
            isVisible = true
            text = getString(if (renewable) R.string.renews_on else R.string.expires_on)
        }

        renewExpiryDateText.apply {
            isVisible = true
            text = TimeUtils.formatDate(
                if (renewable) viewModel.getRenewTime() else viewModel.getExpirationTime(),
                TimeUtils.DATE_MM_DD_YYYY_FORMAT
            )
        }
    }

    /**
     * Updates the business alert view by setting the expired or grace period message.
     *
     * @param expired True if the account is expired, false otherwise.
     */
    private fun MyAccountPaymentInfoContainerBinding.setBusinessAlert(expired: Boolean) {
        renewExpiryText.isVisible = false
        renewExpiryDateText.isVisible = false

        businessStatusText.apply {
            isVisible = true
            text = getString(
                if (expired) R.string.payment_overdue_label
                else R.string.payment_required_label
            )
        }
    }
}