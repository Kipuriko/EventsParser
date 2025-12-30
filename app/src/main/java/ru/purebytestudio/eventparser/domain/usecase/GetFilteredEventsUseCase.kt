package ru.purebytestudio.eventparser.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.purebytestudio.eventparser.domain.model.Event
import ru.purebytestudio.eventparser.domain.model.EventCategory
import ru.purebytestudio.eventparser.domain.model.EventSortType
import ru.purebytestudio.eventparser.domain.model.QuickFilter
import ru.purebytestudio.eventparser.domain.repository.EventRepository
import ru.purebytestudio.eventparser.platform.TimeProvider
import java.time.LocalDateTime

/**
 * UseCase для получения отфильтрованного списка событий с использованием базы данных.
 */
class GetFilteredEventsUseCase(
    private val repository: EventRepository,
    private val timeProvider: TimeProvider
) {
    operator fun invoke(
        category: EventCategory?,
        quickFilters: Set<QuickFilter>,
        sortType: EventSortType
    ): Flow<List<Event>> {
        val requireOnline = quickFilters.contains(QuickFilter.ONLINE)
        val requireFree = quickFilters.contains(QuickFilter.FREE)

        var startDate: LocalDateTime?
        var endDate: LocalDateTime? = null

        val now = timeProvider.now()
        val today = timeProvider.today()

        // По умолчанию показываем только предстоящие события (или текущие)
        startDate = now

        if (quickFilters.contains(QuickFilter.TODAY)) {
            // События сегодня
            val todayEnd = today.plusDays(1).atStartOfDay().minusNanos(1)
            endDate = todayEnd
        } else if (quickFilters.contains(QuickFilter.THIS_WEEK)) {
            // События на этой неделе (следующие 7 дней)
            val weekLater = today.plusDays(7).atStartOfDay()
            endDate = weekLater
        }

        return repository.getFilteredEvents(
            category = category,
            requireOnline = requireOnline,
            requireFree = requireFree,
            startDate = startDate,
            endDate = endDate
        ).map { events ->
            sort(
                events,
                sortType
            )
        }
    }

    private fun sort(
        events: List<Event>,
        sortType: EventSortType
    ): List<Event> {
        if (events.isEmpty()) return events

        return when (sortType) {
            EventSortType.DATE_ASC -> events.sortedWith(
                compareBy<Event> { it.dateTime ?: LocalDateTime.MAX }
                    .thenBy { it.title.lowercase() }
            )

            EventSortType.DATE_DESC -> events.sortedWith(
                compareByDescending<Event> { it.dateTime ?: LocalDateTime.MIN }
                    .thenBy { it.title.lowercase() }
            )

            EventSortType.TITLE_ASC -> events.sortedBy { it.title.lowercase() }
            EventSortType.TITLE_DESC -> events.sortedByDescending { it.title.lowercase() }
        }
    }
}
