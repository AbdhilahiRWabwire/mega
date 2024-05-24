package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import nz.mega.sdk.MegaNode
import javax.inject.Inject

class DefaultGetNodeListByIds @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GetNodeListByIds {

    override suspend fun invoke(ids: List<Long>): List<MegaNode> = withContext(ioDispatcher) {
        ids.mapNotNull {
            getNodeByHandle(it)
        }
    }
}