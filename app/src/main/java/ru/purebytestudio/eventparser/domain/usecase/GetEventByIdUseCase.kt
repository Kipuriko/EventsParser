package ru.purebytestudio.eventparser.domain.usecase

import ru.purebytestudio.eventparser.domain.model.Event
import ru.purebytestudio.eventparser.domain.repository.EventRepository

/**
 * UseCase для получения детальной информации о событии по его ID.
 */
class GetEventByIdUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(id: String): Event? = repository.getEventById(id)
}