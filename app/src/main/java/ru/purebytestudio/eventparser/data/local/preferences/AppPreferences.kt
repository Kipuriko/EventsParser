package ru.purebytestudio.eventparser.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.purebytestudio.eventparser.domain.model.EventCategory
import ru.purebytestudio.eventparser.domain.model.EventSortType

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

/**
 * Класс для работы с DataStore Preferences.
 * Хранит настройки приложения, выбранные категории, фильтры и статус онбординга.
 */
class AppPreferences(private val context: Context) {
    private val themeKey = booleanPreferencesKey("is_dark_theme")
    private val sortTypeKey = stringPreferencesKey("event_sort_type")
    private val selectedCategoriesKey = stringSetPreferencesKey("selected_categories")
    private val hasSeenOnboardingKey = booleanPreferencesKey("has_seen_onboarding")
    private val hasRequestedPostNotificationsPermissionKey =
        booleanPreferencesKey("has_requested_post_notifications_permission")
    private val lastSelectedCategoryKey = stringPreferencesKey("last_selected_category")
    private val lastActiveQuickFiltersKey = stringSetPreferencesKey("last_active_quick_filters")

    val isDarkTheme: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[themeKey]
    }

    val sortType: Flow<EventSortType> = context.dataStore.data.map { preferences ->
        preferences[sortTypeKey]?.let { sortTypeName ->
            EventSortType.entries.find { it.name == sortTypeName } ?: EventSortType.DATE_ASC
        } ?: EventSortType.DATE_ASC
    }

    val selectedCategories: Flow<Set<EventCategory>> = context.dataStore.data.map { preferences ->
        preferences[selectedCategoriesKey]?.mapNotNull { categoryName ->
            EventCategory.entries.find { it.name == categoryName }
        }?.toSet() ?: emptySet()
    }

    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[hasSeenOnboardingKey] ?: false
    }

    val hasRequestedPostNotificationsPermission: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[hasRequestedPostNotificationsPermissionKey] ?: false
        }

    val lastSelectedCategory: Flow<EventCategory?> = context.dataStore.data.map { preferences ->
        preferences[lastSelectedCategoryKey]?.let { categoryName ->
            EventCategory.entries.find { it.name == categoryName }
        }
    }

    val lastActiveQuickFilters: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[lastActiveQuickFiltersKey] ?: emptySet()
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = enabled
        }
    }

    suspend fun setSortType(sortType: EventSortType) {
        context.dataStore.edit { preferences ->
            preferences[sortTypeKey] = sortType.name
        }
    }

    suspend fun setSelectedCategories(categories: Set<EventCategory>) {
        context.dataStore.edit { preferences ->
            preferences[selectedCategoriesKey] = categories.map { it.name }.toSet()
        }
    }

    suspend fun clearThemePreference() {
        context.dataStore.edit { preferences ->
            preferences.remove(themeKey)
        }
    }

    suspend fun setOnboardingSeen(seen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[hasSeenOnboardingKey] = seen
        }
    }

    suspend fun setHasRequestedPostNotificationsPermission(requested: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[hasRequestedPostNotificationsPermissionKey] = requested
        }
    }

    suspend fun setLastSelectedCategory(category: EventCategory?) {
        context.dataStore.edit { preferences ->
            if (category != null) {
                preferences[lastSelectedCategoryKey] = category.name
            } else {
                preferences.remove(lastSelectedCategoryKey)
            }
        }
    }

    suspend fun setLastActiveQuickFilters(filters: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[lastActiveQuickFiltersKey] = filters
        }
    }

    suspend fun clearCache() {
        context.dataStore.edit { preferences ->
            preferences.remove(lastSelectedCategoryKey)
            preferences.remove(lastActiveQuickFiltersKey)
            preferences.remove(selectedCategoriesKey)
        }
    }
}