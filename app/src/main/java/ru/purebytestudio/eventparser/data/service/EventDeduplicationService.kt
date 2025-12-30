package ru.purebytestudio.eventparser.data.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.purebytestudio.eventparser.data.local.dao.EventDao
import ru.purebytestudio.eventparser.data.local.entity.EventEntity
import ru.purebytestudio.eventparser.domain.util.EventIdentityNormalizer
import timber.log.Timber

/**
 * Сервис для дедупликации событий.
 */
class EventDeduplicationService(private val eventDao: EventDao) {
    /**
     * Удаляет дубликаты из базы данных перед вставкой новых.
     * Оставляет события, помеченные как избранные.
     */
    suspend fun removeDuplicatesFromDatabase(newEvents: List<EventEntity>) {
        if (newEvents.isEmpty()) return

        try {
            // Оптимизация: проверяем диапазон дат
            val dates = newEvents.mapNotNull { it.dateTime?.take(10) }
            if (dates.isEmpty()) return

            val minDate = dates.minOrNull() ?: return
            val maxDate = dates.maxOrNull() ?: return

            // Используем индекс по dateTime
            val existingEvents = eventDao.getEventsBetweenDates(
                startDate = "${minDate}T00:00:00",
                endDate = "${maxDate}T23:59:59"
            )

            if (existingEvents.isEmpty()) return

            // Тяжелые вычисления хешей выносим в фоновый поток
            val duplicateIds = withContext(Dispatchers.Default) {
                val newEventKeys = newEvents.associate { event ->
                    EventIdentityNormalizer.fromPersisted(
                        event.title,
                        event.dateTime
                    ) to event.id
                }

                existingEvents.mapNotNull { existingEvent ->
                    val key = EventIdentityNormalizer.fromPersisted(
                        existingEvent.title,
                        existingEvent.dateTime
                    )
                    // Если есть новое событие с таким же ключом и старое не в избранном -> удаляем старое
                    if (newEventKeys.containsKey(key) && !existingEvent.isFavorite) {
                        existingEvent.id
                    } else null
                }
            }

            if (duplicateIds.isNotEmpty()) {
                eventDao.deleteEventsByIds(duplicateIds)
                Timber.d("Удалено ${duplicateIds.size} дублирующихся событий из базы данных")
            }
        } catch (e: Exception) {
            Timber.e(
                e,
                "Не удалось удалить дубликаты из базы данных"
            )
        }
    }

    /**
     * Возвращает список событий для вставки, исключая те,
     * полные дубликаты которых уже есть в БД и помечены как "Избранное".
     */
    suspend fun filterOutDuplicatesOfFavorites(newEvents: List<EventEntity>): List<EventEntity> {
        if (newEvents.isEmpty()) return newEvents

        val dates = newEvents.mapNotNull { it.dateTime?.take(10) }
        if (dates.isEmpty()) return newEvents

        val minDate = dates.minOrNull() ?: return newEvents
        val maxDate = dates.maxOrNull() ?: return newEvents

        val candidates = eventDao.getEventsBetweenDates(
            startDate = "${minDate}T00:00:00",
            endDate = "${maxDate}T23:59:59"
        )

        val favoriteEvents = candidates.filter { it.isFavorite }

        if (favoriteEvents.isEmpty()) return newEvents

        return withContext(Dispatchers.Default) {
            val favoriteKeys = favoriteEvents.map {
                EventIdentityNormalizer.fromPersisted(
                    it.title,
                    it.dateTime
                )
            }.toSet()

            newEvents.filter { event ->
                val key = EventIdentityNormalizer.fromPersisted(
                    event.title,
                    event.dateTime
                )
                !favoriteKeys.contains(key)
            }
        }
    }
}