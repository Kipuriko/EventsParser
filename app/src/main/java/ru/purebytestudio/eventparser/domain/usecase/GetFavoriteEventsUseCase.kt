package ru.purebytestudio.eventparser.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.purebytestudio.eventparser.domain.model.Event
import ru.purebytestudio.eventparser.domain.repository.EventRepository

/**
 * UseCase для получения списка избранных событий.
 */
class GetFavoriteEventsUseCase(private val repository: EventRepository) {
    operator fun invoke(): Flow<List<Event>> = repository.getFavoriteEvents()
}
