package ru.purebytestudio.eventparser.di

/**
 * Единая точка сборки модулей Koin.
 *
 * Почему это отдельный файл:
 * - удобно видеть «состав приложения» в одном месте;
 * - [ru.purebytestudio.eventparser.EventParserApplication] подключает ровно этот список.
 *
 * Важно: порядок модулей не критичен для Koin, но мы держим его «снизу вверх»:
 * инфраструктура → репозитории → use case → presentation.
 */
val appModules = listOf(
    databaseModule,
    networkModule,
    repositoryModule,
    useCaseModule,
    viewModelModule,
    notificationModule,
    platformModule,
    serializationModule,
    exportModule
)