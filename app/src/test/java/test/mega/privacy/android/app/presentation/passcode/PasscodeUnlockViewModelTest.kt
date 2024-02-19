package test.mega.privacy.android.app.presentation.passcode

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.passcode.PasscodeUnlockViewModel
import mega.privacy.android.app.presentation.passcode.mapper.PasscodeTypeMapper
import mega.privacy.android.app.presentation.passcode.model.PasscodeUIType
import mega.privacy.android.app.presentation.passcode.model.PasscodeUnlockState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.passcode.PasscodeType
import mega.privacy.android.domain.entity.passcode.UnlockPasscodeRequest
import mega.privacy.android.domain.exception.security.NoPasscodeTypeSetException
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeAttemptsUseCase
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeTypeUseCase
import mega.privacy.android.domain.usecase.passcode.UnlockPasscodeUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.extensions.asHotFlow
import test.mega.privacy.android.app.extensions.withCoroutineExceptions

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PasscodeUnlockViewModelTest {
    private lateinit var underTest: PasscodeUnlockViewModel
    private val unlockPasscodeUseCase = mock<UnlockPasscodeUseCase>()
    private val monitorPasscodeAttemptsUseCase = mock<MonitorPasscodeAttemptsUseCase>()
    private val monitorPasscodeTypeUseCase = mock<MonitorPasscodeTypeUseCase>()
    private val passcodeTypeMapper = mock<PasscodeTypeMapper>()

    @AfterEach
    internal fun cleanup() {
        Mockito.reset(
            unlockPasscodeUseCase,
            monitorPasscodeAttemptsUseCase,
            monitorPasscodeTypeUseCase,
            passcodeTypeMapper,
        )
    }

    @Test
    internal fun `test that initial state is loading`() =
        runTest {
            initViewModel(
                monitorPasscodeAttemptsUseCaseStub = monitorPasscodeAttemptsUseCase.stub {
                    on { invoke() }.thenReturn(
                        flow { awaitCancellation() })
                },
            )
            underTest.state.test {
                val actual = awaitItem()
                assertThat(actual).isInstanceOf(PasscodeUnlockState.Loading::class.java)
            }
        }

    @Test
    internal fun `test that failed attempts count is updated when the count changes`() =
        runTest {
            val expected = 1

            initViewModel(
                monitorPasscodeAttemptsUseCaseStub = monitorPasscodeAttemptsUseCase.stub {
                    on { invoke() }.thenReturn(expected.asHotFlow())
                }
            )

            underTest.state.filterIsInstance<PasscodeUnlockState.Data>().test {
                assertThat(awaitItem().failedAttempts).isEqualTo(expected)
            }
        }

    @Test
    internal fun `test that logout warning becomes true after 5 incorrect attempts`() = runTest {
        initViewModel(
            monitorPasscodeAttemptsUseCaseStub = monitorPasscodeAttemptsUseCase.stub {
                on { invoke() }.thenReturn(5.asHotFlow())
            }
        )

        underTest.state.filterIsInstance<PasscodeUnlockState.Data>().test {
            assertThat(awaitItem().logoutWarning).isEqualTo(true)
        }
    }

    @Test
    internal fun `test that an exception from the unlock use case does not get propagated`() =
        withCoroutineExceptions {
            runTest {
                unlockPasscodeUseCase.stub {
                    onBlocking { invoke(any()) }.thenAnswer { throw Exception("Unlock threw an exception") }
                }
                underTest.state.test {
                    awaitItem()
                    underTest.unlockWithPasscode("passcode")
                    val events = cancelAndConsumeRemainingEvents()
                    assertThat(events).isEmpty()
                }
            }
        }

    @Test
    internal fun `test that calling unlock with a password also calls the unlock use case`() =
        runTest {
            val expected = "correct"

            initViewModel()

            underTest.unlockWithPassword(expected)

            verify(unlockPasscodeUseCase).invoke(UnlockPasscodeRequest.PasswordRequest(expected))
        }

    @Test
    internal fun `test that an exception from the monitor attempts use case does not get propagated`() =
        withCoroutineExceptions {
            runTest {


                initViewModel(
                    monitorPasscodeAttemptsUseCaseStub = monitorPasscodeAttemptsUseCase.stub {
                        on { invoke() }.thenAnswer { throw Exception("Monitor threw an exception") }
                    }
                )

                underTest.state.test {
                    awaitItem()
                    val events = cancelAndConsumeRemainingEvents()
                    assertThat(events).isEmpty()
                }
            }
        }

    @Test
    internal fun `test that passcode type is mapped and returned`() = runTest {
        val expected = PasscodeUIType.Alphanumeric(false)

        initViewModel(
            passcodeTypeMapperStub = passcodeTypeMapper.stub {
                on { invoke(any()) }.thenReturn(expected)
            }
        )

        underTest.state.filterIsInstance<PasscodeUnlockState.Data>().test {
            assertThat(awaitItem().passcodeType).isEqualTo(expected)
        }
    }

    @Test
    internal fun `test that an exception from the get passcode type is not propagated`() =
        withCoroutineExceptions {
            runTest {
                initViewModel(
                    monitorPasscodeTypeUseCaseStub = monitorPasscodeTypeUseCase.stub {
                        onBlocking { invoke() }.thenAnswer { throw NoPasscodeTypeSetException() }
                    }
                )

                underTest.state.test {
                    assertThat(awaitItem()).isEqualTo(PasscodeUnlockState.Loading)
                }
            }
        }

    @Test
    internal fun `test that calling unlock with a biometrics calls the unlock use case`() =
        runTest {

            initViewModel()

            underTest.unlockWithBiometrics()

            verify(unlockPasscodeUseCase).invoke(UnlockPasscodeRequest.BiometricRequest)
        }

    private fun initViewModel(
        monitorPasscodeAttemptsUseCaseStub: MonitorPasscodeAttemptsUseCase =
            monitorPasscodeAttemptsUseCase.stub {
                on { invoke() }.thenReturn(0.asHotFlow())
            },
        unlockPasscodeUseCaseStub: UnlockPasscodeUseCase = unlockPasscodeUseCase,
        monitorPasscodeTypeUseCaseStub: MonitorPasscodeTypeUseCase =
            monitorPasscodeTypeUseCase.stub {
                on { invoke() }.thenReturn(PasscodeType.Password.asHotFlow())
            },
        passcodeTypeMapperStub: PasscodeTypeMapper = passcodeTypeMapper.stub {
            on { invoke(any()) }.thenReturn(PasscodeUIType.Alphanumeric(false))
        },
    ) {
        underTest = PasscodeUnlockViewModel(
            monitorPasscodeAttemptsUseCase = monitorPasscodeAttemptsUseCaseStub,
            unlockPasscodeUseCase = unlockPasscodeUseCaseStub,
            monitorPasscodeTypeUseCase = monitorPasscodeTypeUseCaseStub,
            passcodeTypeMapper = passcodeTypeMapperStub,
        )
    }
}