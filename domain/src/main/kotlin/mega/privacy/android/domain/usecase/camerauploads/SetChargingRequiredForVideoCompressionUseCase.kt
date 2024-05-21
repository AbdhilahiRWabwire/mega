package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case that sets whether compressing videos require the device to be charged or not
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 */
class SetChargingRequiredForVideoCompressionUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invocation function
     *
     * @param chargingRequired Whether the device needs to be charged or not
     */
    suspend operator fun invoke(chargingRequired: Boolean) =
        cameraUploadsRepository.setChargingRequiredForVideoCompression(chargingRequired)
}