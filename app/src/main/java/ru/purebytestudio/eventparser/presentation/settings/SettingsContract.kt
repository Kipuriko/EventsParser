package ru.purebytestudio.eventparser.presentation.settings

/**
 * Контракт экрана настроек (MVI).
 */
data class SettingsState(
    val isDarkTheme: Boolean? = null
)

sealed class SettingsSideEffect {
    data class ShowMessage(val message: String) : SettingsSideEffect()
    data object CacheCleared : SettingsSideEffect()
}
