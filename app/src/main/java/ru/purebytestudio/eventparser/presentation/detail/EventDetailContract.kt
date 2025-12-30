package ru.purebytestudio.eventparser.presentation.detail

import ru.purebytestudio.eventparser.domain.model.Event

/**
 * Контракт экрана деталей события (MVI).
 */
data class EventDetailState(
    val event: Event? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class EventDetailSideEffect {
    data class OpenUrl(val url: String) : EventDetailSideEffect()
    data class ShareEvent(val event: Event) : EventDetailSideEffect()
    data class AddToCalendar(val event: Event) : EventDetailSideEffect()
    data object NavigateBack : EventDetailSideEffect()
}
