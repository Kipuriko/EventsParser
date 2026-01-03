package ru.purebytestudio.eventparser.platform

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Абстракция времени.
 *
 * Нужна, чтобы:
 * - изолировать `LocalDateTime.now()` от Domain-логики (удобнее тестировать),
 * - централизовать «текущее время» в одном месте (например, если понадобится смещение/серверное время).
 */
interface TimeProvider {
    fun now(): LocalDateTime
    fun today(): LocalDate
}