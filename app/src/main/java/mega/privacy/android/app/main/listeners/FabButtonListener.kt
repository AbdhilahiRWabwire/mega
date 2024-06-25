package mega.privacy.android.app.main.listeners

import android.content.Context
import android.view.View
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ContactFileListActivity
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.modalbottomsheet.UploadBottomSheetDialogFragment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.mobile.analytics.event.CloudDriveFABPressedEvent
import timber.log.Timber

internal class FabButtonListener(val context: Context) : View.OnClickListener {

    override fun onClick(v: View) {
        Timber.d("FabButtonListener")
        when (v.id) {
            R.id.floating_button -> {
                Timber.d("Floating Button click!")
                if (context is ManagerActivity) {
                    when (context.drawerItem) {
                        DrawerItem.CLOUD_DRIVE -> {
                            Timber.d("Cloud Drive SECTION")
                            if (!Util.isOnline(context)) {
                                context.showSnackbar(
                                    Constants.SNACKBAR_TYPE, context.getString(
                                        R.string.error_server_connection_problem
                                    ), -1
                                )
                                return
                            }
                            context.showUploadPanel(
                                UploadBottomSheetDialogFragment.CLOUD_DRIVE_UPLOAD
                            )
                            Analytics.tracker.trackEvent(CloudDriveFABPressedEvent)
                        }

                        DrawerItem.SHARED_ITEMS -> {
                            Timber.d("Cloud Drive SECTION")
                            if (!Util.isOnline(context)) {
                                context.showSnackbar(
                                    Constants.SNACKBAR_TYPE, context.getString(
                                        R.string.error_server_connection_problem
                                    ), -1
                                )
                                return
                            }
                            context.showUploadPanel()
                        }

                        else -> {}
                    }
                }
            }

            R.id.floating_button_contact_file_list -> {
                if (!Util.isOnline(context)) {
                    if (context is ContactFileListActivity) {
                        context.showSnackbar(
                            Constants.SNACKBAR_TYPE, context.getString(
                                R.string.error_server_connection_problem
                            )
                        )
                    }
                    return
                }
                if (context is ContactFileListActivity) {
                    context.showUploadPanel()
                }
            }
        }
    }
}