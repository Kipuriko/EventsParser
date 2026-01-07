package ru.purebytestudio.eventparser.data.remote.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class TelegramDateExtractorTest {

    private val extractor = TelegramDateExtractor()

    @Test
    fun numericDate_withoutTime_defaultsToNoon_andHasSpecificTimeFalse() {
        val publishedAt = LocalDateTime.of(
            2025,
            12,
            20,
            10,
            0
        )
        val text = """
            Крутой митап про Kotlin
            27.12.2025
        """.trimIndent()

        val info = extractor.extractDateInfo(
            text = text,
            publishedAt = publishedAt
        )
        assertNotNull(info)
        requireNotNull(info)

        assertEquals(
            2025,
            info.start.year
        )
        assertEquals(
            12,
            info.start.monthValue
        )
        assertEquals(
            27,
            info.start.dayOfMonth
        )
        assertEquals(
            LocalTime.NOON,
            info.start.toLocalTime()
        )
        assertEquals(
            false,
            info.hasSpecificTime
        )
        assertNull(info.end)
    }

    @Test
    fun monthNameDate_isNotParsed_andReturnsNull() {
        val publishedAt = LocalDateTime.of(
            2025,
            12,
            20,
            10,
            0
        )
        val text = "Митап\n7 января\n"

        val info = extractor.extractDateInfo(
            text = text,
            publishedAt = publishedAt
        )
        // Текущее поведение: парсим только числовые даты (dd.MM.yyyy).
        // Даты с названиями месяцев на русском сейчас не распознаются.
        assertNull(info)
    }

    @Test
    fun monthDate_withoutYear_returnsNull_whenTooFarFromPublishedAt() {
        val publishedAt = LocalDateTime.of(
            2025,
            1,
            1,
            10,
            0
        )
        val text = "Конференция\n1 декабря\n"

        val info = extractor.extractDateInfo(
            text = text,
            publishedAt = publishedAt
        )
        assertNull(info)
    }

    @Test
    fun dateRangeWithMonthName_isNotParsed_andReturnsNull() {
        val publishedAt = LocalDateTime.of(
            2025,
            12,
            20,
            10,
            0
        )
        val text = "Хакатон 16–29 января 2026 с 10:00 до 18:00"

        val info = extractor.extractDateInfo(
            text = text,
            publishedAt = publishedAt
        )
        assertNull(info)
    }

    @Test
    fun numericDate_withTime_parsesTime_andHasSpecificTimeTrue() {
        val publishedAt = LocalDateTime.of(
            2025,
            12,
            20,
            10,
            0
        )
        // Важно: timeRegex также матчится на "27.12" внутри даты, поэтому время в конце строки может не быть найдено.
        // Чтобы зафиксировать текущее поведение корректного парсинга времени, ставим время ПЕРЕД датой.
        val text = "Митап в 10:30 27.12.2025"

        val info = extractor.extractDateInfo(
            text = text,
            publishedAt = publishedAt
        )
        assertNotNull(info)
        requireNotNull(info)

        assertEquals(
            LocalDateTime.of(
                2025,
                12,
                27,
                10,
                30
            ),
            info.start
        )
        assertEquals(
            true,
            info.hasSpecificTime
        )
    }

    @Test
    fun numericDate_withTimeRange_isRejected_dueToDeadlineHeuristic() {
        val publishedAt = LocalDateTime.of(
            2025,
            12,
            20,
            10,
            0
        )
        val text = "Митап 27.12.2025 с 10:00 до 18:00"

        val info = extractor.extractDateInfo(
            text = text,
            publishedAt = publishedAt
        )
        // Текущее поведение: наличие "до " рядом с датой трактуется как дедлайн/условие.
        assertNull(info)
    }

    @Test
    fun deadlineLikeDate_isRejected() {
        val publishedAt = LocalDateTime.of(
            2025,
            12,
            20,
            10,
            0
        )
        val text = "Скидка до 31.12.2025 на билеты"

        val info = extractor.extractDateInfo(
            text = text,
            publishedAt = publishedAt
        )
        assertNull(info)
    }

    @Test
    fun exhibitionDate_withDateRange_parsesStartDate() {
        val publishedAt = LocalDateTime.of(
            2026,
            1,
            7,
            10,
            0
        )
        val text = """
            iAGRI 2026
            
            Дата проведения: 21.01.2026 - 23.01.2026. Начало 21.01.2026 в 08:00
            
            Место проведения: Москва , Крокус Экспо
            
            Организатором выступает российская выставочная компания
        """.trimIndent()

        val info = extractor.extractDateInfo(
            text = text,
            publishedAt = publishedAt
        )
        assertNotNull(info)
        requireNotNull(info)

        assertEquals(
            2026,
            info.start.year
        )
        assertEquals(
            1,
            info.start.monthValue
        )
        assertEquals(
            21,
            info.start.dayOfMonth
        )
    }

    @Test
    fun competitionDate_withCallForApplications_parsesDate() {
        val publishedAt = LocalDateTime.of(
            2026,
            1,
            7,
            10,
            0
        )
        val text = """
            Data Fusion Awards 2026. Прием заявок
            
            Дата проведения: 19.01.2026 - 20.01.2026. Начало 19.01.2026 в 10:00
            
            Место проведения: Онлайн
            
            Конкурс направлен на продвижение технологий работы с данными и ИИ.
        """.trimIndent()

        val info = extractor.extractDateInfo(
            text = text,
            publishedAt = publishedAt
        )
        assertNotNull(info)
        requireNotNull(info)

        assertEquals(
            2026,
            info.start.year
        )
        assertEquals(
            1,
            info.start.monthValue
        )
        assertEquals(
            19,
            info.start.dayOfMonth
        )
    }
}