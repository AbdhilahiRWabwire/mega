package mega.privacy.android.app.presentation.videosection.mapper

import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.domain.entity.node.TypedVideoNode
import java.io.File
import javax.inject.Inject

/**
 * The mapper class to convert the TypedVideoNode to VideoUIEntity
 */
class VideoUIEntityMapper @Inject constructor(
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
) {
    /**
     * Convert to VideoNode to VideoUIEntity
     */
    operator fun invoke(
        typedVideoNode: TypedVideoNode,
    ) = VideoUIEntity(
        id = typedVideoNode.id,
        name = typedVideoNode.name,
        size = typedVideoNode.size,
        durationString = durationInSecondsTextMapper(typedVideoNode.duration),
        durationInMinutes = typedVideoNode.duration.inWholeMinutes,
        thumbnail = typedVideoNode.thumbnailPath?.let { File(it) },
        isFavourite = typedVideoNode.isFavourite,
        nodeAvailableOffline = typedVideoNode.isAvailableOffline
    )
}
