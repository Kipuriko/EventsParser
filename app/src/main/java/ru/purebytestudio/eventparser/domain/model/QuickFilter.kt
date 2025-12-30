package ru.purebytestudio.eventparser.domain.model

/**
 * Перечисление быстрых фильтров для списка событий.
 *
 * @property displayName Отображаемое название фильтра
 */
enum class QuickFilter(val displayName: String) {
    FREE("Бесплатные"),
    ONLINE("Онлайн"),
    TODAY("Сегодня"),
    THIS_WEEK("На этой неделе")
}