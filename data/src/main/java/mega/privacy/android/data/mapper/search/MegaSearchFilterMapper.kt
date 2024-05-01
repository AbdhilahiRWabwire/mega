package mega.privacy.android.data.mapper.search

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.NodeType
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchTarget
import nz.mega.sdk.MegaSearchFilter
import javax.inject.Inject

/**
 * Mapper create MegaSearchFilter
 * @param searchCategoryIntMapper [SearchCategoryIntMapper]
 * @param dateFilterOptionLongMapper [DateFilterOptionLongMapper]
 */
class MegaSearchFilterMapper @Inject constructor(
    private val searchCategoryIntMapper: SearchCategoryIntMapper,
    private val dateFilterOptionLongMapper: DateFilterOptionLongMapper,
    private val searchTargetIntMapper: SearchTargetIntMapper,
    private val megaNodeTypeMapper: MegaNodeTypeMapper,
) {

    /**
     * invoke
     * @param searchQuery [String]
     * @param parentHandle [NodeId]
     * @param searchCategory [SearchCategory]
     * @param modificationDate [DateFilterOption]
     * @param creationDate [DateFilterOption]
     */
    operator fun invoke(
        searchQuery: String,
        parentHandle: NodeId,
        searchTarget: SearchTarget = SearchTarget.ROOT_NODES,
        searchCategory: SearchCategory? = SearchCategory.ALL,
        modificationDate: DateFilterOption? = null,
        creationDate: DateFilterOption? = null,
    ): MegaSearchFilter = MegaSearchFilter.createInstance().apply {

        // Set the search query
        if (searchQuery.isNotEmpty()) {
            byName(searchQuery)
        }

        // Set the parent node to search, if parentHandle is null, search in all nodes
        if (parentHandle.longValue == -1L) {
            byLocation(searchTargetIntMapper(searchTarget))
        } else {
            byLocationHandle(parentHandle.longValue)
        }

        // Set the search category
        searchCategory?.let {
            if (it == SearchCategory.FOLDER) {
                byNodeType(megaNodeTypeMapper(NodeType.FOLDER))
                byCategory(searchCategoryIntMapper(SearchCategory.ALL))
            } else {
                byCategory(searchCategoryIntMapper(it))
            }
        }

        // Set the modification and creation date
        modificationDate?.let {
            dateFilterOptionLongMapper(modificationDate).apply {
                byModificationTime(first, second)
            }
        }

        // Set the creation date
        creationDate?.let {
            dateFilterOptionLongMapper(creationDate).apply {
                byCreationTime(first, second)
            }
        }
    }
}