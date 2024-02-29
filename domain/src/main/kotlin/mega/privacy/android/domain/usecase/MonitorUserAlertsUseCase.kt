package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.runningFold
import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Monitor user alerts use-case
 *
 * @property notificationsRepository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MonitorUserAlertsUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {
    operator fun invoke() = flow {
        val flow = notificationsRepository.monitorUserAlerts().runningFold(
            initial = notificationsRepository.getUserAlerts(),
            operation = { current, updates ->
                (updates.filterNot { it.isOwnChange } + current).distinctBy { it.id }
            }
        ).distinctUntilChanged()
            .mapLatest { list -> list.sortedByDescending { it.createdTime } }
        emitAll(flow)
    }
}