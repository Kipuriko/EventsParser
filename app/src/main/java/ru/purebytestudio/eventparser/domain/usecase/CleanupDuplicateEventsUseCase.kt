package ru.purebytestudio.eventparser.domain.usecase

import ru.purebytestudio.eventparser.domain.repository.EventRepository

/**
 * UseCase для очистки существующих дубликатов событий в базе данных.
 */
class CleanupDuplicateEventsUseCase(private val repository: EventRepository) {
    /**
     * Удаляет все дубликаты из базы данных.
     * Оставляет только лучшее событие из каждой группы дубликатов.
     *
     * @return количество удаленных дубликатов
     */
    suspend operator fun invoke(): Int = repository.cleanupDuplicates()
}