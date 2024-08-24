package mega.privacy.android.data.mapper.transfer

import com.google.common.truth.Truth
import mega.privacy.android.data.mapper.MegaExceptionMapper
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TransferEventMapperTest {
    private lateinit var underTest: TransferEventMapper

    private val exceptionMapper = mock<MegaExceptionMapper>()
    private val transferMapper = mock<TransferMapper>()
    private val errorContextMapper = mock<ErrorContextMapper>()
    private val mockTransfer = mock<Transfer>()
    private val megaTransfer = mock<MegaTransfer>()
    private val transferStageMapper = mock<TransferStageMapper>()

    val megaError = mock<MegaError> {
        on { errorCode }.thenReturn(MegaError.API_OK)
    }

    private val megaOverQuotaError = mock<MegaError> {
        on { errorCode }.thenReturn(MegaError.API_EOVERQUOTA)
        on { errorString }.thenReturn("")
        on { value }.thenReturn(0L)
    }

    private val megaOtherError = mock<MegaError> {
        on { errorCode }.thenReturn(MegaError.API_EBLOCKED)
        on { errorString }.thenReturn("")
        on { value }.thenReturn(0L)
    }

    @BeforeAll
    fun setup() {
        underTest = TransferEventMapper(
            transferMapper, exceptionMapper, errorContextMapper,
            transferStageMapper
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(exceptionMapper, transferMapper, transferStageMapper)
    }

    @ParameterizedTest(name = "invoked with {0} and returns {1}")
    @MethodSource("provideParameters")
    fun `test that transfer event mapper returns correctly`(
        globalTransfer: GlobalTransfer,
        transferEvent: TransferEvent,
    ) {
        whenever(transferMapper(megaTransfer)).thenReturn(mockTransfer)
        whenever(transferStageMapper(any())).thenReturn(TransferStage.STAGE_NONE)

        val actual = underTest.invoke(globalTransfer)
        when (actual) {
            is TransferEvent.TransferFinishEvent -> {
                Truth.assertThat(actual.error).isNull()
            }

            is TransferEvent.TransferTemporaryErrorEvent -> {
                Truth.assertThat(actual.error).isNull()
            }

            else -> {

            }
        }
        Truth.assertThat(actual).isEqualTo(transferEvent)

    }


    @ParameterizedTest(name = "invoked with {0} and returns {1}")
    @MethodSource("provideExceptionParameters")
    fun `test that transfer event mapper map exception correctly`(
        globalTransfer: GlobalTransfer,
        transferEvent: TransferEvent,
    ) {
        whenever(transferMapper(megaTransfer)).thenReturn(mockTransfer)
        whenever(exceptionMapper(megaOtherError)).thenReturn(
            MegaException(
                MegaError.API_EBLOCKED,
                "",
                0L
            )
        )
        whenever(exceptionMapper(megaOverQuotaError)).thenReturn(
            QuotaExceededMegaException(
                MegaError.API_EOVERQUOTA,
                "",
                0L
            )
        )
        when (val actual = underTest.invoke(globalTransfer)) {
            is TransferEvent.TransferFinishEvent -> {
                Truth.assertThat(actual.error?.errorCode)
                    .isEqualTo((transferEvent as TransferEvent.TransferFinishEvent).error?.errorCode)
            }

            is TransferEvent.TransferTemporaryErrorEvent -> {
                Truth.assertThat(actual.error?.errorCode)
                    .isEqualTo((transferEvent as TransferEvent.TransferTemporaryErrorEvent).error?.errorCode)
            }

            else -> {

            }
        }
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(
            GlobalTransfer.OnTransferStart(megaTransfer),
            TransferEvent.TransferStartEvent(mockTransfer)
        ),
        Arguments.of(
            GlobalTransfer.OnTransferData(megaTransfer, null),
            TransferEvent.TransferDataEvent(mockTransfer, null)
        ),
        Arguments.of(
            GlobalTransfer.OnTransferUpdate(megaTransfer),
            TransferEvent.TransferUpdateEvent(mockTransfer)
        ),
        Arguments.of(
            GlobalTransfer.OnTransferTemporaryError(megaTransfer, megaError),
            TransferEvent.TransferTemporaryErrorEvent(mockTransfer, null)
        ),
        Arguments.of(
            GlobalTransfer.OnTransferFinish(megaTransfer, megaError),
            TransferEvent.TransferFinishEvent(mockTransfer, null)
        ),
        Arguments.of(
            GlobalTransfer.OnFolderTransferUpdate(megaTransfer, 1, 5, 1, 2, "folder", "leaf"),
            TransferEvent.FolderTransferUpdateEvent(
                mockTransfer,
                TransferStage.STAGE_NONE,
                5,
                1,
                2,
                "folder",
                "leaf"
            )
        ),
    )

    private fun provideExceptionParameters() = Stream.of(
        Arguments.of(
            GlobalTransfer.OnTransferTemporaryError(megaTransfer, megaOverQuotaError),
            TransferEvent.TransferTemporaryErrorEvent(
                mockTransfer,
                QuotaExceededMegaException(
                    megaOverQuotaError.errorCode,
                    megaOverQuotaError.errorString,
                    megaOverQuotaError.value
                )
            )
        ),
        Arguments.of(
            GlobalTransfer.OnTransferTemporaryError(megaTransfer, megaOtherError),
            TransferEvent.TransferTemporaryErrorEvent(
                mockTransfer,
                MegaException(
                    megaOtherError.errorCode,
                    megaOtherError.errorString,
                    megaOtherError.value
                )
            )
        ),
        Arguments.of(
            GlobalTransfer.OnTransferFinish(megaTransfer, megaOverQuotaError),
            TransferEvent.TransferFinishEvent(
                mockTransfer,
                QuotaExceededMegaException(
                    megaOverQuotaError.errorCode,
                    megaOverQuotaError.errorString,
                    megaOverQuotaError.value
                )
            )
        ),
        Arguments.of(
            GlobalTransfer.OnTransferFinish(megaTransfer, megaOtherError),
            TransferEvent.TransferFinishEvent(
                mockTransfer,
                MegaException(
                    megaOtherError.errorCode,
                    megaOtherError.errorString,
                    megaOtherError.value
                )
            )
        ),
    )
}
