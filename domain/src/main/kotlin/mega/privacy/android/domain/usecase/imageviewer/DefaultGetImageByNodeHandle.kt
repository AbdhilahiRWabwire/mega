package mega.privacy.android.domain.usecase.imageviewer

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.repository.ImageRepository
import mega.privacy.android.domain.repository.NetworkRepository

/**
 * Default Implementation of [GetImageByNodeHandle]
 */
class DefaultGetImageByNodeHandle(
    private val networkRepository: NetworkRepository,
    private val imageRepository: ImageRepository,
) : GetImageByNodeHandle {
    override suspend fun invoke(
        nodeHandle: Long,
        fullSize: Boolean,
        highPriority: Boolean,
    ): Flow<ImageResult> {
        return imageRepository.getImageByNodeHandle(
            nodeHandle,
            fullSize,
            highPriority,
            networkRepository.isMeteredConnection() ?: false
        )
    }
}