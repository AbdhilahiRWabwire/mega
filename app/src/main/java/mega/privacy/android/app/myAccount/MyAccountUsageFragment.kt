package mega.privacy.android.app.myAccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.databinding.FragmentMyAccountUsageBinding
import mega.privacy.android.app.databinding.MyAccountPaymentInfoContainerBinding
import mega.privacy.android.app.databinding.MyAccountUsageContainerBinding
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.fragments.homepage.Scrollable
import mega.privacy.android.app.myAccount.util.MyAccountViewUtil.businessUpdate
import mega.privacy.android.app.myAccount.util.MyAccountViewUtil.update
import mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

@AndroidEntryPoint
class MyAccountUsageFragment : Fragment(), Scrollable {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    private val viewModel: MyAccountViewModel by viewModels({ requireParentFragment() })

    private lateinit var binding: FragmentMyAccountUsageBinding
    private lateinit var usageBinding: MyAccountUsageContainerBinding
    private lateinit var paymentAlertBinding: MyAccountPaymentInfoContainerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedElementEnterTransition = MaterialContainerTransform()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMyAccountUsageBinding.inflate(layoutInflater)
        usageBinding = binding.usageViewLayout
        paymentAlertBinding = binding.paymentAlert
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupView()
        setupObservers()
    }

    private fun setupView() {
        viewModel.getFileVersionsOption()

        ListenScrollChangesHelper().addViewToListen(
            binding.usageLayout
        ) { _, _, _, _, _ -> checkScroll() }

        usageBinding.root.background = ContextCompat.getDrawable(
            requireContext(),
            R.drawable.background_usage_storage_transfer
        )

        setupAccountDetails()
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized)
            return

        val withElevation = binding.usageLayout.canScrollVertically(SCROLLING_UP_DIRECTION)
        viewModel.setElevation(withElevation)
    }

    private fun setupObservers() {
        viewModel.getVersionsInfo().observe(viewLifecycleOwner, ::refreshVersionsInfo)
        viewModel.onUpdateAccountDetails().observe(viewLifecycleOwner) { setupAccountDetails() }
    }

    /**
     * Updates versions info depending on if is enabled or not.
     *
     * @param versionsInfo Text to show as versions info.
     */
    private fun refreshVersionsInfo(versionsInfo: String) {
        if (MegaApplication.isDisableFileVersions() == 0) {
            binding.rubbishSeparator.isVisible = true
            binding.previousVersionsStorageContainer.isVisible = true
            binding.previousVersionsText.text = versionsInfo
        } else {
            binding.rubbishSeparator.isVisible = false
            binding.previousVersionsStorageContainer.isVisible = false
        }
    }

    private fun setupAccountDetails() {
        if (megaApi.isBusinessAccount) {
            usageBinding.businessUpdate(viewModel)
            paymentAlertBinding.businessUpdate(megaApi)
            paymentAlertBinding.root.isVisible = true
            return
        }

        usageBinding.update(viewModel)
        paymentAlertBinding.root.isVisible = paymentAlertBinding.update(viewModel)

        binding.cloudStorageText.text = viewModel.getCloudStorage()

        val inboxStorage = viewModel.getInboxStorage()
        if (inboxStorage.isEmpty()) {
            binding.inboxStorageContainer.isVisible = false
        } else {
            binding.inboxStorageContainer.isVisible = true
            binding.inboxStorageText.text = inboxStorage
        }

        binding.incomingStorageText.text = viewModel.getIncomingStorage()
        binding.rubbishStorageText.text = viewModel.getRubbishStorage()
    }
}