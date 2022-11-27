package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.model.node.OfflineInformation
import mega.privacy.android.domain.entity.offline.InboxOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.IncomingShareOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import org.junit.Test

class OfflineNodeInformationMapperTest {

    private val expectedPath = "path"
    private val expectedIncomingHandle = "incomingHandle"

    @Test
    fun `test that incoming offline node contains path and incoming handle`() = runTest{
        val origin = OfflineInformation.INCOMING
        val input = getInput(origin)

        assertThat(toOfflineNodeInformation(input)).isEqualTo(
            IncomingShareOfflineNodeInformation(
                path = expectedPath,
                incomingHandle = expectedIncomingHandle,
            )
        )
    }

    @Test
    fun `test that inbox offline node is the correct type and contains path`() = runTest{
        val origin = OfflineInformation.INBOX
        val input = getInput(origin)

        assertThat(toOfflineNodeInformation(input)).isEqualTo(
            InboxOfflineNodeInformation(path = expectedPath)
        )
    }


    @Test
    fun `test that a node with source OTHER returns the correct information`() {
        val origin = OfflineInformation.OTHER
        val input = getInput(origin)

        assertThat(toOfflineNodeInformation(input)).isEqualTo(
            OtherOfflineNodeInformation(expectedPath)
        )
    }

    private fun getInput(origin: Int) = OfflineInformation(
        id = 1,
        handle = "handle",
        path = expectedPath,
        name = "name",
        parentId = 1,
        type = null,
        origin = origin,
        handleIncoming = expectedIncomingHandle
    )
}