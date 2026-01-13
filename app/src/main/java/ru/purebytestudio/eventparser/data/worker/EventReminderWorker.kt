package ru.purebytestudio.eventparser.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.purebytestudio.eventparser.data.notification.NotificationManager
import ru.purebytestudio.eventparser.domain.repository.EventRepository
import ru.purebytestudio.eventparser.platform.TimeProvider
import timber.log.Timber

/**
 * One-time worker, который показывает напоминание о конкретном событии (за N часов до старта).
 * Планируется при добавлении события в избранное.
 */
class EventReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(
    appContext = context,
    params = params
), KoinComponent {
    private val repository: EventRepository by inject()
    private val notificationManager: NotificationManager by inject()
    private val timeProvider: TimeProvider by inject()

    override suspend fun doWork(): Result {
        val eventId = inputData.getString(KEY_EVENT_ID).orEmpty()
        val hoursBefore = inputData.getInt(
            KEY_HOURS_BEFORE,
            -1
        )

        if (eventId.isBlank() || hoursBefore < 0) {
            Timber.w(
                "EventReminderWorker: invalid input (eventId=%s, hoursBefore=%d)",
                eventId,
                hoursBefore
            )
            return Result.success()
        }

        return try {
            val event = repository.getEventById(eventId)
            if (event == null) {
                Timber.d(
                    "EventReminderWorker: event not found, skip (eventId=%s)",
                    eventId
                )
                return Result.success()
            }

            if (!event.isFavorite) {
                Timber.d(
                    "EventReminderWorker: event not favorite anymore, skip (eventId=%s)",
                    eventId
                )
                return Result.success()
            }

            val dt = event.dateTime
            if (dt == null) {
                Timber.d(
                    "EventReminderWorker: event has no dateTime, skip (eventId=%s)",
                    eventId
                )
                return Result.success()
            }

            // Дополнительная защита от устаревших задач: если событие уже началось — не уведомляем.
            if (!dt.isAfter(timeProvider.now())) {
                Timber.d(
                    "EventReminderWorker: event already started/past, skip (eventId=%s)",
                    eventId
                )
                return Result.success()
            }

            notificationManager.showEventReminder(
                event = event,
                hoursBefore = hoursBefore
            )
            Result.success()
        } catch (e: Exception) {
            Timber.e(
                e,
                "EventReminderWorker: failed for eventId=%s",
                eventId
            )
            Result.retry()
        }
    }

    companion object {
        const val KEY_EVENT_ID: String = "event_id"
        const val KEY_HOURS_BEFORE: String = "hours_before"
    }
}