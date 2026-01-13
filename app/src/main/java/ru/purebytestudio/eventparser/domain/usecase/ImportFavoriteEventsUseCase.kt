package ru.purebytestudio.eventparser.domain.usecase

import ru.purebytestudio.eventparser.domain.model.Event
import ru.purebytestudio.eventparser.domain.repository.EventRepository

/**
 * UseCase для импорта списка избранных событий.
 */
class ImportFavoriteEventsUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(events: List<Event>): Int = repository.importFavoriteEvents(events)
}