package ru.purebytestudio.eventparser.domain.model

/**
 * Результат обновления событий из источников.
 *
 * @property newEvents новые события, которых не было в БД на момент обновления (после дедупликации по ключу)
 */
data class EventRefreshSummary(
    val newEvents: List<Event>
) {
    val newEventsCount: Int get() = newEvents.size
}