package ru.purebytestudio.eventparser.domain.usecase

import ru.purebytestudio.eventparser.domain.repository.EventRepository

/**
 * UseCase для добавления/удаления события из избранного.
 */
class ToggleFavoriteUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(eventId: String) = repository.toggleFavorite(eventId)
}