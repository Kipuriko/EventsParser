package ru.purebytestudio.eventparser.data.remote.parser

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

internal data class TelegramDateInfo(
    val start: LocalDateTime,
    val end: LocalDateTime? = null,
    val hasSpecificTime: Boolean
)

/**
 * Извлечение даты/времени из текста Telegram-сообщения.
 *
 * Важно: логика чувствительна к формулировкам и регрессии здесь очень легко «не заметить».
 * Любые изменения должны сопровождаться тестами.
 */
internal class TelegramDateExtractor {
    private val ruLocale = Locale.forLanguageTag("ru")

    fun extractDateInfo(
        text: String,
        publishedAt: LocalDateTime
    ): TelegramDateInfo? {
        val lines = text.lines()

        // 1) Диапазон дат вида "16–29 января 2026"
        TelegramParsingRegex.monthDateRangeRegex.find(text)?.let { m ->
            if (isNotAnEventDate(
                    text,
                    m.range
                )
            ) return@let null
            val startDay = m.groupValues[1].toIntOrNull() ?: return@let null
            val endDay = m.groupValues[2].toIntOrNull() ?: return@let null
            val month = parseMonth(m.groupValues[3]) ?: return@let null
            val year = parseOrInferYear(
                m.groupValues.getOrNull(4),
                month,
                startDay,
                publishedAt
            ) ?: return@let null

            val startDate = safeDate(
                year,
                month,
                startDay
            ) ?: return@let null
            val endDate = safeDate(
                year,
                month,
                endDay
            ) ?: return@let null

            val timeRange = extractTimeRangeNear(
                lines,
                m.range
            )
            val startTime = timeRange?.first ?: extractTimeNear(
                lines,
                m.range
            )
            val endTime = timeRange?.second

            val hasTime = startTime != null
            val startDateTime = LocalDateTime.of(
                startDate,
                startTime ?: LocalTime.NOON
            )
            val endDateTime = if (endTime != null) LocalDateTime.of(
                endDate,
                endTime
            ) else null
            return TelegramDateInfo(
                start = startDateTime,
                end = endDateTime,
                hasSpecificTime = hasTime
            )
        }

        // 2) Числовая дата "27.12.2025"
        TelegramParsingRegex.numericDateRegex.find(text)?.let { m ->
            if (isNotAnEventDate(
                    text,
                    m.range
                )
            ) return@let null
            val day = m.groupValues[1].toIntOrNull() ?: return@let null
            val month = m.groupValues[2].toIntOrNull() ?: return@let null
            val year = parseYearFlexible(m.groupValues[3]) ?: return@let null
            val date = safeDate(
                year,
                month,
                day
            ) ?: return@let null

            val timeRange = extractTimeRangeNear(
                lines,
                m.range
            )
            val startTime = timeRange?.first ?: extractTimeNear(
                lines,
                m.range
            )
            val endTime = timeRange?.second
            val hasTime = startTime != null

            return TelegramDateInfo(
                start = LocalDateTime.of(
                    date,
                    startTime ?: LocalTime.NOON
                ),
                end = endTime?.let {
                    LocalDateTime.of(
                        date,
                        it
                    )
                },
                hasSpecificTime = hasTime
            )
        }

        // 3) Дата "7 февраля [2026]"
        TelegramParsingRegex.monthDateRegex.find(text)?.let { m ->
            if (isNotAnEventDate(
                    text,
                    m.range
                )
            ) return@let null
            val day = m.groupValues[1].toIntOrNull() ?: return@let null
            val month = parseMonth(m.groupValues[2]) ?: return@let null
            val year = parseOrInferYear(
                m.groupValues.getOrNull(3),
                month,
                day,
                publishedAt
            ) ?: return@let null
            val date = safeDate(
                year,
                month,
                day
            ) ?: return@let null

            val timeRange = extractTimeRangeNear(
                lines,
                m.range
            )
            val startTime = timeRange?.first ?: extractTimeNear(
                lines,
                m.range
            )
            val endTime = timeRange?.second
            val hasTime = startTime != null

            return TelegramDateInfo(
                start = LocalDateTime.of(
                    date,
                    startTime ?: LocalTime.NOON
                ),
                end = endTime?.let {
                    LocalDateTime.of(
                        date,
                        it
                    )
                },
                hasSpecificTime = hasTime
            )
        }

        return null
    }

    /**
     * Отсекаем “даты”, которые на деле являются дедлайнами/условиями ("до 31 декабря", "включительно", "скидки до ...").
     */
    private fun isNotAnEventDate(
        fullText: String,
        range: IntRange
    ): Boolean {
        val start = (range.first - 20).coerceAtLeast(0)
        val end = (range.last + 20).coerceAtMost(fullText.length)
        val context = fullText.substring(
            start,
            end
        ).lowercase(ruLocale)

        val deadlineMarkers = listOf(
            "до ",
            "по ",
            "включительно",
            "прием заявок",
            "приём заявок",
            "заявки до",
            "скидк",
            "распродаж",
            "акция",
            "промокод",
            "реклама"
        )
        return deadlineMarkers.any { context.contains(it) }
    }

    private fun parseOrInferYear(
        rawYear: String?,
        month: Int,
        day: Int,
        publishedAt: LocalDateTime
    ): Int? {
        val explicit = rawYear?.trim()?.takeIf { it.isNotBlank() }?.toIntOrNull()
        if (explicit != null) return explicit

        // Требование: если год не указан — ориентируемся на дату публикации и считаем валидным,
        // только если событие попадает в следующие 3 месяца.
        val publishedDate = publishedAt.toLocalDate()
        val candidateThisYear = safeDate(
            publishedDate.year,
            month,
            day
        ) ?: return null

        val candidate = if (candidateThisYear.isBefore(publishedDate)) {
            safeDate(
                publishedDate.year + 1,
                month,
                day
            ) ?: return null
        } else {
            candidateThisYear
        }

        val maxAllowed = publishedDate.plusMonths(3)
        if (candidate.isAfter(maxAllowed)) return null

        return candidate.year
    }

    internal fun parseYearFlexible(yearRaw: String): Int? {
        val y = yearRaw.trim().toIntOrNull() ?: return null
        return if (y < 100) 2000 + y else y
    }

    internal fun safeDate(
        year: Int,
        month: Int,
        day: Int
    ): LocalDate? =
        try {
            LocalDate.of(
                year,
                month,
                day
            )
        } catch (_: Exception) {
            null
        }

    private fun parseMonth(monthStr: String): Int? {
        val lower = monthStr.lowercase(ruLocale)
        return when {
            lower.startsWith("янв") -> 1
            lower.startsWith("фев") -> 2
            lower.startsWith("мар") -> 3
            lower.startsWith("апр") -> 4
            lower.startsWith("мая") || lower.startsWith("май") -> 5
            lower.startsWith("июн") -> 6
            lower.startsWith("июл") -> 7
            lower.startsWith("авг") -> 8
            lower.startsWith("сен") -> 9
            lower.startsWith("окт") -> 10
            lower.startsWith("ноя") -> 11
            lower.startsWith("дек") -> 12
            else -> null
        }
    }

    private fun extractTimeRangeNear(
        lines: List<String>,
        matchRange: IntRange
    ): Pair<LocalTime, LocalTime>? {
        val (snippet, _) = extractSnippetAround(
            lines,
            matchRange
        )
        val rangeRegex =
            Regex("""(?i)\bс\s*(\d{1,2})[:.](\d{2})\s*(?:до|-)\s*(\d{1,2})[:.](\d{2})\b""")
        val m = rangeRegex.find(snippet) ?: return null
        val h1 = m.groupValues[1].toIntOrNull() ?: return null
        val m1 = m.groupValues[2].toIntOrNull() ?: return null
        val h2 = m.groupValues[3].toIntOrNull() ?: return null
        val m2 = m.groupValues[4].toIntOrNull() ?: return null
        return try {
            LocalTime.of(
                h1,
                m1
            ) to LocalTime.of(
                h2,
                m2
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun extractTimeNear(
        lines: List<String>,
        matchRange: IntRange
    ): LocalTime? {
        val (snippet, _) = extractSnippetAround(
            lines,
            matchRange
        )
        val m = TelegramParsingRegex.timeRegex.find(snippet) ?: return null
        val h = m.groupValues[1].toIntOrNull() ?: return null
        val min = m.groupValues[2].toIntOrNull() ?: return null
        return try {
            LocalTime.of(
                h,
                min
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun extractSnippetAround(
        lines: List<String>,
        matchRange: IntRange
    ): Pair<String, IntRange> {
        // грубо сопоставляем по индексу символов в full text: это достаточно для наших целей
        val full = lines.joinToString("\n")
        val start = (matchRange.first - 120).coerceAtLeast(0)
        val end = (matchRange.last + 120).coerceAtMost(full.length)
        return full.substring(
            start,
            end
        ) to (start..end)
    }
}
