package ru.purebytestudio.eventparser.presentation.settings

/**
 * Контракт экрана настроек (MVI).
 */
data class SettingsState(
    val isDarkTheme: Boolean? = null,
    val isCleaningDuplicates: Boolean = false
)

sealed class SettingsSideEffect {
    data class ShowMessage(val message: String) : SettingsSideEffect()
    data object CacheCleared : SettingsSideEffect()
    data class DuplicatesCleaned(val count: Int) : SettingsSideEffect()
}