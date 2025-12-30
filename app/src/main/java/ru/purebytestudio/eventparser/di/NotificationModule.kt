package ru.purebytestudio.eventparser.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ru.purebytestudio.eventparser.data.notification.EventReminderScheduler
import ru.purebytestudio.eventparser.data.notification.NotificationManager

val notificationModule = module {
    singleOf(::NotificationManager)
    singleOf(::EventReminderScheduler)
}