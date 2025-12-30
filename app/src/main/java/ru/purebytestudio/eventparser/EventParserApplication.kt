package ru.purebytestudio.eventparser

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import ru.purebytestudio.eventparser.data.crash.CrashHandler
import ru.purebytestudio.eventparser.data.notification.EventReminderScheduler
import ru.purebytestudio.eventparser.data.worker.EventRefreshWorker
import ru.purebytestudio.eventparser.di.appModules
import ru.purebytestudio.eventparser.domain.repository.EventRepository
import ru.purebytestudio.eventparser.domain.usecase.CleanupPastEventsUseCase
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Основной класс приложения.
 * Инициализирует DI (Koin), логирование (Timber), CrashHandler и планирует периодические задачи (WorkManager).
 */
class EventParserApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        CrashHandler(this)

        val isDebug =
            (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(object : Timber.Tree() {
                override fun log(
                    priority: Int,
                    tag: String?,
                    message: String,
                    t: Throwable?
                ) {
                    // В продакшене логируем только ошибки и предупреждения
                    if (priority >= android.util.Log.WARN) {
                        // Лог в системный лог
                        android.util.Log.println(
                            priority,
                            tag ?: "EventParser",
                            message
                        )
                        // Throwable будет обработан CrashHandler
                        t?.let {
                            android.util.Log.println(
                                priority,
                                tag ?: "EventParser",
                                android.util.Log.getStackTraceString(it)
                            )
                        }
                    }
                }
            })
        }

        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@EventParserApplication)
            modules(appModules)
        }

        applicationScope.launch {
            try {
                val cleanupUseCase = GlobalContext.get().get<CleanupPastEventsUseCase>()
                cleanupUseCase()
                Timber.d("Очищены прошедшие события при старте приложения")
            } catch (e: Exception) {
                Timber.e(
                    e,
                    "Не удалось очистить события при старте приложения"
                )
            }
        }

        // После обновления приложения важно восстановить план напоминаний для уже добавленных избранных.
        applicationScope.launch {
            try {
                val repository = GlobalContext.get().get<EventRepository>()
                val scheduler = GlobalContext.get().get<EventReminderScheduler>()
                val favorites = repository.getFavoriteEventsSnapshot()
                favorites.forEach { scheduler.scheduleForEvent(it) }
                Timber.d(
                    "Запланированы напоминания для избранных при старте (count=%d)",
                    favorites.size
                )
            } catch (e: Exception) {
                Timber.e(
                    e,
                    "Не удалось запланировать напоминания при старте"
                )
            }
        }

        setupPeriodicWork()
    }

    private fun setupPeriodicWork() {
        // На старых версиях приложения мог быть запланирован periodic-work для уведомлений.
        // Сейчас напоминания планируются точечно (one-time) при добавлении в избранное.
        WorkManager.getInstance(this).cancelUniqueWork("event_notifications")

        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false).build()

        // Периодическое обновление событий (каждые 6 часов)
        val refreshWork = PeriodicWorkRequestBuilder<EventRefreshWorker>(
            repeatInterval = 6,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            uniqueWorkName = "event_refresh",
            existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP,
            request = refreshWork
        )

        Timber.d("Периодические задачи запланированы")
    }
}