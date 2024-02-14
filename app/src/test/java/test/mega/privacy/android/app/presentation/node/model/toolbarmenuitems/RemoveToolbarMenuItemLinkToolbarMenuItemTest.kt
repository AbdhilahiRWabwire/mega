package test.mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import androidx.navigation.NavHostController
import com.google.common.truth.Truth
import com.google.gson.Gson
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveLinkToolbarMenuItem
import mega.privacy.android.app.presentation.search.model.navigation.removeNodeLinkRoute
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.io.File
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoveToolbarMenuItemLinkToolbarMenuItemTest {

    private val underTest = RemoveLinkToolbarMenuItem()


    private val nodeWithExportedData = mock<TypedFolderNode> {
        on { exportedData }.thenReturn(
            ExportedData(
                publicLinkCreationTime = 123456L,
                publicLink = "link"
            )
        )
    }
    private val nodeWithoutExportedData = mock<TypedFolderNode> {
        on { exportedData }.thenReturn(null)
    }
    private val multipleNodes = listOf(nodeWithExportedData, nodeWithoutExportedData)

    @ParameterizedTest(name = "when is selected node taken down is {0} and selected nodes are {1}, then is remove link item visible is {2}")
    @MethodSource("provideArguments")
    fun `test that the remove link item visibility is adjusted`(
        noNodeTakenDown: Boolean,
        selectedNodes: List<TypedNode>,
        expected: Boolean,
    ) {
        val result = underTest.shouldDisplay(
            hasNodeAccessPermission = false,
            selectedNodes = selectedNodes,
            canBeMovedToTarget = false,
            noNodeInBackups = false,
            noNodeTakenDown = noNodeTakenDown,
            allFileNodes = false,
            resultCount = 10
        )
        Truth.assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that the remove link item click action is correct`() {
        val selectedNode = mock<TypedFolderNode> {
            on { exportedData }.thenReturn(
                ExportedData(
                    publicLinkCreationTime = 123456L,
                    publicLink = "link"
                )
            )
        }
        val onDismiss = mock<() -> Unit>()
        val actionHandler = mock<(menuAction: Any, nodes: List<TypedNode>) -> Unit>()
        val navController = mock<NavHostController>()
        val onClick = underTest.getOnClick(
            selectedNodes = listOf(selectedNode),
            onDismiss = onDismiss,
            actionHandler = actionHandler,
            navController = navController
        )
        onClick.invoke()
        verify(onDismiss).invoke()
        verify(navController).navigate(
            removeNodeLinkRoute.plus(File.separator)
                .plus(Gson().toJson(listOf(selectedNode.id.longValue)))
        )
    }

    private fun provideArguments() = Stream.of(
        Arguments.of(false, emptyList<TypedFolderNode>(), false),
        Arguments.of(false, listOf(nodeWithExportedData), false),
        Arguments.of(false, listOf(nodeWithoutExportedData), false),
        Arguments.of(false, multipleNodes, false),
        Arguments.of(true, emptyList<TypedFolderNode>(), false),
        Arguments.of(true, listOf(nodeWithExportedData), true),
        Arguments.of(true, listOf(nodeWithoutExportedData), false),
        Arguments.of(true, multipleNodes, false),
    )
}