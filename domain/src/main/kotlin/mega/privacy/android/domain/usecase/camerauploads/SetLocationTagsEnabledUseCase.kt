package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case that updates the value in the Database, as to whether Location Tags are added or not
 * when uploading Photos in Camera Uploads
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 */
class SetLocationTagsEnabledUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invocation function
     *
     * @param enable true if Location Tags should be added when uploading Photos, and false if otherwise
     */
    suspend operator fun invoke(enable: Boolean) =
        cameraUploadsRepository.setLocationTagsEnabled(enable)
}