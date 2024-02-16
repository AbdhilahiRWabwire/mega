package mega.privacy.android.feature.sync.presentation

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.file.GetExternalPathByContentUriUseCase
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.usecase.sync.SyncFolderPairUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSelectedMegaFolderUseCase
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderAction
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderState
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncNewFolderViewModelTest {

    private val getExternalPathByContentUriUseCase: GetExternalPathByContentUriUseCase = mock()
    private val monitorSelectedMegaFolderUseCase: MonitorSelectedMegaFolderUseCase = mock()
    private val syncFolderPairUseCase: SyncFolderPairUseCase = mock()
    private lateinit var underTest: SyncNewFolderViewModel

    @AfterEach
    fun resetAndTearDown() {
        reset(getExternalPathByContentUriUseCase, monitorSelectedMegaFolderUseCase)
    }

    @Test
    fun `test that local folder selected action results in updated state`() = runTest {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flowOf(mock()))
        initViewModel()
        val localFolderContentUri =
            "content://com.android.externalstorage.documents/tree/primary%3ASync%2FsomeFolder"
        val localFolderUri: Uri = mock()
        val localFolderFolderStoragePath = "/storage/emulated/0/Sync/someFolder"
        val expectedState = SyncNewFolderState(selectedLocalFolder = localFolderFolderStoragePath)
        whenever(getExternalPathByContentUriUseCase.invoke(localFolderContentUri)).thenReturn(
            localFolderFolderStoragePath
        )
        whenever(localFolderUri.toString()).thenReturn(localFolderContentUri)

        underTest.handleAction(SyncNewFolderAction.LocalFolderSelected(localFolderUri))

        assertThat(expectedState.selectedLocalFolder).isEqualTo(underTest.state.value.selectedLocalFolder)
    }

    @Test
    fun `test that folder name changed action results in updated state`() {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flowOf(mock()))
        initViewModel()
        val folderPairName = "folderPairName"
        val expectedState = SyncNewFolderState(folderPairName = folderPairName)

        underTest.handleAction(SyncNewFolderAction.FolderNameChanged(folderPairName))

        assertThat(expectedState.folderPairName).isEqualTo(underTest.state.value.folderPairName)
    }

    @Test
    fun `test that when mega folder is updated state is also updated`() = runTest {
        val remoteFolder = RemoteFolder(123L, "someFolder")
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(
            flow {
                emit(remoteFolder)
                awaitCancellation()
            }
        )
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(
            flow {
                emit(remoteFolder)
                awaitCancellation()
            }
        )
        initViewModel()
        val expectedState = SyncNewFolderState(selectedMegaFolder = remoteFolder)

        assertThat(expectedState).isEqualTo(underTest.state.value)
    }

    @Test
    fun `test that next button create new folder pair`() = runTest {
        val remoteFolder = RemoteFolder(123L, "someFolder")
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(
            flow {
                emit(remoteFolder)
                awaitCancellation()
            }
        )
        val state = SyncNewFolderState(
            selectedMegaFolder = remoteFolder
        )
        initViewModel()

        underTest.handleAction(SyncNewFolderAction.NextClicked)

        verify(syncFolderPairUseCase).invoke(
            name = "",
            localPath = state.selectedLocalFolder,
            remotePath = state.selectedMegaFolder!!
        )
    }

    private fun initViewModel() {
        underTest = SyncNewFolderViewModel(
            getExternalPathByContentUriUseCase,
            monitorSelectedMegaFolderUseCase,
            syncFolderPairUseCase
        )
    }
}
