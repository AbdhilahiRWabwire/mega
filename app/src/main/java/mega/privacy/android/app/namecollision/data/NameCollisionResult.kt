package mega.privacy.android.app.namecollision.data

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class containing all the required to present a name collision for an upload, copy or movement.
 *
 * @property nameCollision          [LegacyNameCollision]
 * @property collisionName          The node name with which there is a name collision.
 * @property collisionSize          The node size with which there is a name collision if is a file, null otherwise.
 * @property collisionFolderContent The folder content of the node with which there is a name collision if is a folder, null otherwise.
 * @property collisionLastModified  The node last modified date with which there is a name collision.
 * @property collisionThumbnail     The node thumbnail if exists.
 * @property renameName             Name of the item for the rename option. Null if if the item is a folder.
 * @property thumbnail              The thumbnail of the item to upload, copy or move if exists.
 * @property choice                 [NameCollisionChoice] with the collision resolution.
 */
@Parcelize
@Deprecated("Use NodeNameCollisionResult instead")
data class NameCollisionResult(
    val nameCollision: LegacyNameCollision,
    var collisionName: String? = null,
    var collisionSize: Long? = null,
    var collisionFolderContent: String? = null,
    var collisionLastModified: Long? = null,
    var collisionThumbnail: Uri? = null,
    var renameName: String? = null,
    var thumbnail: Uri? = null,
    var choice: NameCollisionChoice? = null,
) : Parcelable