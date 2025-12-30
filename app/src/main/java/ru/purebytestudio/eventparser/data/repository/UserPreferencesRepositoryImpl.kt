package ru.purebytestudio.eventparser.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.purebytestudio.eventparser.data.local.preferences.AppPreferences
import ru.purebytestudio.eventparser.domain.model.EventCategory
import ru.purebytestudio.eventparser.domain.model.EventSortType
import ru.purebytestudio.eventparser.domain.model.QuickFilter
import ru.purebytestudio.eventparser.domain.repository.UserPreferencesRepository

class UserPreferencesRepositoryImpl(
    private val appPreferences: AppPreferences
) : UserPreferencesRepository {
    override val isDarkTheme: Flow<Boolean?> = appPreferences.isDarkTheme
    override val sortType: Flow<EventSortType> = appPreferences.sortType
    override val selectedCategories: Flow<Set<EventCategory>> = appPreferences.selectedCategories
    override val hasSeenOnboarding: Flow<Boolean> = appPreferences.hasSeenOnboarding
    override val hasRequestedPostNotificationsPermission: Flow<Boolean> =
        appPreferences.hasRequestedPostNotificationsPermission
    override val lastSelectedCategory: Flow<EventCategory?> = appPreferences.lastSelectedCategory
    override val lastActiveQuickFilters: Flow<Set<QuickFilter>> =
        appPreferences.lastActiveQuickFilters.map { names ->
            names.mapNotNull { name ->
                QuickFilter.entries.find { it.name == name }
            }.toSet()
        }

    override suspend fun setDarkTheme(enabled: Boolean) = appPreferences.setDarkTheme(enabled)

    override suspend fun clearThemePreference() = appPreferences.clearThemePreference()

    override suspend fun setSortType(sortType: EventSortType) = appPreferences.setSortType(sortType)

    override suspend fun setSelectedCategories(categories: Set<EventCategory>) =
        appPreferences.setSelectedCategories(categories)

    override suspend fun setLastSelectedCategory(category: EventCategory?) =
        appPreferences.setLastSelectedCategory(category)

    override suspend fun setLastActiveQuickFilters(filters: Set<QuickFilter>) =
        appPreferences.setLastActiveQuickFilters(filters.map { it.name }.toSet())

    override suspend fun setOnboardingSeen(seen: Boolean) = appPreferences.setOnboardingSeen(seen)

    override suspend fun setHasRequestedPostNotificationsPermission(requested: Boolean) =
        appPreferences.setHasRequestedPostNotificationsPermission(requested)

    override suspend fun clearCache() = appPreferences.clearCache()
}