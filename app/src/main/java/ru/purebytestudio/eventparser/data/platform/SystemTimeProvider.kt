package ru.purebytestudio.eventparser.data.platform

import ru.purebytestudio.eventparser.platform.TimeProvider
import java.time.LocalDate
import java.time.LocalDateTime

class SystemTimeProvider : TimeProvider {
    override fun now(): LocalDateTime = LocalDateTime.now()
    override fun today(): LocalDate = LocalDate.now()
}