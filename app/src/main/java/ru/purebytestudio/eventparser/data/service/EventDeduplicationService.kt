package ru.purebytestudio.eventparser.data.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.purebytestudio.eventparser.data.local.dao.EventDao
import ru.purebytestudio.eventparser.data.local.entity.EventEntity
import ru.purebytestudio.eventparser.domain.util.EventIdentityNormalizer
import ru.purebytestudio.eventparser.domain.util.TextSimilarity
import timber.log.Timber

/**
 * Сервис для дедупликации событий.
 */
class EventDeduplicationService(private val eventDao: EventDao) {
    companion object {
        /**
         * Порог сходства заголовков для определения дубликатов (от 0.0 до 1.0).
         * 0.65 означает, что заголовки должны быть похожи на 65%, чтобы считаться дубликатами.
         * Этот порог подобран экспериментально и позволяет ловить события с небольшими
         * вариациями в названии (например, "Запускаем 2026 год с правильного запуска LLM"
         * и "Запускаем год с запуска LLM").
         */
        private const val SIMILARITY_THRESHOLD = 0.65
    }
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
                // Сначала строим карту нормализованных ключей для быстрого поиска точных дубликатов
                val newEventKeys = newEvents.associate { event ->
                    EventIdentityNormalizer.fromPersisted(
                        event.title,
                        event.dateTime
                    ) to event
                }

                // Группируем новые события по дате для оптимизации
                val newEventsByDate = newEvents.groupBy { it.dateTime?.take(10) }

                existingEvents.mapNotNull { existingEvent ->
                    // Пропускаем избранные события
                    if (existingEvent.isFavorite) return@mapNotNull null

                    val existingKey = EventIdentityNormalizer.fromPersisted(
                        existingEvent.title,
                        existingEvent.dateTime
                    )
                    val existingDate = existingEvent.dateTime?.take(10)

                    // Стратегия 1: Точное совпадение нормализованных ключей
                    if (newEventKeys.containsKey(existingKey)) {
                        return@mapNotNull existingEvent.id
                    }

                    // Стратегия 2: Similarity-based matching для событий с той же датой
                    val sameDateNewEvents = newEventsByDate[existingDate] ?: emptyList()
                    for (newEvent in sameDateNewEvents) {
                        if (TextSimilarity.areSimilar(
                                existingEvent.title,
                                newEvent.title,
                                SIMILARITY_THRESHOLD
                            )
                        ) {
                            Timber.d(
                                "Найден похожий дубликат: '${existingEvent.title}' ≈ '${newEvent.title}'"
                            )
                            return@mapNotNull existingEvent.id
                        }
                    }

                    null
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
            // Создаём карту нормализованных ключей избранных событий
            val favoriteKeyToEvent = favoriteEvents.associateBy {
                EventIdentityNormalizer.fromPersisted(
                    it.title,
                    it.dateTime
                )
            }

            // Группируем избранные события по датам для оптимизации
            val favoritesByDate = favoriteEvents.groupBy { it.dateTime?.take(10) }

            newEvents.filter { newEvent ->
                val newEventKey = EventIdentityNormalizer.fromPersisted(
                    newEvent.title,
                    newEvent.dateTime
                )
                val newEventDate = newEvent.dateTime?.take(10)

                // Стратегия 1: Проверяем точное совпадение ключа
                if (favoriteKeyToEvent.containsKey(newEventKey)) {
                    Timber.d(
                        "Пропуск нового события '${newEvent.title}' - точный дубликат избранного"
                    )
                    return@filter false
                }

                // Стратегия 2: Проверяем сходство с избранными событиями той же даты
                val sameDateFavorites = favoritesByDate[newEventDate] ?: emptyList()
                for (favorite in sameDateFavorites) {
                    if (TextSimilarity.areSimilar(
                            newEvent.title,
                            favorite.title,
                            SIMILARITY_THRESHOLD
                        )
                    ) {
                        Timber.d(
                            "Пропуск нового события '${newEvent.title}' - похоже на избранное '${favorite.title}'"
                        )
                        return@filter false
                    }
                }

                true // Оставляем событие, если оно не дубликат
            }
        }
    }

    /**
     * Удаляет все существующие дубликаты из базы данных.
     * Оставляет только одно событие из группы дубликатов (предпочтительно избранное или с изображением).
     *
     * @return количество удаленных дубликатов
     */
    suspend fun cleanupExistingDuplicates(): Int {
        return try {
            // Получаем все события
            val allEvents = eventDao.getAllEvents()

            if (allEvents.isEmpty()) return 0

            withContext(Dispatchers.Default) {
                // Группируем события по дате для оптимизации
                val eventsByDate = allEvents.groupBy { it.dateTime?.take(10) }

                val idsToDelete = mutableListOf<String>()

                // Для каждой даты ищем дубликаты
                for ((_, eventsOnDate) in eventsByDate) {
                    if (eventsOnDate.size < 2) continue // Если событие одно, пропускаем

                    // Группируем по нормализованному ключу
                    val groupsByKey = eventsOnDate.groupBy { event ->
                        EventIdentityNormalizer.fromPersisted(
                            event.title,
                            event.dateTime
                        )
                    }

                    // Для каждой группы с одинаковым ключом оставляем лучшее
                    for ((_, group) in groupsByKey) {
                        if (group.size > 1) {
                            val toKeep = selectBestEvent(group)
                            val toDelete = group.filter { it.id != toKeep.id }
                            idsToDelete.addAll(toDelete.map { it.id })

                            Timber.d("Группа дубликатов (точное совпадение ключа): ${group.size} событий, оставляем: '${toKeep.title}'")
                        }
                    }

                    // Теперь ищем похожие события (similarity-based)
                    val remainingEvents = eventsOnDate.filter { it.id !in idsToDelete }

                    for (i in remainingEvents.indices) {
                        val event1 = remainingEvents[i]
                        if (event1.id in idsToDelete) continue

                        for (j in (i + 1) until remainingEvents.size) {
                            val event2 = remainingEvents[j]
                            if (event2.id in idsToDelete) continue

                            if (TextSimilarity.areSimilar(
                                    event1.title,
                                    event2.title,
                                    SIMILARITY_THRESHOLD
                                )
                            ) {
                                // Найдены похожие события
                                val toKeep = selectBestEvent(
                                    listOf(
                                        event1,
                                        event2
                                    )
                                )
                                val toDelete = if (toKeep.id == event1.id) event2 else event1

                                idsToDelete.add(toDelete.id)

                                Timber.d("Найдены похожие дубликаты: '${event1.title}' ≈ '${event2.title}', оставляем: '${toKeep.title}'")
                            }
                        }
                    }
                }

                // Удаляем все найденные дубликаты
                if (idsToDelete.isNotEmpty()) {
                    eventDao.deleteEventsByIds(idsToDelete)
                    Timber.i("Очистка дубликатов: удалено ${idsToDelete.size} событий")
                }

                idsToDelete.size
            }
        } catch (e: Exception) {
            Timber.e(
                e,
                "Ошибка при очистке существующих дубликатов"
            )
            0
        }
    }

    /**
     * Выбирает "лучшее" событие из группы дубликатов.
     * Приоритет: избранное > с изображением > более длинное описание
     */
    private fun selectBestEvent(events: List<EventEntity>): EventEntity {
        // Сначала проверяем избранные
        val favorite = events.firstOrNull { it.isFavorite }
        if (favorite != null) return favorite

        // Затем предпочитаем с изображением
        val withImage = events.filter { it.imageUrl != null }
        if (withImage.isNotEmpty()) {
            return withImage.maxByOrNull { it.description.length } ?: withImage.first()
        }

        // Иначе берем с самым длинным описанием
        return events.maxByOrNull { it.description.length } ?: events.first()
    }
}