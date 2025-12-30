package ru.purebytestudio.eventparser.domain.usecase

import ru.purebytestudio.eventparser.domain.model.EventCategory
import ru.purebytestudio.eventparser.domain.model.EventRefreshSummary
import ru.purebytestudio.eventparser.domain.repository.EventRepository

/**
 * UseCase для обновления событий с сервера/источника.
 */
class RefreshEventsUseCase(private val repository: EventRepository) {
    /**
     * Обновить все события
     */
    suspend operator fun invoke(): Result<EventRefreshSummary> = repository.refreshEvents()

    /**
     * Обновить события определенной категории
     */
    suspend operator fun invoke(category: EventCategory): Result<EventRefreshSummary> =
        repository.refreshEventsForCategory(category)
}