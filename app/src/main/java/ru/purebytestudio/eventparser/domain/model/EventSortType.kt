package ru.purebytestudio.eventparser.domain.model

/**
 * Типы сортировки событий
 */
enum class EventSortType(val displayName: String) {
    DATE_ASC("По дате (сначала ближайшие)"),
    DATE_DESC("По дате (сначала дальние)"),
    TITLE_ASC("По алфавиту (А-Я)"),
    TITLE_DESC("По алфавиту (Я-А)")
}