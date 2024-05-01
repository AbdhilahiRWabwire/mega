package mega.privacy.android.data.facade

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FileFacadeTest {

    private lateinit var underTest: FileFacade
    private val context: Context = mock()

    @TempDir
    lateinit var temporaryFolder: File

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = FileFacade(context)
    }

    @Test
    fun `test that get external path by content uri returns the uri string`() = runTest {
        val uriMock = mockStatic(Uri::class.java)
        val environmentMock = mockStatic(android.os.Environment::class.java)
        val contentUri =
            "content://com.android.externalstorage.documents/tree/primary%3ASync%2FsomeFolder"
        val expected = "/storage/emulated/0/Sync/someFolder"
        val contentUriMock: Uri = mock()
        whenever(contentUriMock.toString()).thenReturn(contentUri)
        whenever(contentUriMock.lastPathSegment).thenReturn("primary:Sync/someFolder")
        whenever(Uri.parse(contentUri)).thenReturn(contentUriMock)
        whenever(Environment.getExternalStorageDirectory()).thenReturn(
            File("/storage/emulated/0")
        )

        val actual = underTest.getExternalPathByContentUri(contentUri)

        assertThat(expected).isEqualTo(actual)

        uriMock.close()
        environmentMock.close()
    }

    @Test
    fun `test that buildExternalStorageFile returns correctly`() = runTest {
        val file = mock<File> {
            on { absolutePath } doReturn "/storage/emulated/0"
        }
        val environmentMock = mockStatic(Environment::class.java)
        whenever(Environment.getExternalStorageDirectory()).thenReturn(file)
        val actual = underTest.buildExternalStorageFile("/Mega.txt")

        assertThat(actual.path).isEqualTo("/storage/emulated/0/Mega.txt")
        environmentMock.close()
    }

    @Test
    fun `test that get file by path returns file if file exists`() = runTest {
        val result = underTest.getFileByPath(temporaryFolder.path)

        assertThat(result).isEqualTo(temporaryFolder)
    }

    @Test
    fun `test that get file by path returns null if file does not exist`() = runTest {
        val result = underTest.getFileByPath("non/existent/path")

        assertThat(result).isNull()
    }

    @Test
    fun `test that getTotalSize returns correct file size`() = runTest {
        val expectedSize = 1000L
        val file = mock<File> {
            on { isFile } doReturn true
            on { length() } doReturn expectedSize
        }

        val actualSize = underTest.getTotalSize(file)
        assertEquals(expectedSize, actualSize)
    }

    @Test
    fun `test that getTotalSize returns correct total size if it's a directory`() = runTest {
        val file1 = mock<File> {
            on { isFile } doReturn true
            on { isDirectory } doReturn false
            on { length() } doReturn 1000L
        }
        val file2 = mock<File> {
            on { isFile } doReturn true
            on { isDirectory } doReturn false
            on { length() } doReturn 1500L
        }
        val childDir = mock<File> {
            on { isFile } doReturn false
            on { isDirectory } doReturn true
            on { listFiles() } doReturn arrayOf(file1, file2)
        }
        val dir = mock<File> {
            on { isFile } doReturn false
            on { isDirectory } doReturn true
            on { listFiles() } doReturn arrayOf(file1, file2, childDir)
        }

        val actualSize = underTest.getTotalSize(dir)
        assertThat(actualSize).isEqualTo(5000L)
    }

    @Test
    fun `test that delete file by uri returns correct result`() = runTest {
        val contentUriMock: Uri = mock()
        val contentResolver = mock<ContentResolver>()
        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(contentResolver.delete(contentUriMock, null, null)).thenReturn(1)
        val result = underTest.deleteFileByUri(contentUriMock)

        assertThat(result).isTrue()
    }
}