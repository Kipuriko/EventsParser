package ru.purebytestudio.eventparser.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.purebytestudio.eventparser.domain.model.Event
import ru.purebytestudio.eventparser.domain.model.EventCategory
import ru.purebytestudio.eventparser.domain.model.EventRefreshSummary
import java.time.LocalDateTime

interface EventRepository {
    /**
     * Получить события с применением сложных фильтров
     */
    fun getFilteredEvents(
        category: EventCategory?,
        requireOnline: Boolean,
        requireFree: Boolean,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): Flow<List<Event>>

    /**
     * Получить избранные события
     */
    fun getFavoriteEvents(): Flow<List<Event>>

    /**
     * Получить "снимок" избранных событий (одномоментно).
     * Используется для экспорта/импорта.
     */
    suspend fun getFavoriteEventsSnapshot(): List<Event>

    /**
     * Импортировать события и пометить их как избранные.
     * Возвращает количество импортированных (после фильтрации/валидации) событий.
     */
    suspend fun importFavoriteEvents(events: List<Event>): Int

    /**
     * Получить одно событие по id
     */
    suspend fun getEventById(id: String): Event?

    /**
     * Обновить события из всех источников
     */
    suspend fun refreshEvents(): Result<EventRefreshSummary>

    /**
     * Обновить события для конкретной категории
     */
    suspend fun refreshEventsForCategory(category: EventCategory): Result<EventRefreshSummary>

    /**
     * Переключить статус избранного
     */
    suspend fun toggleFavorite(eventId: String)

    /**
     * Очистка прошедших событий (кроме избранных)
     */
    suspend fun cleanupPastEvents()

    /**
     * Очистка дубликатов событий в базе данных.
     * Оставляет только лучшее событие из каждой группы дубликатов.
     *
     * @return количество удаленных дубликатов
     */
    suspend fun cleanupDuplicates(): Int

    /**
     * Перепарсить даты всех событий локально (используется для миграций)
     */
    suspend fun reparseEvents()
}