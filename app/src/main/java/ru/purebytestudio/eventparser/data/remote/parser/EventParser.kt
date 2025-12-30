package ru.purebytestudio.eventparser.data.remote.parser

import ru.purebytestudio.eventparser.domain.model.Event
import ru.purebytestudio.eventparser.domain.model.EventCategory
import ru.purebytestudio.eventparser.domain.model.EventSource

/**
 * Базовый интерфейс для всех парсеров событий.
 */
interface EventParser {
    val source: EventSource

    /**
     * Парсинг событий для указанных категорий.
     */
    suspend fun parseEvents(categories: List<EventCategory>): Result<List<Event>>
}