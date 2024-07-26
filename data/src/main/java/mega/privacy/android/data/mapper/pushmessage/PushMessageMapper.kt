package mega.privacy.android.data.mapper.pushmessage

import androidx.work.Data
import mega.privacy.android.data.extensions.decodeBase64
import mega.privacy.android.domain.entity.pushes.PushMessage
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava
import javax.inject.Inject

/**
 * Mapper to convert [Data] to [PushMessage].
 */
class PushMessageMapper @Inject constructor() {

    /**
     * Invoke.
     *
     * @param data [Data]
     */
    operator fun invoke(data: Data): PushMessage? =
        when (data.getString(KEY_TYPE)) {
            PUSH_TYPE_CALL -> PushMessage.CallPushMessage
            PUSH_TYPE_CHAT -> {
                PushMessage.ChatPushMessage(
                    shouldBeep = data.getString(KEY_CHAT_SILENT) != VALUE_NO_BEEP,
                    chatId = data.getString(KEY_CHAT_ROOM_HANDLE)
                        ?.base64ToUserHandle() ?: MegaChatApiJava.MEGACHAT_INVALID_HANDLE,
                    msgId = data.getString(KEY_MSG_HANDLE)
                        ?.base64ToUserHandle() ?: MegaChatApiJava.MEGACHAT_INVALID_HANDLE,
                )
            }

            PUSH_TYPE_SCHED_MEETING -> {
                PushMessage.ScheduledMeetingPushMessage(
                    schedId = data.getString(KEY_SCHED_MEETING_HANDLE)
                        ?.base64ToUserHandle() ?: MegaChatApiJava.MEGACHAT_INVALID_HANDLE,
                    userHandle = data.getString(KEY_SCHED_MEETING_USER_HANDLE)
                        ?.base64ToUserHandle() ?: MegaApiJava.INVALID_HANDLE,
                    chatRoomHandle = data.getString(KEY_CHAT_ROOM_HANDLE)
                        ?.base64ToUserHandle() ?: MegaChatApiJava.MEGACHAT_INVALID_HANDLE,
                    title = data.getString(KEY_SCHED_MEETING_TITLE)?.decodeBase64(),
                    description = data.getString(KEY_SCHED_MEETING_DESCRIPTION)?.decodeBase64(),
                    startTimestamp = data.getString(KEY_SCHED_MEETING_START_TIME)
                        ?.toLongOrNull() ?: -1,
                    endTimestamp = data.getString(KEY_SCHED_MEETING_END_TIME)
                        ?.toLongOrNull() ?: -1,
                    timezone = data.getString(KEY_SCHED_MEETING_TIMEZONE)?.decodeBase64(),
                    isStartReminder = data.getString(KEY_SCHED_MEETING_START_REMINDER) == VALUE_START_REMINDER,
                )
            }

            PUSH_TYPE_PROMO -> {
                PushMessage.PromoPushMessage(
                    id = data.getString(KEY_PUSH_PARAM_ID)?.toIntOrNull()
                        ?: MegaChatApiJava.MEGACHAT_INVALID_HANDLE.toInt(),
                    title = data.getString(KEY_PUSH_PARAM_TITLE) ?: "",
                    subtitle = data.getString(KEY_PUSH_PARAM_SUBTITLE),
                    description = data.getString(KEY_PUSH_PARAM_DESCRIPTION) ?: "",
                    redirectLink = data.getString(KEY_PUSH_PARAM_TARGET_URL) ?: "",
                    imagePath = data.getString(KEY_PUSH_PARAM_IMAGE_PATH),
                    sound = data.getString(KEY_PUSH_PARAM_SOUND),
                )
            }

            else -> null
        }

    private fun String.base64ToUserHandle(): Long =
        MegaApiJava.base64ToUserHandle(this)

    companion object {
        const val PUSH_TYPE_SHARE_FOLDER = "1"
        const val PUSH_TYPE_CHAT = "2"
        const val PUSH_TYPE_CONTACT_REQUEST = "3"
        const val PUSH_TYPE_CALL = "4"
        const val PUSH_TYPE_ACCEPTANCE = "5"
        const val PUSH_TYPE_SCHED_MEETING = "7"
        const val PUSH_TYPE_PROMO = "8"

        private const val KEY_CHAT_ROOM_HANDLE = "chatid"
        private const val KEY_MSG_HANDLE = "msgid"

        private const val KEY_TYPE = "type"
        private const val KEY_CHAT_SILENT = "silent"
        private const val KEY_SCHED_MEETING_HANDLE = "id"
        private const val KEY_SCHED_MEETING_USER_HANDLE = "u"
        private const val KEY_SCHED_MEETING_TITLE = "t"
        private const val KEY_SCHED_MEETING_DESCRIPTION = "d"
        private const val KEY_SCHED_MEETING_TIMEZONE = "tz"
        private const val KEY_SCHED_MEETING_START_TIME = "s"
        private const val KEY_SCHED_MEETING_END_TIME = "e"
        private const val KEY_SCHED_MEETING_START_REMINDER = "f"
        private const val KEY_PUSH_PARAM_TARGET_URL = "generic_href"
        private const val KEY_PUSH_PARAM_SUBTITLE = "generic_subtitle"
        private const val KEY_PUSH_PARAM_DESCRIPTION = "generic_desc"
        private const val KEY_PUSH_PARAM_TITLE = "generic_title"
        private const val KEY_PUSH_PARAM_ID = "generic_id"
        private const val KEY_PUSH_PARAM_IMAGE_PATH = "generic_img"
        private const val KEY_PUSH_PARAM_SOUND = "sound"

        private const val VALUE_NO_BEEP = "0"
        private const val VALUE_START_REMINDER = "0"
    }
}
