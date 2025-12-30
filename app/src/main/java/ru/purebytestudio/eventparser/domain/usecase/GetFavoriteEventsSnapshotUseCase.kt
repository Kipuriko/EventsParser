package ru.purebytestudio.eventparser.domain.usecase

import ru.purebytestudio.eventparser.domain.model.Event
import ru.purebytestudio.eventparser.domain.repository.EventRepository

/**
 * UseCase для получения одномоментного списка избранного (для экспорта/планирования).
 */
class GetFavoriteEventsSnapshotUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(): List<Event> = repository.getFavoriteEventsSnapshot()
}
