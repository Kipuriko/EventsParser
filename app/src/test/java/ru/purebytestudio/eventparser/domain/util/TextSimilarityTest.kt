package ru.purebytestudio.eventparser.domain.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextSimilarityTest {

    @Test
    fun hasCommonEventNames_withSameEventName_returnsTrue() {
        val text1 = """
            T-Sync Conf — конференция БЕЗ ДОКЛАДОВ
            7 февраля в Москве
        """.trimIndent()

        val text2 = """
            Встречайте новый формат инженерного диалога
            T-Sync Conf — офлайн-конференция от Группы «Т-Технологии»
            7 февраля в Москве
        """.trimIndent()

        assertTrue(
            "Должны найти общее название 'T-Sync Conf'",
            TextSimilarity.hasCommonEventNames(
                text1,
                text2
            )
        )
    }

    @Test
    fun hasCommonEventNames_withDifferentEvents_returnsFalse() {
        val text1 = """
            Android Dev Summit 2026
            15 февраля в Москве
        """.trimIndent()

        val text2 = """
            iOS Dev Conference 2026
            20 февраля в Санкт-Петербурге
        """.trimIndent()

        assertFalse(
            "Не должны найти общие названия",
            TextSimilarity.hasCommonEventNames(
                text1,
                text2
            )
        )
    }

    @Test
    fun hasCommonEventNames_withShortNames_returnsFalse() {
        val text1 = "IT meetup в среду"
        val text2 = "IT event в пятницу"

        assertFalse(
            "Короткие названия (< 5 символов) не должны считаться",
            TextSimilarity.hasCommonEventNames(
                text1,
                text2
            )
        )
    }

    @Test
    fun hasCommonEventNames_withKotlinConf_returnsTrue() {
        val text1 = """
            🎉 KotlinConf 2026 объявляет регистрацию
            Главная конференция по Kotlin
        """.trimIndent()

        val text2 = """
            Приглашаем на KotlinConf 2026
            Три дня докладов и workshop'ов
        """.trimIndent()

        assertTrue(
            "Должны найти общее название 'KotlinConf'",
            TextSimilarity.hasCommonEventNames(
                text1,
                text2
            )
        )
    }

    @Test
    fun hasCommonEventNames_withNoEventNames_returnsFalse() {
        val text1 = "просто текст без названий событий"
        val text2 = "ещё один текст без названий"

        assertFalse(
            "Без названий событий должны вернуть false",
            TextSimilarity.hasCommonEventNames(
                text1,
                text2
            )
        )
    }

    @Test
    fun hasCommonEventNames_withComplexEventName_returnsTrue() {
        val text1 = """
            AI ML Summit 2026
            Конференция по искусственному интеллекту
        """.trimIndent()

        val text2 = """
            Регистрация на AI ML Summit 2026 открыта!
            Не пропустите главное событие года
        """.trimIndent()

        assertTrue(
            "Должны найти общее название 'AI ML Summit'",
            TextSimilarity.hasCommonEventNames(
                text1,
                text2
            )
        )
    }

    @Test
    fun areSimilar_withCommonEventNames_returnsTrue() {
        val text1 = "T-Sync Conf — конференция БЕЗ ДОКЛАДОВ"
        val text2 = "Встречайте новый формат: T-Sync Conf в Москве"

        assertTrue(
            "Тексты с общим названием события должны считаться похожими",
            TextSimilarity.areSimilar(
                text1,
                text2,
                0.65
            )
        )
    }

    @Test
    fun jaccardSimilarity_identicalStrings_returnsOne() {
        val str = "конференция по разработке"
        val similarity = TextSimilarity.jaccardSimilarity(
            str,
            str
        )

        assertTrue(
            "Идентичные строки должны иметь similarity = 1.0",
            similarity == 1.0
        )
    }

    @Test
    fun jaccardSimilarity_completelyDifferent_returnsLow() {
        val str1 = "конференция kotlin"
        val str2 = "митап по javascript"
        val similarity = TextSimilarity.jaccardSimilarity(
            str1,
            str2
        )

        assertTrue(
            "Разные строки должны иметь низкий similarity",
            similarity < 0.5
        )
    }

    @Test
    fun levenshteinDistance_identicalStrings_returnsZero() {
        val str = "test"
        val distance = TextSimilarity.levenshteinDistance(
            str,
            str
        )

        assertTrue(
            "Расстояние для идентичных строк должно быть 0",
            distance == 0
        )
    }

    @Test
    fun levenshteinSimilarity_identicalStrings_returnsOne() {
        val str = "конференция"
        val similarity = TextSimilarity.levenshteinSimilarity(
            str,
            str
        )

        assertTrue(
            "Идентичные строки должны иметь similarity = 1.0",
            similarity == 1.0
        )
    }
}
