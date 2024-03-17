package test.mega.privacy.android.app.presentation.mediaplayer

import com.google.common.truth.Truth
import com.jraska.livedata.test
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.data.extensions.observeOnce
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.mediaplayer.MediaPlayerViewModel
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.usecase.LegacyCopyNodeUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.node.MoveNodeUseCase
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.presentation.myaccount.InstantTaskExecutorExtension

@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MediaPlayerViewModelTest {
    private lateinit var underTest: MediaPlayerViewModel

    private lateinit var checkNameCollision: CheckNameCollision
    private lateinit var checkNameCollisionUseCase: CheckNameCollisionUseCase
    private lateinit var copyNodeUseCase: CopyNodeUseCase
    private lateinit var legacyCopyNodeUseCase: LegacyCopyNodeUseCase
    private lateinit var moveNodeUseCase: MoveNodeUseCase
    private lateinit var getNodeByHandle: GetNodeByHandle

    @BeforeAll
    fun initialise() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    @BeforeEach
    fun setUp() {
        checkNameCollision = mock()
        copyNodeUseCase = mock()
        moveNodeUseCase = mock()
        checkNameCollisionUseCase = mock()
        legacyCopyNodeUseCase = mock()
        getNodeByHandle = mock()
        underTest = MediaPlayerViewModel(
            checkNameCollision = checkNameCollision,
            copyNodeUseCase = copyNodeUseCase,
            moveNodeUseCase = moveNodeUseCase,
            getNodeByHandle = getNodeByHandle,
            legacyCopyNodeUseCase = legacyCopyNodeUseCase,
            checkNameCollisionUseCase = checkNameCollisionUseCase,
            legacyPublicAlbumPhotoNodeProvider = mock(),
        )
    }

    @AfterAll
    fun tearDown() {
        RxAndroidPlugins.reset()
    }

    @Test
    internal fun `test that copy complete snack bar is shown when file is copied to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNameCollision(
                    nodeHandle = NodeId(selectedNode),
                    parentHandle = NodeId(newParentNode),
                    type = NameCollisionType.COPY,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            whenever(
                copyNodeUseCase(
                    nodeToCopy = NodeId(selectedNode),
                    newNodeParent = NodeId(newParentNode), newNodeName = null
                )
            ).thenReturn(NodeId(selectedNode))
            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            testScheduler.advanceUntilIdle()
            underTest.onSnackbarMessage().test().assertValue(R.string.context_correctly_copied)
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when copy failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNameCollision(
                    nodeHandle = NodeId(selectedNode),
                    parentHandle = NodeId(newParentNode),
                    type = NameCollisionType.COPY,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            val runtimeException = RuntimeException("Copy node failed")
            whenever(
                copyNodeUseCase(
                    nodeToCopy = NodeId(selectedNode),
                    newNodeParent = NodeId(newParentNode), newNodeName = null
                )
            ).thenAnswer { throw runtimeException }
            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onExceptionThrown().test().assertValue(runtimeException)
        }

    @Test
    internal fun `test move complete snack bar is shown when file is moved to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNameCollision(
                    nodeHandle = NodeId(selectedNode),
                    parentHandle = NodeId(newParentNode),
                    type = NameCollisionType.MOVE,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            whenever(
                moveNodeUseCase(
                    nodeToMove = NodeId(selectedNode),
                    newNodeParent = NodeId(newParentNode)
                )
            ).thenReturn(NodeId(selectedNode))
            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onSnackbarMessage().test().assertValue(R.string.context_correctly_moved)
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when move failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNameCollision(
                    nodeHandle = NodeId(selectedNode),
                    parentHandle = NodeId(newParentNode),
                    type = NameCollisionType.MOVE,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            val runtimeException = RuntimeException("Move node failed")
            whenever(
                moveNodeUseCase(
                    nodeToMove = NodeId(selectedNode),
                    newNodeParent = NodeId(newParentNode)
                )
            ).thenThrow(runtimeException)
            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onExceptionThrown().test().assertValue(runtimeException)
        }

    @Test
    internal fun `test copy complete snack bar is shown when file is imported to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            val nodeToImport = mock<MegaNode> {
                on { handle }.thenReturn(selectedNode)
            }
            val parentNode = mock<MegaNode>()
            whenever(getNodeByHandle(newParentNode)).thenReturn(parentNode)
            whenever(
                checkNameCollisionUseCase.check(
                    node = nodeToImport,
                    parentNode = parentNode,
                    type = NameCollisionType.COPY,
                )
            ).thenReturn(Single.error(MegaNodeException.ChildDoesNotExistsException()))
            whenever(
                legacyCopyNodeUseCase.copy(
                    node = nodeToImport,
                    parentHandle = newParentNode
                )
            ).thenReturn(Completable.complete())
            underTest.importNode(
                node = nodeToImport,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onSnackbarMessage().observeOnce {
                Truth.assertThat(it).isEqualTo(R.string.context_correctly_copied)
            }
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when import failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            val nodeToImport = mock<MegaNode> {
                on { handle }.thenReturn(selectedNode)
            }
            val parentNode = mock<MegaNode>()
            whenever(getNodeByHandle(newParentNode)).thenReturn(parentNode)
            whenever(
                checkNameCollisionUseCase.check(
                    node = nodeToImport,
                    parentNode = parentNode,
                    type = NameCollisionType.COPY,
                )
            ).thenReturn(Single.error(MegaNodeException.ChildDoesNotExistsException()))
            val runtimeException = RuntimeException("Import node failed")
            whenever(
                legacyCopyNodeUseCase.copy(
                    node = nodeToImport,
                    parentHandle = newParentNode
                )
            ).thenReturn(Completable.error(runtimeException))
            underTest.importNode(
                node = nodeToImport,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onExceptionThrown().observeOnce {
                Truth.assertThat(it).isEqualTo(runtimeException)
            }
        }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}