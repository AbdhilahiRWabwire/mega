package mega.privacy.android.app.nav

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.activities.ManageChatHistoryActivity
import mega.privacy.android.app.activities.settingsActivities.LegacyCameraUploadsPreferencesActivity
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.presentation.meeting.chat.ChatHostActivity
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_LINK
import mega.privacy.android.app.presentation.meeting.managechathistory.view.screen.ManageChatHistoryActivityV2
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsComposeActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.navigation.MegaNavigator
import javax.inject.Inject

/**
 * Mega navigator impl
 * Centralized navigation logic instead of call navigator separately
 * We will replace with navigation component in the future
 */
internal class MegaNavigatorImpl @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : MegaNavigator,
    AppNavigatorImpl {
    override fun openSettingsCameraUploads(activity: Activity) {
        applicationScope.launch {
            val settingsCameraUploadsClass =
                if (getFeatureFlagValueUseCase(AppFeatures.SettingsCameraUploadsCompose)) {
                    SettingsCameraUploadsComposeActivity::class.java
                } else {
                    LegacyCameraUploadsPreferencesActivity::class.java
                }
            activity.startActivity(Intent(activity, settingsCameraUploadsClass))
        }
    }

    override fun openChat(
        context: Context,
        chatId: Long,
        action: String?,
        link: String?,
        text: String?,
        messageId: Long?,
        isOverQuota: Int?,
        flags: Int,
    ) {
        applicationScope.launch {
            val intent = if (getFeatureFlagValueUseCase(AppFeatures.NewChatActivity)) {
                getChatActivityIntent(
                    context = context,
                    action = action,
                    link = link,
                    text = text,
                    chatId = chatId,
                    messageId = messageId,
                    isOverQuota = isOverQuota,
                    flags = flags
                )
            } else {
                getLegacyChatIntent(
                    context = context,
                    action = action,
                    link = link,
                    text = text,
                    chatId = chatId,
                    messageId = messageId,
                    isOverQuota = isOverQuota,
                    flags = flags
                )
            }
            context.startActivity(intent)
        }
    }

    private fun getChatActivityIntent(
        context: Context,
        action: String?,
        link: String?,
        text: String?,
        chatId: Long,
        messageId: Long?,
        isOverQuota: Int?,
        flags: Int,
    ): Intent {
        val intent = Intent(context, ChatHostActivity::class.java).apply {
            this.action = action
            putExtra(EXTRA_ACTION, action)
            text?.let { putExtra(Constants.SHOW_SNACKBAR, text) }
            putExtra(Constants.CHAT_ID, chatId)
            messageId?.let { putExtra("ID_MSG", messageId) }
            isOverQuota?.let { putExtra("IS_OVERQUOTA", isOverQuota) }
            if (flags > 0) setFlags(flags)
        }
        link?.let {
            intent.putExtra(EXTRA_LINK, it)
        }
        return intent
    }

    private fun getLegacyChatIntent(
        context: Context,
        action: String?,
        link: String?,
        text: String?,
        chatId: Long?,
        messageId: Long?,
        isOverQuota: Int?,
        flags: Int,
    ) = Intent(context, ChatActivity::class.java).apply {
        this.action = action
        putExtra(EXTRA_ACTION, action)
        link?.let {
            this.data = Uri.parse(it)
        }
        text?.let { putExtra(Constants.SHOW_SNACKBAR, text) }
        chatId?.let { putExtra(Constants.CHAT_ID, chatId) }
        messageId?.let { putExtra("ID_MSG", messageId) }
        isOverQuota?.let { putExtra("IS_OVERQUOTA", isOverQuota) }
        if (flags > 0) setFlags(flags)
    }

    override fun openManageChatHistoryActivity(
        context: Context,
        chatId: Long,
        email: String?,
    ) {
        applicationScope.launch {
            val activity =
                if (getFeatureFlagValueUseCase(AppFeatures.NewManageChatHistoryActivity)) {
                    ManageChatHistoryActivityV2::class.java
                } else {
                    ManageChatHistoryActivity::class.java
                }
            val intent = Intent(context, activity).apply {
                putExtra(Constants.CHAT_ID, chatId)
                email?.let { putExtra(Constants.EMAIL, it) }
            }
            context.startActivity(intent)
        }
    }
}
