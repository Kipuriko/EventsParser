package ru.purebytestudio.eventparser.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.purebytestudio.eventparser.data.local.preferences.AppPreferences
import ru.purebytestudio.eventparser.data.repository.EventRepositoryImpl
import ru.purebytestudio.eventparser.data.repository.UserPreferencesRepositoryImpl
import ru.purebytestudio.eventparser.domain.repository.EventRepository
import ru.purebytestudio.eventparser.domain.repository.UserPreferencesRepository

/**
 * Модуль репозиториев (реализация контрактов Domain-слоя).
 *
 * Здесь регистрируются:
 * - [EventRepository] → [EventRepositoryImpl] (Room + Remote парсеры + дедупликация),
 * - DataStore preferences ([AppPreferences]),
 * - [UserPreferencesRepository] → [UserPreferencesRepositoryImpl].
 */
val repositoryModule = module {
    singleOf(::EventRepositoryImpl) bind EventRepository::class
    singleOf(::AppPreferences)
    singleOf(::UserPreferencesRepositoryImpl) bind UserPreferencesRepository::class
}