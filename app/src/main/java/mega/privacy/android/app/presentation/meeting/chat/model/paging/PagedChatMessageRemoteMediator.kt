package mega.privacy.android.app.presentation.meeting.chat.model.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.paging.FetchMessagePageResponse
import mega.privacy.android.domain.usecase.chat.message.paging.ClearChatMessagesUseCase
import mega.privacy.android.domain.usecase.chat.message.paging.FetchMessagePageUseCase
import mega.privacy.android.domain.usecase.chat.message.paging.SaveChatMessagesUseCase
import timber.log.Timber

/**
 * Paged chat message remote mediator
 *
 * @property fetchMessages
 * @property saveMessages
 * @property clearChatMessagesUseCase
 * @property chatId
 * @property coroutineScope
 */
@OptIn(ExperimentalPagingApi::class)
class PagedChatMessageRemoteMediator @AssistedInject constructor(
    private val fetchMessages: FetchMessagePageUseCase,
    private val saveMessages: SaveChatMessagesUseCase,
    private val clearChatMessagesUseCase: ClearChatMessagesUseCase,
    @Assisted private val chatId: Long,
    @Assisted private val coroutineScope: CoroutineScope,
) : RemoteMediator<Int, TypedMessage>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, TypedMessage>,
    ): MediatorResult {
        return try {
            Timber.d("Paging mediator load: loadType : $loadType")

            if (loadType == LoadType.PREPEND) {
                return MediatorResult.Success(endOfPaginationReached = true)
            }

            if (loadType == LoadType.REFRESH) {
                clearChatMessagesUseCase(chatId)
            }

            val count = when (loadType) {
                LoadType.REFRESH -> state.config.initialLoadSize
                else -> state.config.pageSize
            }

            val messages = mutableListOf<ChatMessage>()
            lateinit var response: FetchMessagePageResponse
            while (messages.size < count) {
                response = fetchMessages(chatId, coroutineScope)
                Timber.d("Paging mediator load: fetch messages response : $response")
                messages.addAll(response.messages)
                if (response.loadResponse == ChatHistoryLoadStatus.NONE) break
            }
            saveMessages(chatId = chatId, messages = messages)

            MediatorResult.Success(endOfPaginationReached = response.loadResponse == ChatHistoryLoadStatus.NONE)
        } catch (e: Exception) {
            Timber.e(e, "Paging mediator load: error")
            MediatorResult.Error(e)
        }
    }
}