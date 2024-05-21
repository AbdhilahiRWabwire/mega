package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import java.util.stream.Stream

/**
 * Test class for [GetSecondaryFolderPathUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetSecondaryFolderPathUseCaseTest {

    private lateinit var underTest: GetSecondaryFolderPathUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetSecondaryFolderPathUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
            fileSystemRepository = fileSystemRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadsRepository,
            fileSystemRepository,
        )
    }

    @ParameterizedTest(name = "path: {0}")
    @ValueSource(strings = ["", " ", "test/path/"])
    fun `test that the local secondary folder path is returned`(path: String) = runTest {
        cameraUploadsRepository.stub {
            onBlocking { getSecondaryFolderLocalPath() }.thenReturn(path)
        }
        fileSystemRepository.stub {
            onBlocking { doesFolderExists(any()) }.thenReturn(true)
        }

        assertThat(underTest()).isEqualTo(path)
    }

    @Test
    fun `test that an empty secondary folder path is set and returned if the previously set path does not exist`() =
        runTest {
            val testPath = "test/path/"

            cameraUploadsRepository.stub {
                onBlocking { getSecondaryFolderLocalPath() }.thenReturn(testPath)
            }
            fileSystemRepository.stub {
                onBlocking { doesFolderExists(any()) }.thenReturn(false)
            }

            val expected = underTest()
            assertThat(expected).isNotEqualTo(testPath)
            assertThat(expected).isEmpty()
            verify(cameraUploadsRepository).setSecondaryFolderLocalPath("")
        }

    @ParameterizedTest(name = "when the original path is {0}, the new path becomes {1}")
    @MethodSource("providePathParameters")
    fun `test that the separator is appended in the secondary folder path`(
        originalPath: String,
        newPath: String,
    ) = runTest {
        cameraUploadsRepository.stub {
            onBlocking { getSecondaryFolderLocalPath() }.thenReturn(originalPath)
        }
        fileSystemRepository.stub {
            onBlocking { doesFolderExists(any()) }.thenReturn(true)
        }
        val expectedPath = underTest()
        assertThat(expectedPath).isEqualTo(newPath)
    }

    private fun providePathParameters() = Stream.of(
        Arguments.of("", ""),
        Arguments.of(" ", " "),
        Arguments.of("test/path", "test/path/"),
        Arguments.of("test/path/", "test/path/"),
        Arguments.of("test/path//", "test/path//")
    )
}
