package ru.purebytestudio.eventparser.presentation.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.collectLatest
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import ru.purebytestudio.eventparser.domain.repository.UserPreferencesRepository
import ru.purebytestudio.eventparser.domain.usecase.CleanupDuplicateEventsUseCase
import timber.log.Timber

/**
 * ViewModel для экрана настроек.
 */
class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val cleanupDuplicateEventsUseCase: CleanupDuplicateEventsUseCase
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

    fun cleanupDuplicates() = intent {
        reduce { state.copy(isCleaningDuplicates = true) }

        try {
            val deletedCount = cleanupDuplicateEventsUseCase()
            postSideEffect(SettingsSideEffect.DuplicatesCleaned(deletedCount))
        } catch (e: Exception) {
            Timber.e(
                e,
                "Ошибка при очистке дубликатов"
            )
            postSideEffect(SettingsSideEffect.ShowMessage("Ошибка при очистке дубликатов"))
        } finally {
            reduce { state.copy(isCleaningDuplicates = false) }
        }
    }
}