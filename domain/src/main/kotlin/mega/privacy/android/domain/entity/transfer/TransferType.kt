package mega.privacy.android.domain.entity.transfer

/**
 * Transfer type
 */
enum class TransferType {
    /**
     * None
     */
    NONE,

    /**
     * Type Download refer to MegaTransfer.TYPE_DOWNLOAD
     */
    DOWNLOAD,

    /**
     * Type Upload refer to MegaTransfer.TYPE_UPLOAD for general uploads (no camera upload nor chat upload)
     */
    GENERAL_UPLOAD,

    /**
     * Type Upload refer to MegaTransfer.TYPE_UPLOAD  for camera upload transfers
     */
    CU_UPLOAD,

    /**
     * Type Upload refer to MegaTransfer.TYPE_UPLOAD  for camera upload transfers
     */
    CHAT_UPLOAD;

    /**
     * @return true if transfer type is any of the upload types
     */
    fun isUploadType() = when (this) {
        GENERAL_UPLOAD, CU_UPLOAD, CHAT_UPLOAD -> true
        else -> false
    }

    /**
     * @return true if transfer type is download type
     */
    fun isDownloadType() = this == DOWNLOAD
}