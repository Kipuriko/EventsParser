package ru.purebytestudio.eventparser.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import ru.purebytestudio.eventparser.domain.usecase.CleanupPastEventsUseCase
import ru.purebytestudio.eventparser.domain.usecase.GetEventByIdUseCase
import ru.purebytestudio.eventparser.domain.usecase.GetFavoriteEventsSnapshotUseCase
import ru.purebytestudio.eventparser.domain.usecase.GetFavoriteEventsUseCase
import ru.purebytestudio.eventparser.domain.usecase.GetFilteredEventsUseCase
import ru.purebytestudio.eventparser.domain.usecase.ImportFavoriteEventsUseCase
import ru.purebytestudio.eventparser.domain.usecase.RefreshEventsUseCase
import ru.purebytestudio.eventparser.domain.usecase.ToggleFavoriteUseCase

val useCaseModule = module {
    factoryOf(::GetFilteredEventsUseCase)
    factoryOf(::GetFavoriteEventsUseCase)
    factoryOf(::GetFavoriteEventsSnapshotUseCase)
    factoryOf(::RefreshEventsUseCase)
    factoryOf(::ToggleFavoriteUseCase)
    factoryOf(::GetEventByIdUseCase)
    factoryOf(::ImportFavoriteEventsUseCase)
    factoryOf(::CleanupPastEventsUseCase)
}