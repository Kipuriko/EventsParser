package ru.purebytestudio.eventparser.data.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import ru.purebytestudio.eventparser.data.worker.EventReminderWorker
import ru.purebytestudio.eventparser.domain.model.Event
import ru.purebytestudio.eventparser.platform.TimeProvider
import timber.log.Timber
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Планирует точечные напоминания (WorkManager one-time) для избранных событий.
 *
 * Вместо периодического опроса мы создаём задачи на конкретные моменты:
 * - за 24 часа до начала
 * - за 1 час до начала
 */
class EventReminderScheduler(
    context: Context,
    private val timeProvider: TimeProvider
) {
    private val workManager: WorkManager = WorkManager.getInstance(context)

    fun scheduleForEvent(event: Event) {
        if (!event.isFavorite) return
        val start = event.dateTime ?: return

        schedule(
            eventId = event.id,
            startEpochMillis = start.toEpochMillis(),
            hoursBefore = 24
        )
        schedule(
            eventId = event.id,
            startEpochMillis = start.toEpochMillis(),
            hoursBefore = 1
        )
    }

    fun cancelForEvent(eventId: String) {
        cancel(
            eventId,
            24
        )
        cancel(
            eventId,
            1
        )
    }

    private fun cancel(
        eventId: String,
        hoursBefore: Int
    ) {
        workManager.cancelUniqueWork(
            workName(
                eventId,
                hoursBefore
            )
        )
    }

    private fun schedule(
        eventId: String,
        startEpochMillis: Long,
        hoursBefore: Int
    ) {
        val nowMillis = timeProvider.now().toEpochMillis()
        val triggerAtMillis = startEpochMillis - TimeUnit.HOURS.toMillis(hoursBefore.toLong())

        // Если момент уже прошёл — не планируем.
        if (triggerAtMillis <= nowMillis) {
            Timber.d(
                "EventReminderScheduler: skip schedule (eventId=%s, hoursBefore=%d, triggerAt=%d, now=%d)",
                eventId,
                hoursBefore,
                triggerAtMillis,
                nowMillis
            )
            cancel(
                eventId,
                hoursBefore
            ) // на всякий случай уберём старые задачи
            return
        }

        val delayMillis = triggerAtMillis - nowMillis
        val request = OneTimeWorkRequestBuilder<EventReminderWorker>()
            .setInitialDelay(
                delayMillis,
                TimeUnit.MILLISECONDS
            )
            .setInputData(
                workDataOf(
                    EventReminderWorker.KEY_EVENT_ID to eventId,
                    EventReminderWorker.KEY_HOURS_BEFORE to hoursBefore
                )
            )
            .addTag(tagForEvent(eventId))
            .build()

        workManager.enqueueUniqueWork(
            workName(
                eventId,
                hoursBefore
            ),
            ExistingWorkPolicy.REPLACE,
            request
        )

        Timber.d(
            message = "EventReminderScheduler: scheduled (eventId=%s, hoursBefore=%d, in=%s)",
            eventId,
            hoursBefore,
            Duration.ofMillis(delayMillis)
        )
    }

    private fun workName(
        eventId: String,
        hoursBefore: Int
    ): String =
        "event_reminder_${eventId}_${hoursBefore}h"

    private fun tagForEvent(eventId: String): String = "event_reminder_$eventId"
}

private fun java.time.LocalDateTime.toEpochMillis(): Long =
    atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
