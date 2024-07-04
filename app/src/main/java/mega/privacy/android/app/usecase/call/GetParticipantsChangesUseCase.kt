package mega.privacy.android.app.usecase.call

import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.CustomCountDownTimer
import mega.privacy.android.app.utils.Constants.TYPE_JOIN
import mega.privacy.android.app.utils.Constants.TYPE_LEFT
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.CallCompositionChanges
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.MainImmediateDispatcher
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatCall
import timber.log.Timber
import javax.inject.Inject

/**
 * Main use case to get changes in participants
 */
class GetParticipantsChangesUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid,
    private val getCallUseCase: GetCallUseCase,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    @ApplicationScope private val sharingScope: CoroutineScope,
    @MainImmediateDispatcher private val mainImmediateDispatcher: CoroutineDispatcher,
) {

    companion object {
        const val MAX_NUM_OF_WAITING_SHIFTS = 2
        const val NUM_OF_SECONDS_TO_WAIT: Long = 1
    }

    var joinedCountDownTimer: CustomCountDownTimer? = null
    var leftCountDownTimer: CustomCountDownTimer? = null

    var numberOfShiftsToWaitToJoin = MAX_NUM_OF_WAITING_SHIFTS
    var numberOfShiftsToWaitToLeft = MAX_NUM_OF_WAITING_SHIFTS

    val joinedParticipantLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val leftParticipantLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val peerIdsJoined = ArrayList<Long>()
    val peerIdsLeft = ArrayList<Long>()

    /**
     * Participants' changes result
     *
     * @property chatId        Chat ID of the call
     * @property typeChange    TYPE_JOIN or TYPE_LEFT
     * @property peers        List of user IDs
     */
    data class ParticipantsChangesResult(
        val chatId: Long,
        val typeChange: Int,
        val peers: ArrayList<Long>?,
    )

    /**
     * Num participants changes result
     *
     * @property chatId        Chat ID of the call
     * @property onlyMeInTheCall    True, if I'm the only one in the call. False, if there are more participants.
     * @property waitingForOthers True, if I'm waiting for others participants. False, otherwise.
     * @property isReceivedChange True, if the changes is received. False, if no change has been received.
     */
    data class NumParticipantsChangesResult(
        val chatId: Long,
        val onlyMeInTheCall: Boolean,
        val waitingForOthers: Boolean,
        var isReceivedChange: Boolean)

    /**
     * Method to check if I am alone on any call and whether it is because I am waiting for others or because everyone has dropped out of the call.
     */
    fun checkIfIAmAloneOnAnyCall(): Flowable<NumParticipantsChangesResult> =
        Flowable.create({ emitter ->
            getCallUseCase.getCallsInProgressAndOnHold().let { calls ->
                for (call in calls) {
                    val result: NumParticipantsChangesResult = checkIfIAmAloneOnSpecificCall(call)
                    result.isReceivedChange = false
                    emitter.onNext(result)
                }
            }

            sharingScope.launch {
                monitorChatCallUpdatesUseCase()
                    .catch { Timber.e(it) }
                    .collectLatest { call ->
                        withContext(mainImmediateDispatcher) {
                            call.changes?.apply {
                                Timber.d("Monitor chat call updated, changes $this")
                                if (contains(ChatCallChanges.CallComposition)) {
                                    if (call.status == ChatCallStatus.InProgress || call.status == ChatCallStatus.Joining) {
                                        emitter.onNext(checkIfIAmAloneOnSpecificCall(call))
                                    } else if (call.status != ChatCallStatus.Destroyed && call.status != ChatCallStatus.UserNoPresent && call.status != ChatCallStatus.TerminatingUserParticipation) {
                                        emitter.onNext(
                                            NumParticipantsChangesResult(
                                                call.chatId,
                                                onlyMeInTheCall = false,
                                                waitingForOthers = false,
                                                isReceivedChange = true
                                            )
                                        )

                                    }
                                }
                            }
                        }
                    }
            }
        }, BackpressureStrategy.LATEST)

    /**
     * Method to check if I am alone on a specific call and whether it is because I am waiting for others or because everyone has dropped out of the call
     *
     * @param call MegaChatCall
     * @return NumParticipantsChangesResult
     */
    fun checkIfIAmAloneOnSpecificCall(call: MegaChatCall): NumParticipantsChangesResult {
        var waitingForOthers = false
        var onlyMeInTheCall = false
        megaChatApi.getChatRoom(call.chatid)?.let { chat ->
            val isOneToOneCall = !chat.isGroup && !chat.isMeeting
            if (!isOneToOneCall) {
                call.peeridParticipants?.let { list ->
                    onlyMeInTheCall =
                        list.size().toInt() == 1 && list.get(0) == megaChatApi.myUserHandle

                    waitingForOthers = onlyMeInTheCall &&
                            MegaApplication.getChatManagement().isRequestSent(call.callId)
                }
            }
        }

        return NumParticipantsChangesResult(call.chatid,
            onlyMeInTheCall,
            waitingForOthers,
            isReceivedChange = true)
    }

    /**
     * Method to check if I am alone on a specific call and whether it is because I am waiting for others or because everyone has dropped out of the call
     *
     * @param call MegaChatCall
     * @return NumParticipantsChangesResult
     */
    private fun checkIfIAmAloneOnSpecificCall(call: ChatCall): NumParticipantsChangesResult {
        var waitingForOthers = false
        var onlyMeInTheCall = false
        runCatching {
            megaChatApi.getChatRoom(call.chatId)?.let { chat ->
                val isOneToOneCall = !chat.isGroup && !chat.isMeeting
                if (!isOneToOneCall) {
                    call.peerIdParticipants?.let { list ->
                        onlyMeInTheCall =
                            list.size == 1 && list[0] == megaChatApi.myUserHandle

                        waitingForOthers = onlyMeInTheCall &&
                                MegaApplication.getChatManagement().isRequestSent(call.callId)
                    }
                }
            }
        }

        return NumParticipantsChangesResult(
            call.chatId,
            onlyMeInTheCall,
            waitingForOthers,
            isReceivedChange = true
        )
    }

    /**
     * Method to get local audio changes
     *
     * @return Flowable containing True, if audio is enabled. False, if audio is disabled.
     */
    fun getChangesFromParticipants(): Flowable<ParticipantsChangesResult> =
        Flowable.create({ emitter ->
            sharingScope.launch {
                monitorChatCallUpdatesUseCase()
                    .catch { Timber.e(it) }
                    .collectLatest { call ->
                        withContext(mainImmediateDispatcher) {
                            call.changes?.apply {
                                Timber.d("Monitor chat call updated, changes $this")
                                if (contains(ChatCallChanges.CallComposition)) {
                                    megaChatApi.getChatRoom(call.chatId)?.let { chat ->
                                        if (chat.isGroup || chat.isMeeting)
                                            emitter.checkParticipantsChanges(call)
                                    }
                                }
                            }
                        }
                    }
            }
        }, BackpressureStrategy.LATEST)

    /**
     * Control when participants join or leave and the appropriate sound should be played.
     *
     * @param call MegaChatCall
     */
    private fun FlowableEmitter<ParticipantsChangesResult>.checkParticipantsChanges(call: ChatCall) {
        if (call.status != ChatCallStatus.InProgress || call.peerIdCallCompositionChange == megaChatApi.myUserHandle || call.callCompositionChange == CallCompositionChanges.NoChange)
            return

        call.peerIdCallCompositionChange?.let { peerId ->
            when (call.callCompositionChange) {
                CallCompositionChanges.Added -> {
                    peerIdsJoined.add(peerId)
                    if (numberOfShiftsToWaitToJoin > 0) {
                        numberOfShiftsToWaitToJoin--

                        if (joinedCountDownTimer == null) {
                            joinedCountDownTimer = CustomCountDownTimer(joinedParticipantLiveData)
                            joinedCountDownTimer?.mutableLiveData?.observeForever { counterState ->
                                counterState?.let { isFinished ->
                                    if (isFinished) {
                                        joinedCountDownTimer?.stop()
                                        numberOfShiftsToWaitToJoin = MAX_NUM_OF_WAITING_SHIFTS

                                        val listOfPeers = ArrayList<Long>()
                                        listOfPeers.addAll(peerIdsJoined)
                                        val result = ParticipantsChangesResult(
                                            chatId = call.chatId,
                                            typeChange = TYPE_JOIN,
                                            listOfPeers
                                        )
                                        this.onNext(result)
                                        peerIdsJoined.clear()
                                    }
                                }
                            }

                        } else {
                            joinedCountDownTimer?.stop()
                        }

                        joinedCountDownTimer?.start(NUM_OF_SECONDS_TO_WAIT)
                    }
                }

                CallCompositionChanges.Removed -> {
                    peerIdsLeft.add(peerId)
                    if (numberOfShiftsToWaitToLeft > 0) {
                        numberOfShiftsToWaitToLeft--

                        if (leftCountDownTimer == null) {
                            leftCountDownTimer = CustomCountDownTimer(leftParticipantLiveData)
                            leftCountDownTimer?.mutableLiveData?.observeForever { counterState ->
                                counterState?.let { isFinished ->
                                    if (isFinished) {
                                        leftCountDownTimer?.stop()
                                        numberOfShiftsToWaitToLeft = MAX_NUM_OF_WAITING_SHIFTS

                                        val listOfPeers = ArrayList<Long>()
                                        listOfPeers.addAll(peerIdsLeft)
                                        val result = ParticipantsChangesResult(
                                            chatId = call.chatId,
                                            typeChange = TYPE_LEFT,
                                            listOfPeers
                                        )

                                        this.onNext(result)
                                        peerIdsLeft.clear()
                                    }
                                }
                            }
                        } else {
                            leftCountDownTimer?.stop()
                        }

                        leftCountDownTimer?.start(NUM_OF_SECONDS_TO_WAIT)
                    }
                }

                else -> {}
            }
        }
    }

    /**
     * Cancel all count downs timer
     */
    private fun removeCountDown() {
        joinedCountDownTimer?.let {
            cancelCountDown(it)
        }

        leftCountDownTimer?.let {
            cancelCountDown(it)
        }
    }

    /**
     * Cancel count down timer
     */
    private fun cancelCountDown(countDownTimer: CustomCountDownTimer) {
        countDownTimer.apply {
            stop()
        }
    }
}