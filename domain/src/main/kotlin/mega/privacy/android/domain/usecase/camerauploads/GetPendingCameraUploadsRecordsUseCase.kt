package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import javax.inject.Inject

/**
 * Get the records to upload through Camera Uploads
 *
 * @param cameraUploadsRepository
 */
class GetPendingCameraUploadsRecordsUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
    private val getUploadOptionUseCase: GetUploadOptionUseCase,
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
) {

    /**
     * Get the records with upload status PENDING, STARTED and FAILED from the database
     *
     * @return the list of CameraUploadsRecord with status PENDING, STARTED and FAILED and matching the type
     */
    suspend operator fun invoke(): List<CameraUploadsRecord> {
        val uploadStatus = listOf(
            CameraUploadsRecordUploadStatus.PENDING,
            CameraUploadsRecordUploadStatus.STARTED,
            CameraUploadsRecordUploadStatus.FAILED,
        )

        val types = when (getUploadOptionUseCase()) {
            UploadOption.PHOTOS -> listOf(CameraUploadsRecordType.TYPE_PHOTO)
            UploadOption.VIDEOS -> listOf(CameraUploadsRecordType.TYPE_VIDEO)
            UploadOption.PHOTOS_AND_VIDEOS ->
                listOf(CameraUploadsRecordType.TYPE_PHOTO, CameraUploadsRecordType.TYPE_VIDEO)
        }

        val folderTypes =
            if (isSecondaryFolderEnabled())
                listOf(CameraUploadFolderType.Primary, CameraUploadFolderType.Secondary)
            else listOf(CameraUploadFolderType.Primary)

        return cameraUploadsRepository.getCameraUploadsRecordsBy(
            uploadStatus = uploadStatus,
            types = types,
            folderTypes = folderTypes,
        )
    }
}
