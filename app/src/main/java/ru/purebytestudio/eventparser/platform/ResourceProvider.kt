package ru.purebytestudio.eventparser.platform

import androidx.annotation.StringRes

/**
 * Провайдер Android-строк для слоёв, где нежелательно держать прямую зависимость от `Context`.
 *
 * Обычно используется для:
 * - локализованных сообщений об ошибках,
 * - строковых шаблонов (format args).
 */
interface ResourceProvider {
    fun getString(@StringRes id: Int): String
    fun getString(
        @StringRes id: Int,
        vararg args: Any
    ): String
}

