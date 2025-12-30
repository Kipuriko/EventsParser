package ru.purebytestudio.eventparser.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ru.purebytestudio.eventparser.MainActivity
import ru.purebytestudio.eventparser.R
import ru.purebytestudio.eventparser.domain.model.Event
import timber.log.Timber
import java.time.LocalDateTime

/**
 * Менеджер уведомлений приложения.
 *
 * Что делает:
 * - создаёт каналы уведомлений (напоминания и «новые события»);
 * - учитывает глобальное отключение уведомлений и runtime-разрешение `POST_NOTIFICATIONS` (Android 13+);
 * - формирует `PendingIntent` на [MainActivity], чтобы открыть экран деталей по `event_id`.
 *
 * Примечание: этот класс не планирует напоминания во времени — это делает
 * [ru.purebytestudio.eventparser.data.notification.EventReminderScheduler] через WorkManager.
 */
class NotificationManager(private val context: Context) {
    private val remindersChannelId = "event_reminders"
    private val newEventsChannelId = "new_events"
    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
    }

    private fun canPostNotifications(): Boolean {
        if (!notificationManager.areNotificationsEnabled()) return false

        // Android 13+ требует runtime permission POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }

        return true
    }

    private fun createNotificationChannel() {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val reminders = NotificationChannel(
            remindersChannelId,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
            enableVibration(true)
            enableLights(true)
        }

        val newEvents = NotificationChannel(
            newEventsChannelId,
            context.getString(R.string.notification_new_events_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_new_events_channel_desc)
            enableVibration(true)
            enableLights(true)
        }

        manager.createNotificationChannel(reminders)
        manager.createNotificationChannel(newEvents)
    }

    /**
     * Показывает уведомление о предстоящем событии
     */
    fun showEventReminder(
        event: Event,
        hoursBefore: Int
    ) {
        if (!canPostNotifications()) {
            Timber.d(
                "Notifications disabled or permission not granted; skip reminder for eventId=%s",
                event.id
            )
            return
        }

        val pendingIntent = mainActivityPendingIntent(
            requestCode = event.id.hashCode(),
            eventId = event.id
        )

        val hoursText = "$hoursBefore ${getHoursText(hoursBefore)}"
        val title = context.getString(
            R.string.notification_title,
            event.title
        )
        val text = context.getString(
            R.string.notification_text,
            hoursText
        )

        val bigText = buildString {
            append(event.description)
            event.location?.let { location ->
                append("\n\n📍 ")
                append(location)
            }
        }

        val notification = baseEventNotificationBuilder(
            channelId = remindersChannelId,
            pendingIntent = pendingIntent
        )
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bigText)
            )
            .build()

        try {
            notificationManager.notify(
                event.id.hashCode(),
                notification
            )
        } catch (e: SecurityException) {
            Timber.w(
                e,
                "Failed to post notification (MissingPermission) for eventId=%s",
                event.id
            )
        }
    }

    /**
     * Показывает уведомление о появлении новых событий (после фонового обновления).
     */
    fun showNewEventsFound(newEvents: List<Event>) {
        if (newEvents.isEmpty()) return

        // UI по умолчанию показывает только предстоящие события (см. GetFilteredEventsUseCase),
        // поэтому в уведомление тоже не включаем прошедшие — иначе пользователь видит "устаревшие"
        // события, которых уже нет в списке (они чистятся cleanupPastEvents()).
        val now = LocalDateTime.now()
        val upcomingEvents = newEvents.filter { event ->
            val dt = event.dateTime ?: return@filter false
            !dt.isBefore(now)
        }

        if (upcomingEvents.isEmpty()) {
            Timber.d(
                "Skip new events notification: all new events are already in the past (originalCount=%d)",
                newEvents.size
            )
            return
        }

        if (!canPostNotifications()) {
            Timber.d(
                "Notifications disabled or permission not granted; skip new events notification (count=%d)",
                newEvents.size
            )
            return
        }

        val pendingIntent = mainActivityPendingIntent(requestCode = 1001)

        val count = upcomingEvents.size
        val title = context.getString(
            R.string.notification_new_events_title,
            count
        )
        val text = context.getString(R.string.notification_new_events_text)

        val bigText = buildString {
            upcomingEvents.take(5).forEach { e ->
                append("• ")
                append(e.title)
                e.dateTime?.let { dt ->
                    append(" — ")
                    append(dt.toLocalDate().toString())
                }
                append('\n')
            }
        }.trimEnd()

        val notification = baseEventNotificationBuilder(
            channelId = newEventsChannelId,
            pendingIntent = pendingIntent
        )
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .build()

        try {
            notificationManager.notify(
                1001,
                notification
            )
        } catch (e: SecurityException) {
            Timber.w(
                e,
                "Failed to post notification (MissingPermission) for newEvents count=%d",
                count
            )
        }
    }

    private fun getHoursText(hours: Int): String = when (hours) {
        1 -> context.getString(R.string.time_hour_one)
        in 2..4 -> context.getString(R.string.time_hour_few)
        else -> context.getString(R.string.time_hour_many)
    }

    private fun baseEventNotificationBuilder(
        channelId: String,
        pendingIntent: PendingIntent
    ): NotificationCompat.Builder =
        NotificationCompat.Builder(
            context,
            channelId
        )
            // Здесь используется системная иконка, чтобы не зависеть от ресурсов mipmap.
            // При желании можно заменить на иконку приложения (но это уже «визуальная» правка).
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)

    private fun mainActivityPendingIntent(
        requestCode: Int,
        eventId: String? = null
    ): PendingIntent {
        val intent = Intent(
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            eventId?.let {
                putExtra(
                    "event_id",
                    it
                )
            }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}