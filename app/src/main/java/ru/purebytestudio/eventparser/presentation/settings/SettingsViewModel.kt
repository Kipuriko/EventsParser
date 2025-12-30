package ru.purebytestudio.eventparser.presentation.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.collectLatest
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import ru.purebytestudio.eventparser.domain.repository.UserPreferencesRepository

/**
 * ViewModel для экрана настроек.
 */
class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel(), ContainerHost<SettingsState, SettingsSideEffect> {
    override val container = container<SettingsState, SettingsSideEffect>(SettingsState())

    init {
        loadThemePreference()
    }

    private fun loadThemePreference() = intent {
        userPreferencesRepository.isDarkTheme.collectLatest { isDark ->
            reduce { state.copy(isDarkTheme = isDark) }
        }
    }

    fun setDarkTheme(enabled: Boolean) = intent {
        userPreferencesRepository.setDarkTheme(enabled)
    }

    fun resetThemeToSystem() = intent {
        userPreferencesRepository.clearThemePreference()
    }

    fun clearCache() = intent {
        userPreferencesRepository.clearCache()
        postSideEffect(SettingsSideEffect.CacheCleared)
    }
}