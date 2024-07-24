package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import javax.inject.Inject

/**
 * Check general nodes name collision use case (Cloud driver node)
 */
class CheckNodesNameCollisionUseCase @Inject constructor(
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
    private val getChildNodeUseCase: GetChildNodeUseCase,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     *
     * @param nodes
     * @param type
     */
    suspend operator fun invoke(
        nodes: Map<Long, Long>,
        type: NodeNameCollisionType,
    ): NodeNameCollisionResult {
        val noConflictNodes = hashMapOf<Long, Long>()
        val conflictNodes = hashMapOf<Long, NodeNameCollision>()
        nodes.forEach { entry ->
            val (nodeHandle, parentNodeHandle) = entry
            val parent = getParentOrRootNode(parentNodeHandle)
            if (parent == null || parent is FileNode) {
                noConflictNodes[nodeHandle] = parentNodeHandle
            } else {
                val currentNode = getNodeByHandleUseCase(
                    handle = nodeHandle,
                    attemptFromFolderApi = true
                ) ?: throw NodeDoesNotExistsException()
                getChildNodeUseCase(
                    parentNodeId = NodeId(parentNodeHandle),
                    name = currentNode.name
                )?.let { conflictNode ->
                    conflictNodes[nodeHandle] = createNodeNameCollision(
                        currentNode = currentNode,
                        parent = parent,
                        conflictNode = conflictNode
                    )
                } ?: run {
                    noConflictNodes[nodeHandle] = parentNodeHandle
                }
            }
        }

        return NodeNameCollisionResult(noConflictNodes, conflictNodes, type)
    }

    private fun createNodeNameCollision(
        currentNode: UnTypedNode,
        parent: Node,
        conflictNode: UnTypedNode,
    ) = NodeNameCollision.Default(
        collisionHandle = conflictNode.id.longValue,
        nodeHandle = currentNode.id.longValue,
        parentHandle = parent.id.longValue,
        name = currentNode.name,
        size = (currentNode as? FileNode)?.size ?: 0,
        childFolderCount = (parent as? FolderNode)?.childFolderCount ?: 0,
        childFileCount = (parent as? FolderNode)?.childFileCount ?: 0,
        lastModified = if (currentNode is FileNode) currentNode.modificationTime else currentNode.creationTime,
        isFile = currentNode is FileNode
    )

    private suspend fun getParentOrRootNode(parentHandle: Long) =
        if (parentHandle == nodeRepository.getInvalidHandle()) {
            getRootNodeUseCase()
        } else {
            getNodeByHandleUseCase(handle = parentHandle)
        }?.takeUnless { isNodeInRubbishBinUseCase(NodeId(parentHandle)) }
}
