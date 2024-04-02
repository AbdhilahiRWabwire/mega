package mega.privacy.android.domain.usecase.transfers.uploads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.repository.CancelTokenRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.canceltoken.InvalidateCancelTokenUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleTransferEventUseCase
import mega.privacy.android.domain.usecase.transfers.sd.HandleSDCardEventUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.internal.verification.Times
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadFilesUseCaseTest {

    private val cancelCancelTokenUseCase = mock<CancelCancelTokenUseCase>()
    private val invalidateCancelTokenUseCase = mock<InvalidateCancelTokenUseCase>()
    private val handleTransferEventUseCase = mock<HandleTransferEventUseCase>()
    private val handleSDCardEventUseCase = mock<HandleSDCardEventUseCase>()
    private val monitorTransferEventsUseCase = mock<MonitorTransferEventsUseCase>()
    private val transferRepository = mock<TransferRepository>()
    private val cancelTokenRepository = mock<CancelTokenRepository>()
    private val fileNode = mock<TypedFileNode>()
    private val fileSystemRepository = mock<FileSystemRepository>()
    private val transfer = mock<Transfer>()

    private lateinit var underTest: UploadFilesUseCase

    @BeforeAll
    fun setup() {
        underTest =
            UploadFilesUseCase(
                cancelCancelTokenUseCase = cancelCancelTokenUseCase,
                invalidateCancelTokenUseCase = invalidateCancelTokenUseCase,
                handleTransferEventUseCase = handleTransferEventUseCase,
                handleSDCardEventUseCase = handleSDCardEventUseCase,
                monitorTransferEventsUseCase = monitorTransferEventsUseCase,
                transferRepository = transferRepository,
            )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            transferRepository, cancelTokenRepository, fileSystemRepository,
            handleTransferEventUseCase, fileNode, invalidateCancelTokenUseCase,
            cancelCancelTokenUseCase, transfer, handleSDCardEventUseCase,
            monitorTransferEventsUseCase,
        )
        commonStub()
    }

    private fun commonStub() {
        whenever(monitorTransferEventsUseCase()).thenReturn(emptyFlow())
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }


    @ParameterizedTest(name = "priority: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that repository start upload is called with the proper priority`(priority: Boolean) =
        runTest {
            underTest(mapOf(file to null), parentId, null, priority, false).test {

                verify(transferRepository).startUpload(
                    ABSOLUTE_PATH,
                    parentId,
                    null,
                    MODIFIED_TIME_SECS,
                    null,
                    false,
                    priority,
                )
                awaitComplete()
            }
        }

    @ParameterizedTest(name = "appdata: \"{0}\"")
    @MethodSource("provideAppDataExceptChat")
    fun `test that repository start upload is called with the proper appData when appData is not a chat upload`(
        appData: TransferAppData?,
    ) = runTest {
        underTest(
            mapOf(file to null), parentId, appData,
            isHighPriority = false,
            isSourceTemporary = false
        ).test {
            verify(transferRepository).startUpload(
                ABSOLUTE_PATH,
                parentId,
                null,
                MODIFIED_TIME_SECS,
                appData,
                isSourceTemporary = false,
                shouldStartFirst = false,
            )
            awaitComplete()
        }
    }

    @ParameterizedTest(name = "appdata: \"{0}\"")
    @MethodSource("provideChatAppData")
    fun `test that repository start upload for chat is called when appData is chat transfer app data`(
        appData: TransferAppData.ChatTransferAppData,
    ) = runTest {
        underTest(
            mapOf(file to null), parentId, appData,
            isSourceTemporary = false,
            isHighPriority = false
        ).test {
            verify(transferRepository).startUploadForChat(
                ABSOLUTE_PATH,
                parentId,
                null,
                appData,
                isSourceTemporary = false,
            )
            awaitComplete()
        }
    }

    private fun provideAppDataExceptChat() = listOf(
        TransferAppData.BackgroundTransfer,
        TransferAppData.SdCardDownload("target", null),
        TransferAppData.CameraUpload,
        TransferAppData.TextFileUpload(TransferAppData.TextFileUpload.Mode.Create, false),
        null,
    )

    private fun provideChatAppData() = listOf(
        TransferAppData.ChatUpload(12345L),
        TransferAppData.VoiceClip,
    )


    @Test
    fun `test that repository start upload is invoked for each nodeId when start upload is invoked`() =
        runTest {
            underTest(
                fileNodesAndNullNames, parentId, null,
                isHighPriority = false,
                isSourceTemporary = false
            ).test {
                fileNodesAndNullNames.keys.forEach { file ->
                    verify(transferRepository).startUpload(
                        file.absolutePath,
                        parentId,
                        null,
                        MODIFIED_TIME_SECS,
                        null,
                        isSourceTemporary = false,
                        shouldStartFirst = false,
                    )
                }
                awaitComplete()
            }
        }


    @Test
    fun `test that cancel token is canceled when start upload flow is canceled`() =
        runTest {
            stubDelay()
            underTest(
                fileNodesAndNullNames, parentId, null,
                isHighPriority = false, isSourceTemporary = false
            ).test {
                cancel()
                verify(cancelCancelTokenUseCase).invoke()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that cancel token is not invalidated when start upload flow is canceled before completion`() =
        runTest {
            stubDelay()
            underTest(
                fileNodesAndNullNames, parentId, null,
                isHighPriority = false,
                isSourceTemporary = false
            ).test {
                cancel()
                verify(invalidateCancelTokenUseCase, never()).invoke()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that cancel token is not canceled if start upload flow is not completed`() =
        runTest {
            stubDelay()
            underTest(
                fileNodesAndNullNames, parentId, null,
                isHighPriority = false,
                isSourceTemporary = false
            ).test {
                verify(cancelCancelTokenUseCase, never()).invoke()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that transfer single node events are emitted when each transfer is updated`() =
        runTest {
            stubSingleEvents()
            underTest(
                fileNodesAndNullNames, parentId, null,
                isHighPriority = false,
                isSourceTemporary = false
            )
                .filterIsInstance<MultiTransferEvent.SingleTransferEvent>().test {
                    repeat(fileNodesAndNullNames.size) {
                        assertThat(awaitItem().transferEvent)
                            .isInstanceOf(TransferEvent.TransferStartEvent::class.java)
                        assertThat(awaitItem().transferEvent)
                            .isInstanceOf(TransferEvent.TransferUpdateEvent::class.java)
                        assertThat(awaitItem().transferEvent)
                            .isInstanceOf(TransferEvent.TransferFinishEvent::class.java)
                    }
                    awaitComplete()
                }
        }


    @Test
    fun `test that addOrUpdateActiveTransferUseCase is invoked when each transfer is updated`() =
        runTest {
            stubSingleEvents()
            underTest(
                fileNodesAndNullNames, parentId, null,
                isHighPriority = false,
                isSourceTemporary = false
            )
                .filterIsInstance<MultiTransferEvent.SingleTransferEvent>().test {
                    cancelAndConsumeRemainingEvents()
                }
            verify(
                handleTransferEventUseCase,
                Times(fileNodesAndNullNames.size * 3)
            ).invoke(any())
        }

    @Test
    fun `test that handleSDCardEventUseCase is invoked when each transfer is updated`() =
        runTest {
            stubSingleEvents()
            underTest(
                fileNodesAndNullNames, parentId, null,
                isHighPriority = false,
                isSourceTemporary = false
            )
                .filterIsInstance<MultiTransferEvent.SingleTransferEvent>().test {
                    cancelAndConsumeRemainingEvents()
                }
            verify(
                handleSDCardEventUseCase,
                Times(fileNodesAndNullNames.size * 3)
            ).invoke(any())
        }

    @Test
    fun `test that fileName is used in startUpload when is not null`() = runTest {
        val name = "RenamedFile"
        underTest(mapOf(file to name), parentId, null, false, false).test {

            verify(transferRepository).startUpload(
                ABSOLUTE_PATH,
                parentId,
                name,
                MODIFIED_TIME_SECS,
                null,
                false,
                false,
            )
            awaitComplete()
        }
    }

    @ParameterizedTest(name = "appdata: \"{0}\"")
    @MethodSource("provideChatAppData")
    fun `test that fileName is used in startUploadForChat when is not null`(
        appData: TransferAppData.ChatTransferAppData
    ) = runTest {
        val name = "RenamedFile"
        underTest(mapOf(file to name), parentId, appData, false, false).test {
            verify(transferRepository).startUploadForChat(
                ABSOLUTE_PATH,
                parentId,
                name,
                appData,
                isSourceTemporary = false,
            )
            awaitComplete()
        }
    }

    private fun stubDelay() {
        fileNodesAndNullNames.keys.forEach { file ->
            whenever(
                transferRepository.startUpload(
                    file.absolutePath,
                    parentId,
                    null,
                    MODIFIED_TIME_SECS,
                    null,
                    isSourceTemporary = false,
                    shouldStartFirst = false,
                )
            ).thenReturn(
                flow { delay(100) }
            )
        }
    }

    private fun stubSingleEvents() {
        whenever(transfer.isFolderTransfer).thenReturn(false)
        val transferEventFlow = flowOf(
            mock<TransferEvent.TransferStartEvent> { on { it.transfer }.thenReturn(transfer) },
            mock<TransferEvent.TransferUpdateEvent> { on { it.transfer }.thenReturn(transfer) },
            mock<TransferEvent.TransferFinishEvent> { on { it.transfer }.thenReturn(transfer) },
        )
        fileNodesAndNullNames.keys.forEach { file ->
            whenever(
                transferRepository.startUpload(
                    file.absolutePath,
                    parentId,
                    null,
                    MODIFIED_TIME_SECS,
                    null,
                    isSourceTemporary = false,
                    shouldStartFirst = false,
                )
            ).thenReturn(transferEventFlow)
        }
    }


    companion object {
        private val file = mock<File> {
            on { name }.thenReturn(FILE_NAME)
            on { absolutePath }.thenReturn(ABSOLUTE_PATH)
            on { lastModified() }.thenReturn(MODIFIED_TIME_MILLIS)
        }
        private val fileNodesAndNullNames = (0L..10L).map { nodeId ->
            mock<File> {
                on { name }.thenReturn("$FILE_NAME$nodeId")
                on { absolutePath }.thenReturn("$ABSOLUTE_PATH$nodeId")
                on { lastModified() }.thenReturn(MODIFIED_TIME_MILLIS)
            }
        }.associateWith { null }
        private val parentId = NodeId(1L)

        private const val ABSOLUTE_PATH = "root/parent/destination/File"
        private const val FILE_NAME = "File"
        private const val MODIFIED_TIME_MILLIS = 1000L
        private const val MODIFIED_TIME_SECS = 1L
    }
}