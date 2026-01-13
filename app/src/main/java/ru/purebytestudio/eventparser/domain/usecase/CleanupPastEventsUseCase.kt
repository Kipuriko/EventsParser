package ru.purebytestudio.eventparser.domain.usecase

import ru.purebytestudio.eventparser.domain.repository.EventRepository

/**
 * UseCase для очистки прошедших событий.
 */
class CleanupPastEventsUseCase(private val repository: EventRepository) {
    /**
     * Удалить прошедшие события (кроме избранных)
     */
    suspend operator fun invoke() = repository.cleanupPastEvents()
}