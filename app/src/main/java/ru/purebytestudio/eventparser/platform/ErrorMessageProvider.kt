package ru.purebytestudio.eventparser.platform

/**
 * Провайдер человеко-понятных сообщений об ошибках.
 *
 * Идея: Domain-слой должен уметь вернуть текст ошибки для UI, но при этом не тянуть за собой Android
 * ресурсы/контекст. Поэтому интерфейс находится в `platform`, а Android-реализация — в `data.platform`.
 */
interface ErrorMessageProvider {
    fun noInternet(): String
    fun fromThrowable(
        throwable: Throwable,
        fallback: String? = null
    ): String
}