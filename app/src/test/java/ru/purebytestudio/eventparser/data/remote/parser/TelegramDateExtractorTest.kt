package ru.purebytestudio.eventparser.data.remote.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month

class TelegramDateExtractorTest {
    private val extractor = TelegramDateExtractor()
    private val publishedAt = LocalDateTime.of(2026, 1, 1, 12, 0)

    @Test
    fun `extractDateInfo finds time when it is far from date`() {
        val text = """
            📅 15 января 2026 года состоится митап.
            
            Очень много текста про спикеров...
            ...
            ...
            ... еще текст ...
            
            ⏰ Начало в 19:00
            📍 Где-то там
        """.trimIndent()

        val result = extractor.extractDateInfo(text, publishedAt)

        assertNotNull("Result should not be null", result)
        assertEquals("Should have specific time", true, result?.hasSpecificTime)
        assertEquals(
            "Time should be 19:00",
            LocalTime.of(19, 0),
            result?.start?.toLocalTime()
        )
        assertEquals(
            "Date should be 15 Jan",
            LocalDateTime.of(2026, Month.JANUARY, 15, 19, 0),
            result?.start
        )
        }

    @Test
    fun `extractDateInfo ignores dot-separated date looking like time`() {
        // "03.02" is ambiguous. It looks like a date (DD.MM) but also like time if dots allowed.
        // We provide a MAIN valid date so extraction succeeds, and "03.02" nearby.
        
        val text = """
            📅 Дата: 01.01.2026
            
            Важное напоминание 03.02 не забудьте.
        """.trimIndent()

        val result = extractor.extractDateInfo(text, publishedAt)

        // Should find date 01.01.2026
        assertNotNull("Should find event date", result)
        
        // Should NOT find 03:02 as time
        if (result!!.hasSpecificTime) {
            val time = result.start.toLocalTime()
            val isThreeOhTwo = time.hour == 3 && time.minute == 2
            assertFalse("Should not mistake 03.02 date snippet for 03:02 time", isThreeOhTwo)
        }
    }
    
    @Test
    fun `extractDateInfo ignores dot-separated date when full date is present`() {
        val text = "Дата: 15.01.2026"
        val result = extractor.extractDateInfo(text, publishedAt)
        
        // Should find date, but NO time.
        assertNotNull(result)
        assertEquals(false, result?.hasSpecificTime)
    }

    @Test
    fun `extractDateInfo finds time with colon`() {
        val text = "Дата: 01.01.2026. Начало в 15:30"
        val result = extractor.extractDateInfo(text, publishedAt)
        
        assertNotNull(result)
        assertEquals(LocalTime.of(15, 30), result?.start?.toLocalTime())
    }

}