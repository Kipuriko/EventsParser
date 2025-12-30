package ru.purebytestudio.eventparser.platform

/**
 * Контракт стратегии повторов (retry).
 *
 * Задача: инкапсулировать политику повторных попыток для операций, которые могут «падать» временно
 * (например, сеть).
 *
 * Почему это не `fun interface`: Kotlin не разрешает generic-методы у функциональных интерфейсов.
 */
interface RetryPolicy {
    suspend fun <T> execute(block: suspend () -> T): T
}