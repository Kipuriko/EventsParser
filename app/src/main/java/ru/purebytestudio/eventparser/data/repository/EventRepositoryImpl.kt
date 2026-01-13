package ru.purebytestudio.eventparser.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.purebytestudio.eventparser.data.local.dao.EventDao
import ru.purebytestudio.eventparser.data.local.entity.EventEntity
import ru.purebytestudio.eventparser.data.notification.EventReminderScheduler
import ru.purebytestudio.eventparser.data.remote.EventParserAggregator
import ru.purebytestudio.eventparser.data.service.EventDeduplicationService
import ru.purebytestudio.eventparser.domain.model.Event
import ru.purebytestudio.eventparser.domain.model.EventCategory
import ru.purebytestudio.eventparser.domain.model.EventRefreshSummary
import ru.purebytestudio.eventparser.domain.repository.EventRepository
import ru.purebytestudio.eventparser.domain.util.EventIdentityNormalizer
import ru.purebytestudio.eventparser.platform.ErrorMessageProvider
import ru.purebytestudio.eventparser.platform.NetworkStatusProvider
import ru.purebytestudio.eventparser.platform.RetryPolicy
import timber.log.Timber
import java.time.LocalDateTime

/**
 * Реализация [EventRepository] для приложения.
 *
 * За что отвечает класс:
 * - выдаёт события из Room (включая фильтрацию/избранное) как `Flow` для реактивного UI;
 * - обновляет события из удалённых источников через [EventParserAggregator];
 * - перед записью в БД выполняет дедупликацию:
 *   - не вставляет новые события, если они дублируют уже существующие **избранные**
 *     (чтобы «не потерять» избранное при обновлениях);
 *   - чистит старые дубли, которые не в избранном (чтобы база не разрасталась);
 * - при изменении избранного планирует/отменяет напоминания через [EventReminderScheduler].
 *
 * Важно: репозиторий специально формирует [EventRefreshSummary] **до** записи в БД, чтобы
 * корректно определить «новые» события относительно текущего состояния базы.
 */
class EventRepositoryImpl(
    private val eventDao: EventDao,
    private val parserAggregator: EventParserAggregator,
    private val networkStatusProvider: NetworkStatusProvider,
    private val retryPolicy: RetryPolicy,
    private val errorMessageProvider: ErrorMessageProvider,
    private val eventDeduplicationService: EventDeduplicationService,
    private val reminderScheduler: EventReminderScheduler
) : EventRepository {
    private val defaultCategories = EventCategory.entries

    override fun getFilteredEvents(
        category: EventCategory?,
        requireOnline: Boolean,
        requireFree: Boolean,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): Flow<List<Event>> {
        return eventDao.getFilteredEvents(
            category = category?.name,
            requireOnline = requireOnline,
            requireFree = requireFree,
            startDate = startDate?.toString(),
            endDate = endDate?.toString()
        ).map { entities -> entities.toDomainList() }
    }

    override fun getFavoriteEvents(): Flow<List<Event>> {
        return eventDao.getFavoriteEvents().map { entities -> entities.toDomainList() }
    }

    override suspend fun getFavoriteEventsSnapshot(): List<Event> {
        return eventDao.getFavoriteEventsList().toDomainList()
    }

    override suspend fun importFavoriteEvents(events: List<Event>): Int {
        if (events.isEmpty()) return 0

        // Импорт — это "upsert" событий и пометка их как избранных.
        // Параллельно планируем напоминания (если есть дата/время).
        val prepared = events
            .filter { it.id.isNotBlank() && it.title.isNotBlank() && it.url.startsWith("http") }
            .map { it.copy(isFavorite = true) }

        if (prepared.isEmpty()) return 0

        eventDao.insertEvents(prepared.map { EventEntity.fromDomain(it) })
        prepared.forEach { reminderScheduler.scheduleForEvent(it) }
        return prepared.size
    }

    override suspend fun getEventById(id: String): Event? {
        return eventDao.getEventById(id)?.toDomain()
    }

    override suspend fun refreshEvents(): Result<EventRefreshSummary> {
        return refreshEventsInternal(defaultCategories)
    }

    override suspend fun refreshEventsForCategory(category: EventCategory): Result<EventRefreshSummary> {
        return refreshEventsInternal(listOf(category))
    }

    private suspend fun refreshEventsInternal(categories: List<EventCategory>): Result<EventRefreshSummary> {
        if (!networkStatusProvider.isOnline()) {
            return Result.failure(Exception(errorMessageProvider.noInternet()))
        }

        return try {
            val events = fetchRemoteEvents(categories)

            // Фильтрация и подготовка к вставке в фоновом потоке
            val prepared = prepareForInsert(events)

            // Фильтруем новые события, которые дублируют уже существующие ИЗБРАННЫЕ.
            val eventsToReallyInsert =
                eventDeduplicationService.filterOutDuplicatesOfFavorites(prepared.entities)

            // Удаляем старые дубликаты (которые НЕ в избранном)
            eventDeduplicationService.removeDuplicatesFromDatabase(eventsToReallyInsert)

            eventDao.insertEvents(eventsToReallyInsert)
            cleanupPastEvents()

            Result.success(prepared.summary)
        } catch (e: Exception) {
            Timber.e(
                e,
                "Не удалось обновить события для категорий: $categories"
            )
            val message = errorMessageProvider.fromThrowable(e)
            Result.failure(Exception(message))
        }
    }

    private suspend fun fetchRemoteEvents(categories: List<EventCategory>): List<Event> {
        return retryPolicy.execute {
            parserAggregator.parseAllEvents(categories)
        }
    }

    private suspend fun prepareForInsert(events: List<Event>): Prepared =
        withContext(Dispatchers.Default) {
            val validEvents = filterValidEvents(events)
            val summary = buildRefreshSummary(validEvents)

            Prepared(
                entities = validEvents.map { EventEntity.fromDomain(it) },
                summary = summary
            )
        }

    private fun filterValidEvents(events: List<Event>): List<Event> {
        val valid = events.filter { event ->
            event.title.isNotBlank() &&
                    event.url.isNotBlank() &&
                    event.url.startsWith("http") &&
                    event.dateTime != null
        }

        if (valid.size < events.size) {
            Timber.w("Отфильтровано ${events.size - valid.size} невалидных событий")
        }

        return valid
    }

    private suspend fun buildRefreshSummary(validEvents: List<Event>): EventRefreshSummary {
        // Подготовим summary "новые события" ДО записи в БД
        // Почему так:
        // - после вставки отличить «новые» от «старых» сложно без дополнительного флага/таблицы;
        // - UI-уведомление о новых событиях не должно быть «шумным», поэтому мы:
        //   1) сравниваем с текущей БД в окне дат,
        //   2) делаем distinct по нормализованному ключу (см. EventIdentityNormalizer),
        //   3) ограничиваем список (в summary хранится максимум 5).
        val dateStrings = validEvents.mapNotNull { it.dateTime?.toLocalDate()?.toString() }
        val minDate = dateStrings.minOrNull()
        val maxDate = dateStrings.maxOrNull()

        val existingKeys = if (minDate != null && maxDate != null) {
            val existing = eventDao.getEventsBetweenDates(
                startDate = "${minDate}T00:00:00",
                endDate = "${maxDate}T23:59:59"
            )
            existing.map {
                EventIdentityNormalizer.fromPersisted(
                    it.title,
                    it.dateTime
                )
            }.toSet()
        } else {
            emptySet()
        }

        val newEvents = validEvents
            .distinctBy { EventIdentityNormalizer.fromEvent(it) }
            .filter { EventIdentityNormalizer.fromEvent(it) !in existingKeys }

        return EventRefreshSummary(
            newEvents = newEvents.take(5)
        )
    }

    override suspend fun toggleFavorite(eventId: String) {
        val current = eventDao.isFavorite(eventId) ?: return
        val newValue = !current
        eventDao.setFavorite(
            eventId,
            newValue
        )

        val updated = eventDao.getEventById(eventId)?.toDomain() ?: return

        if (newValue) {
            reminderScheduler.scheduleForEvent(updated)
        } else {
            reminderScheduler.cancelForEvent(eventId)
        }
    }

    override suspend fun cleanupPastEvents() {
        try {
            eventDao.deletePastEvents()
            Timber.d("Очищены прошедшие события")
        } catch (e: Exception) {
            Timber.e(
                e,
                "Не удалось очистить прошедшие события"
            )
        }
    }

    override suspend fun cleanupDuplicates(): Int {
        return eventDeduplicationService.cleanupExistingDuplicates()
    }

    private data class Prepared(
        val entities: List<EventEntity>,
        val summary: EventRefreshSummary
    )

    private fun List<EventEntity>.toDomainList(): List<Event> = map { it.toDomain() }
}