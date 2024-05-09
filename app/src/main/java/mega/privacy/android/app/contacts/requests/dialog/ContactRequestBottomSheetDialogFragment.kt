package mega.privacy.android.app.contacts.requests.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.contacts.requests.ContactRequestsViewModel
import mega.privacy.android.app.databinding.BottomSheetContactRequestBinding
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.utils.setImageRequestFromUri
import mega.privacy.android.domain.entity.contacts.ContactRequestAction
import nz.mega.sdk.MegaApiJava

/**
 * Bottom Sheet Dialog that represents the UI for a dialog containing contact request information.
 */
@AndroidEntryPoint
class ContactRequestBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    companion object {
        private const val TAG = "ContactRequestBottomSheetDialogFragment"
        private const val REQUEST_HANDLE = "REQUEST_HANDLE"

        fun newInstance(requestHandle: Long): ContactRequestBottomSheetDialogFragment =
            ContactRequestBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(REQUEST_HANDLE, requestHandle)
                }
            }
    }

    private val viewModel by viewModels<ContactRequestsViewModel>({ requireParentFragment() })
    private val requestHandle by lazy {
        arguments?.getLong(
            REQUEST_HANDLE,
            MegaApiJava.INVALID_HANDLE
        ) ?: MegaApiJava.INVALID_HANDLE
    }

    private lateinit var binding: BottomSheetContactRequestBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = BottomSheetContactRequestBinding.inflate(inflater, container, false)
        contentView = binding.root
        itemsLayout = binding.itemsLayout
        binding.header.btnMore.isVisible = false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.getContactRequest(requestHandle).observe(viewLifecycleOwner) { item ->
            requireNotNull(item) { "Contact request not found" }

            binding.header.txtTitle.text = item.email
            binding.header.txtSubtitle.text = item.createdTime
            binding.header.imgThumbnail.hierarchy.setPlaceholderImage(item.placeholder)
            binding.header.imgThumbnail.setImageRequestFromUri(item.avatarUri)
            binding.groupReceived.isVisible = !item.isOutgoing
            binding.groupSent.isVisible = item.isOutgoing

            binding.btnAccept.setOnClickListener {
                viewModel.handleContactRequest(
                    requestHandle = requestHandle,
                    contactRequestAction = ContactRequestAction.Accept
                )
                dismiss()
            }
            binding.btnIgnore.setOnClickListener {
                viewModel.handleContactRequest(
                    requestHandle = requestHandle,
                    contactRequestAction = ContactRequestAction.Ignore
                )
                dismiss()
            }
            binding.btnDecline.setOnClickListener {
                viewModel.handleContactRequest(
                    requestHandle = requestHandle,
                    contactRequestAction = ContactRequestAction.Deny
                )
                dismiss()
            }
            binding.btnReinvite.setOnClickListener {
                viewModel.handleContactRequest(
                    requestHandle = requestHandle,
                    contactRequestAction = ContactRequestAction.Remind
                )
                dismiss()
            }
            binding.btnRemove.setOnClickListener {
                viewModel.handleContactRequest(
                    requestHandle = requestHandle,
                    contactRequestAction = ContactRequestAction.Delete
                )
                dismiss()
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    /**
     * Custom show method to avoid showing the same dialog multiple times
     */
    fun show(manager: FragmentManager) {
        if (manager.findFragmentByTag(TAG) == null) {
            super.show(manager, TAG)
        }
    }
}
