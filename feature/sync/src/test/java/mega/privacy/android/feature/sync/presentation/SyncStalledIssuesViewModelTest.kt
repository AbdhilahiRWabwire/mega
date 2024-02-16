package mega.privacy.android.feature.sync.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.ui.mapper.stalledissue.StalledIssueItemMapper
import mega.privacy.android.feature.sync.ui.model.StalledIssueDetailedInfo
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesState
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncStalledIssuesViewModelTest {

    private val monitorStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase = mock()
    private val stalledIssueItemMapper: StalledIssueItemMapper = mock()
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()

    private lateinit var underTest: SyncStalledIssuesViewModel

    private val stalledIssues = listOf(
        StalledIssue(
            nodeIds = listOf(NodeId(3L)),
            localPaths = listOf("/storage/emulated/0/DCIM"),
            issueType = StallIssueType.DownloadIssue,
            conflictName = "conflicting folder",
            nodeNames = listOf("Camera"),
        )
    )

    private val stalledIssuesUiItems = listOf(
        StalledIssueUiItem(
            nodeIds = listOf(NodeId(3L)),
            localPaths = listOf("/storage/emulated/0/DCIM"),
            issueType = StallIssueType.DownloadIssue,
            conflictName = "conflicting folder",
            nodeNames = listOf("Camera"),
            icon = 0,
            detailedInfo = StalledIssueDetailedInfo("", ""),
            actions = emptyList()
        )
    )

    @AfterEach
    fun resetAndTearDown() {
        reset(
            monitorStalledIssuesUseCase,
            stalledIssueItemMapper,
            getNodeByHandleUseCase,
        )
    }

    @Test
    fun `test that view model fetches all stalled issues and updates the state on init`() =
        runTest {
            whenever(monitorStalledIssuesUseCase()).thenReturn(flow {
                emit(stalledIssues)
                awaitCancellation()
            })
            val node: FolderNode = mock {
                on { name } doReturn "Camera"
                on { isIncomingShare } doReturn true
            }
            whenever(
                stalledIssueItemMapper(
                    stalledIssues.first(),
                    listOf(node),
                )
            ).thenReturn(
                stalledIssuesUiItems.first()
            )
            whenever(getNodeByHandleUseCase(stalledIssues.first().nodeIds.first().longValue))
                .thenReturn(node)
            initViewModel()

            underTest.state.test {
                assertThat(awaitItem())
                    .isEqualTo(SyncStalledIssuesState(stalledIssuesUiItems))
            }
        }

    private fun initViewModel() {
        underTest = SyncStalledIssuesViewModel(
            monitorStalledIssuesUseCase,
            stalledIssueItemMapper,
            getNodeByHandleUseCase
        )
    }
}