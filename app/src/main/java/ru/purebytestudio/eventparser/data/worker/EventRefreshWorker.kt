package ru.purebytestudio.eventparser.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.purebytestudio.eventparser.data.notification.NotificationManager
import ru.purebytestudio.eventparser.domain.usecase.RefreshEventsUseCase
import timber.log.Timber

/**
 * Периодический worker для фонового обновления событий.
 *
 * Планируется при старте приложения (см. `EventParserApplication`) и запускается примерно раз в 6 часов
 * при наличии сети.
 *
 * Логика:
 * - вызывает [RefreshEventsUseCase];
 * - если найдено хотя бы одно новое событие — показывает уведомление через [NotificationManager];
 * - при сетевых ошибках отдаёт `Result.retry()`, чтобы WorkManager повторил попытку.
 */
class EventRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(
    appContext = context,
    params = params
), KoinComponent {
    private val refreshEventsUseCase: RefreshEventsUseCase by inject()
    private val notificationManager: NotificationManager by inject()

    override suspend fun doWork(): Result {
        return try {
            Timber.d("EventRefreshWorker: Starting background refresh")

            val result = refreshEventsUseCase()
            result.fold(
                onSuccess = { summary ->
                    Timber.d("EventRefreshWorker: Successfully refreshed events")
                    if (summary.newEventsCount > 0) {
                        notificationManager.showNewEventsFound(summary.newEvents)
                    }
                    Result.success()
                },
                onFailure = { error ->
                    Timber.e(
                        error,
                        "EventRefreshWorker: Failed to refresh events"
                    )
                    // Повторяем попытку при сетевых ошибках, иначе завершаем успешно (чтобы не зацикливаться).
                    if (error is java.io.IOException || error is retrofit2.HttpException) {
                        Result.retry()
                    } else {
                        Result.success() // Не критичная ошибка, не ретраим
                    }
                }
            )
        } catch (e: Exception) {
            Timber.e(
                e,
                "EventRefreshWorker: Unexpected error"
            )
            Result.retry()
        }
    }
}