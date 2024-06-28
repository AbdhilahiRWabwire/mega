package mega.privacy.android.app.usecase.call

import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase.Result.OnChatListItemUpdate
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatCall.CALL_STATUS_CONNECTING
import nz.mega.sdk.MegaChatCall.CALL_STATUS_DESTROYED
import nz.mega.sdk.MegaChatCall.CALL_STATUS_INITIAL
import nz.mega.sdk.MegaChatCall.CALL_STATUS_IN_PROGRESS
import nz.mega.sdk.MegaChatCall.CALL_STATUS_JOINING
import nz.mega.sdk.MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatRoom
import timber.log.Timber
import javax.inject.Inject

/**
 * Main use case to get a Mega Chat Call and get information about existing calls
 *
 * @property getChatChangesUseCase      Use case required to get chat request updates
 * @property megaChatApi   Mega Chat API needed to get call information.
 */
class GetCallUseCase @Inject constructor(
    private val getChatChangesUseCase: GetChatChangesUseCase,
    private val megaChatApi: MegaChatApiAndroid,
) {

    /**
     * Get the MegaChatCall from a chatRoom ID
     *
     * @param chatRoomId Chat ID
     * @return MegaChatCall
     */
    fun getMegaChatCall(chatRoomId: Long): Single<MegaChatCall> =
        Single.fromCallable {
            megaChatApi.getChatCall(chatRoomId)
        }

    /**
     * Get a chat id of another call in progress or on hold
     *
     * @param currentChatId Chat ID of the current call
     * @return Chat ID of the another call or -1 if no exists
     */
    fun getChatIdOfAnotherCallInProgress(currentChatId: Long): Single<Long> =
        Single.fromCallable {
            var result: Long = MEGACHAT_INVALID_HANDLE
            val calls = getCallsInProgressAndOnHold()

            for (call in calls) {
                if (call.chatid != currentChatId) {
                    result = call.chatid
                    break
                }
            }

            result
        }

    /**
     * Method to check if there is another call in progress or on hold
     *
     * @param currentChatId Chat ID of the current call
     * @return Chat ID of the another call or -1 if no exists
     */
    fun checkAnotherCall(currentChatId: Long): Flowable<Long> =
        Flowable.create({ emitter ->
            emitter.onNext(getChatIdOfAnotherCallInProgress(currentChatId).blockingGet())

            val callStatusObserver = Observer<MegaChatCall> { call ->
                when (call.status) {
                    CALL_STATUS_DESTROYED -> {
                        emitter.onNext(getChatIdOfAnotherCallInProgress(currentChatId).blockingGet())
                    }
                }
            }

            LiveEventBus.get(EventConstants.EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
                .observeForever(callStatusObserver)

            emitter.setCancellable {
                LiveEventBus.get(EventConstants.EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
                    .removeObserver(callStatusObserver)
            }
        }, BackpressureStrategy.LATEST)

    /**
     * Method to get the destroyed call
     *
     * @return Chat ID of the call
     */
    fun getCallEnded(): Flowable<Long> =
        Flowable.create({ emitter ->
            val callStatusObserver = Observer<MegaChatCall> { call ->
                when (call.status) {
                    CALL_STATUS_TERMINATING_USER_PARTICIPATION,
                    CALL_STATUS_DESTROYED,
                    -> {
                        emitter.onNext(call.chatid)
                    }
                }
            }

            LiveEventBus.get(EventConstants.EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
                .observeForever(callStatusObserver)

            emitter.setCancellable {
                LiveEventBus.get(EventConstants.EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
                    .removeObserver(callStatusObserver)
            }
        }, BackpressureStrategy.LATEST)

    /**
     * Method to get the all calls with status connecting, joining and in progress
     *
     * @return List of ongoing calls
     */
    fun getCallsInProgressAndOnHold(): ArrayList<MegaChatCall> {
        val listCalls = ArrayList<MegaChatCall>()

        megaChatApi.chatCalls?.let {
            for (i in 0 until it.size()) {
                val chatId = it[i]
                megaChatApi.getChatCall(chatId)?.let { call ->
                    if (call.status == CALL_STATUS_CONNECTING ||
                        call.status == CALL_STATUS_JOINING ||
                        call.status == CALL_STATUS_IN_PROGRESS
                    ) {
                        listCalls.add(call)
                    }
                }
            }
        }

        return listCalls
    }

    /**
     * Method to get if the call associated a chat ID exists and I'm the moderator of the call
     *
     * @return Flowable containing True, if there call exists and I'm moderator. False, otherwise
     */
    fun isThereACallAndIAmModerator(chatId: Long): Flowable<Boolean> =
        Flowable.create({ emitter ->
            val disposable = CompositeDisposable()
            megaChatApi.getChatCall(chatId)?.let { call ->
                megaChatApi.getChatRoom(chatId)?.let { chat ->
                    emitter.checkCallAndPrivileges(call, chat)
                }
            }

            val callStatusObserver = Observer<MegaChatCall> { call ->
                if (chatId == call.chatid) {
                    val chat = megaChatApi.getChatRoom(chatId)
                    if (chat == null) {
                        emitter.onNext(false)
                    } else {
                        emitter.checkCallAndPrivileges(call, chat)
                    }
                }
            }

            getChatChangesUseCase.get()
                .filter { it is OnChatListItemUpdate }
                .subscribeBy(
                    onNext = { change ->
                        if (emitter.isCancelled) return@subscribeBy
                        (change as OnChatListItemUpdate).item?.let { item ->
                            if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_OWN_PRIV)) {
                                if (chatId == item.chatId) {
                                    val call = megaChatApi.getChatCall(chatId)
                                    val chat = megaChatApi.getChatRoom(chatId)
                                    if (call == null || chat == null) {
                                        emitter.onNext(false)
                                    } else {
                                        emitter.checkCallAndPrivileges(call, chat)
                                    }
                                }
                            }
                        }
                    },
                    onError = { error ->
                        Timber.e(error.stackTraceToString())
                    }
                ).addTo(disposable)

            LiveEventBus.get(EventConstants.EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
                .observeForever(callStatusObserver)

            emitter.setCancellable {
                disposable.clear()
                LiveEventBus.get(EventConstants.EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
                    .removeObserver(callStatusObserver)
            }

        }, BackpressureStrategy.LATEST)

    /**
     * Check call status and own privileges
     *
     * @return True, if the status of the call is other than:CALL_STATUS_DESTROYED, CALL_STATUS_INITIAL, CALL_STATUS_JOINING and own privilege is moderator. False, otherwise.
     */
    private fun FlowableEmitter<Boolean>.checkCallAndPrivileges(
        call: MegaChatCall,
        chat: MegaChatRoom,
    ) {
        val result: Boolean =
            call.status != CALL_STATUS_INITIAL && call.status != CALL_STATUS_TERMINATING_USER_PARTICIPATION && call.status != CALL_STATUS_DESTROYED && chat.ownPrivilege == MegaChatRoom.PRIV_MODERATOR

        this.onNext(result)
    }
}
