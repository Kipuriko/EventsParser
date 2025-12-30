package ru.purebytestudio.eventparser.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.purebytestudio.eventparser.domain.model.EventCategory
import ru.purebytestudio.eventparser.domain.model.EventSortType
import ru.purebytestudio.eventparser.domain.model.QuickFilter

/**
 * Абстракция над персистентными настройками пользователя, чтобы слои presentation/domain
 * оставались отвязанными от конкретной реализации хранилища.
 */
interface UserPreferencesRepository {
    val isDarkTheme: Flow<Boolean?>
    val sortType: Flow<EventSortType>
    val selectedCategories: Flow<Set<EventCategory>>
    val hasSeenOnboarding: Flow<Boolean>
    val hasRequestedPostNotificationsPermission: Flow<Boolean>
    val lastSelectedCategory: Flow<EventCategory?>
    val lastActiveQuickFilters: Flow<Set<QuickFilter>>

    suspend fun setDarkTheme(enabled: Boolean)
    suspend fun clearThemePreference()
    suspend fun setSortType(sortType: EventSortType)
    suspend fun setSelectedCategories(categories: Set<EventCategory>)
    suspend fun setLastSelectedCategory(category: EventCategory?)
    suspend fun setLastActiveQuickFilters(filters: Set<QuickFilter>)
    suspend fun setOnboardingSeen(seen: Boolean)
    suspend fun setHasRequestedPostNotificationsPermission(requested: Boolean)
    suspend fun clearCache()
}