package ru.purebytestudio.eventparser.platform

import kotlinx.coroutines.flow.Flow

/**
 * Абстракция проверки сети.
 *
 * Зачем нужна:
 * - Domain-слой не должен зависеть от Android API (`ConnectivityManager` и т.п.);
 * - реализация находится в Data-слое (см. `data.platform.AndroidNetworkStatusProvider`).
 */
interface NetworkStatusProvider {
    /**
     * Текущее состояние сети «здесь и сейчас».
     */
    fun isOnline(): Boolean

    /**
     * Поток изменений состояния сети.
     */
    fun observe(): Flow<Boolean>
}