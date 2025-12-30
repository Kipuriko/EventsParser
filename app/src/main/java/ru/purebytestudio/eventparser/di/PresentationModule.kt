package ru.purebytestudio.eventparser.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ru.purebytestudio.eventparser.presentation.detail.EventDetailViewModel
import ru.purebytestudio.eventparser.presentation.events.EventsViewModel
import ru.purebytestudio.eventparser.presentation.favorites.FavoritesViewModel
import ru.purebytestudio.eventparser.presentation.settings.SettingsViewModel

/**
 * Модуль Presentation-слоя: регистрация ViewModel’ей.
 *
 * Важно: ViewModel получают зависимости только через конструктор (Koin создаёт их автоматически).
 */
val viewModelModule = module {
    viewModelOf(::EventsViewModel)
    viewModelOf(::FavoritesViewModel)
    viewModelOf(::EventDetailViewModel)
    viewModelOf(::SettingsViewModel)
}

