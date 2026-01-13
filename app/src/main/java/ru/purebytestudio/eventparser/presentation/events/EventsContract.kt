package ru.purebytestudio.eventparser.presentation.events

import ru.purebytestudio.eventparser.domain.model.Event
import ru.purebytestudio.eventparser.domain.model.EventCategory
import ru.purebytestudio.eventparser.domain.model.EventSortType
import ru.purebytestudio.eventparser.domain.model.QuickFilter

/**
 * Контракт экрана списка событий (MVI):
 * - [EventsState] — состояние
 * - [EventsSideEffect] — одноразовые эффекты (навигация/тосты и т.п.)
 */
data class EventsState(
    val events: List<Event> = emptyList(),
    val filteredEvents: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isInitialLoad: Boolean = true,
    val error: String? = null,
    val selectedCategory: EventCategory? = null,
    val sortType: EventSortType = EventSortType.DATE_ASC,
    val activeQuickFilters: Set<QuickFilter> = emptySet(),
    val availableCategories: List<EventCategory> = listOf(
        EventCategory.ANDROID_DEV,
        EventCategory.GAME_DEV,
        EventCategory.IOS_DEV,
        EventCategory.WEB_DEV,
        EventCategory.ML_AI,
        EventCategory.DEVOPS,
        EventCategory.OTHER
    )
) {
    val showEmptyState: Boolean
        get() = !isLoading && !isRefreshing && events.isEmpty() && !isInitialLoad

    val showNoResultsState: Boolean
        get() = !isLoading && !isRefreshing && filteredEvents.isEmpty() && selectedCategory != null && events.isNotEmpty()

    val showContent: Boolean
        get() = !isLoading || events.isNotEmpty()
}

sealed class EventsSideEffect {
    data class ShowError(val message: String) : EventsSideEffect()
    data class NavigateToDetail(val eventId: String) : EventsSideEffect()
    data object RefreshComplete : EventsSideEffect()
}