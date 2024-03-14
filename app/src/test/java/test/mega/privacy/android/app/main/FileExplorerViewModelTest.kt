package test.mega.privacy.android.app.main

import android.content.Intent
import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.FileExplorerViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.GetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.GetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.chat.message.AttachNodeUseCase
import mega.privacy.android.domain.usecase.chat.message.SendChatAttachmentsUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileExplorerViewModelTest {
    private lateinit var underTest: FileExplorerViewModel
    private val getCopyLatestTargetPathUseCase = mock<GetCopyLatestTargetPathUseCase>()
    private val getMoveLatestTargetPathUseCase = mock<GetMoveLatestTargetPathUseCase>()
    private val getNodeAccessPermission = mock<GetNodeAccessPermission>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val attachNodeUseCase = mock<AttachNodeUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val sendChatAttachmentsUseCase = mock<SendChatAttachmentsUseCase>()

    @BeforeAll
    fun setUp() {

        underTest = FileExplorerViewModel(
            ioDispatcher = StandardTestDispatcher(),
            monitorStorageStateEventUseCase = mock(),
            getCopyLatestTargetPathUseCase = getCopyLatestTargetPathUseCase,
            getMoveLatestTargetPathUseCase = getMoveLatestTargetPathUseCase,
            getNodeAccessPermission = getNodeAccessPermission,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            attachNodeUseCase = attachNodeUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            sendChatAttachmentsUseCase = sendChatAttachmentsUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() = reset(
        getCopyLatestTargetPathUseCase,
        getMoveLatestTargetPathUseCase,
        getNodeAccessPermission,
        getFeatureFlagValueUseCase,
        attachNodeUseCase,
        getNodeByIdUseCase,
        sendChatAttachmentsUseCase,
    )

    /**
     * Checks if it is importing a text instead of files.
     * This is true if the action of the intent is ACTION_SEND, the type of the intent
     * is TYPE_TEXT_PLAIN and the intent does not contain EXTRA_STREAM extras.
     *
     */

    @Test
    fun `test that an intent with action send, type plain text and no stream extra is marked as a text import`() {

        val intent = mock<Intent> {
            on { action }.thenReturn(Intent.ACTION_SEND)
            on { type }.thenReturn(Constants.TYPE_TEXT_PLAIN)
        }

        assertThat(underTest.isImportingText(intent)).isTrue()
    }

    @Test
    fun `test that an intent with a stream extra is marked as not a text import`() {

        val bundle = mock<Bundle> {
            on { containsKey(Intent.EXTRA_STREAM) }.thenReturn(true)
        }

        val intent = mock<Intent> {
            on { action }.thenReturn(Intent.ACTION_SEND)
            on { type }.thenReturn(Constants.TYPE_TEXT_PLAIN)
            on { extras }.thenReturn(bundle)
        }

        assertThat(underTest.isImportingText(intent)).isFalse()
    }

    @Test
    fun `test that toDoIfFalse is invoked if feature flag is false`() = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.NewChatActivity)) doReturn false
        val toDoIfFalse = mock<() -> Unit>()

        underTest.uploadFilesToChatIfFeatureFlagIsTrue(
            emptyList(),
            emptyList(),
            emptyList(),
            toDoIfFalse = toDoIfFalse
        ) {}

        verify(toDoIfFalse).invoke()
    }

    @Test
    fun `test that toDoIfFalse is invoked if feature flag is true`() = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.NewChatActivity)) doReturn true
        val toDoIfFalse = mock<() -> Unit>()

        underTest.uploadFilesToChatIfFeatureFlagIsTrue(
            emptyList(),
            emptyList(),
            emptyList(),
            toDoIfFalse = toDoIfFalse
        ) {}

        verifyNoInteractions(toDoIfFalse)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that toDoAfter is invoked regardless of feature flag value`(
        featureFlagValue: Boolean,
    ) = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.NewChatActivity)) doReturn featureFlagValue
        val toDoAfter = mock<() -> Unit>()

        underTest.uploadFilesToChatIfFeatureFlagIsTrue(
            emptyList(),
            emptyList(),
            emptyList(),
            {},
            toDoAfter = toDoAfter
        )

        verify(toDoAfter).invoke()
    }

    @Test
    fun `test that files are attached`() = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.NewChatActivity)) doReturn true
        val filePaths = listOf("path1", "path2")

        val flow1 = mock<Flow<MultiTransferEvent>>()
        val flow2 = mock<Flow<MultiTransferEvent>>()
        whenever(
            sendChatAttachmentsUseCase(chatId1, filePaths.associateWith { null })
        ) doReturn flow1
        whenever(
            sendChatAttachmentsUseCase(chatId2, filePaths.associateWith { null })
        ) doReturn flow2

        underTest.uploadFilesToChatIfFeatureFlagIsTrue(
            chatIds = chatIds,
            filePaths = filePaths,
            emptyList(),
            {}, {}
        )

        verify(flow1).collect(any())
        verify(flow2).collect(any())
    }

    @Test
    fun `test that nodes are attached`() = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.NewChatActivity)) doReturn true

        val nodeId1 = NodeId(1L)
        val nodeId2 = NodeId(2L)
        val nodeIds = listOf(nodeId1, nodeId2)
        val fileNode1 = mock<TypedFileNode>()
        val fileNode2 = mock<TypedFileNode>()
        whenever(getNodeByIdUseCase(nodeId1)) doReturn fileNode1
        whenever(getNodeByIdUseCase(nodeId2)) doReturn fileNode2

        underTest.uploadFilesToChatIfFeatureFlagIsTrue(
            chatIds = chatIds,
            emptyList(),
            nodeIds = nodeIds,
            {}, {}
        )

        chatIds.forEach {
            verify(attachNodeUseCase).invoke(it, fileNode1)
            verify(attachNodeUseCase).invoke(it, fileNode2)
        }
    }

    private val chatId1 = 10L
    private val chatId2 = 20L
    private val chatIds = listOf(chatId1, chatId2)

}