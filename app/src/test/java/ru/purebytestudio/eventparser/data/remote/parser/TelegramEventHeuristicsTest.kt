package ru.purebytestudio.eventparser.data.remote.parser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramEventHeuristicsTest {
    private val heuristics = TelegramEventHeuristics()

    @Test
    fun looksLikeEvent_withConferenceMarker_returnsTrue() {
        val text = """
            Крутая конференция по Kotlin
            Дата: 27.12.2025
        """.trimIndent()

        assertTrue(heuristics.looksLikeEvent(text))
    }

    @Test
    fun looksLikeEvent_withMeetupMarker_returnsTrue() {
        val text = """
            Митап для разработчиков
            Дата: 27.12.2025
        """.trimIndent()

        assertTrue(heuristics.looksLikeEvent(text))
    }

    @Test
    fun looksLikeEvent_withExhibitionMarker_returnsTrue() {
        // Проверяем, что слово "выставочной" содержит префикс "выстав"
        assertTrue("выставочной".lowercase().contains("выстав"))

        val text = """
            iAGRI 2026

            Дата проведения: 21.01.2026 - 23.01.2026. Начало 21.01.2026 в 08:00

            Место проведения: Москва , Крокус Экспо

            Организатором выступает российская выставочная компания «Агрос Экспо Групп».

            В рамках выставочной экспозиции iAGRI ведущие российские и зарубежные компании представят передовые технологии
        """.trimIndent()

        val lower = text.lowercase()
        assertTrue(
            "Text should contain 'выстав'",
            lower.contains("выстав")
        )
        assertFalse(
            "Text should not contain promo markers",
            lower.contains("ооо")
        )

        assertTrue(heuristics.looksLikeEvent(text))
    }

    @Test
    fun looksLikeEvent_withCompetitionMarker_returnsTrue() {
        val text = """
            Data Fusion Awards 2026. Прием заявок

            Дата проведения: 19.01.2026 - 20.01.2026. Начало 19.01.2026 в 10:00

            Место проведения: Онлайн

            Конкурс направлен на продвижение технологий работы с данными и ИИ.
        """.trimIndent()

        assertTrue(heuristics.looksLikeEvent(text))
    }

    @Test
    fun looksLikeEvent_withAwardsMarker_returnsTrue() {
        val text = """
            Tech Awards 2026
            
            Прием заявок до 20.01.2026
            
            Awards для лучших стартапов
        """.trimIndent()

        assertTrue(heuristics.looksLikeEvent(text))
    }

    @Test
    fun looksLikeEvent_withExpoMarker_returnsTrue() {
        val text = """
            IT Экспо 2026
            
            Дата: 15.02.2026
            
            Место: Москва
        """.trimIndent()

        assertTrue(heuristics.looksLikeEvent(text))
    }

    @Test
    fun looksLikeEvent_withRegistrationMarker_returnsTrue() {
        val text = """
            Конференция по ИИ
            
            Регистрация открыта
            
            Дата: 15.02.2026
        """.trimIndent()

        assertTrue(heuristics.looksLikeEvent(text))
    }

    @Test
    fun looksLikeEvent_withPromoMarkers_returnsFalse() {
        val text = """
            Реклама
            Крутая акция на курсы
            Скидка до 31.12.2025
        """.trimIndent()

        assertFalse(heuristics.looksLikeEvent(text))
    }

    @Test
    fun looksLikeEvent_withSaleMarkers_returnsFalse() {
        val text = """
            Купите курс со скидкой!
            Промокод: SALE2026
            До 31.12.2025
        """.trimIndent()

        assertFalse(heuristics.looksLikeEvent(text))
    }

    @Test
    fun looksLikeEvent_withoutEventMarkers_returnsFalse() {
        val text = """
            Просто обычный пост
            Без каких-либо маркеров события
            Дата: 27.12.2025
        """.trimIndent()

        assertFalse(heuristics.looksLikeEvent(text))
    }

    @Test
    fun realEvent_iAGRI2026_shouldBeRecognizedAsEvent() {
        // Реальное событие с упоминанием "инновации"
        val text = """
iAGRI 2026

Дата проведения: 21.01.2026 - 23.01.2026. Начало 21.01.2026 в 08:00

Место проведения: Москва , Крокус Экспо

Организатором выступает российская выставочная компания «Агрос Экспо Групп». Среди партнеров мероприятия – ключевые отраслевые объединения в АПК, в сфере ИИ и робототехники.

В рамках выставочной экспозиции iAGRI ведущие российские и зарубежные компании представят передовые технологии и инновации в сфере АПК – автоматизацию производства и роботизированные системы, искусственный интеллект и цифровые платформы, биоинженерию, агрохимию, Big Data и точное земледелие. Эти инновационные решения способствуют повышению эффективности и устойчивости в растениеводстве и животноводстве, кормопроизводстве, селекции и ветеринарии, первичной и глубокой переработке сельхозпродукции, сельхозмашиностроении, логистике и многих других сферах.

https://iagri-expo.com/

IT мероприятия России — мы собрали все самые топовые IT-события в одном месте (https://t.me/iteventsrus)
        """.trimIndent()

        // Событие должно распознаваться, несмотря на слово "инновации" (не "ИНН")
        assertTrue(
            "iAGRI 2026 должен распознаваться как событие",
            heuristics.looksLikeEvent(text)
        )
    }

    @Test
    fun realEvent_DataFusionAwards2026_shouldBeRecognizedAsEvent() {
        // Реальное событие с упоминанием "инновации"
        val text = """
Data Fusion Awards 2026. Прием заявок

Дата проведения: 19.01.2026 - 20.01.2026. Начало 19.01.2026 в 10:00

Место проведения: Онлайн

Конкурс направлен на продвижение технологий работы с данными и ИИ.

Тематики конкурса:
- Математический аппарат ИИ
- Алгоритмы оптимизации
- Машинное обучение и глубокое обучение
- Нейроморфные вычисления
- Робототехника
- Объяснимый ИИ (Explainable AI)
- Другие смежные области

https://awards.data-fusion.ru/ai/

IT мероприятия России — мы собрали все самые топовые IT-события в одном месте (https://t.me/iteventsrus)
        """.trimIndent()

        // Событие должно распознаваться с маркерами "конкурс", "awards", "прием заявок"
        assertTrue(
            "Data Fusion Awards 2026 должен распознаваться как событие",
            heuristics.looksLikeEvent(text)
        )
    }

    @Test
    fun looksLikeEvent_withRealINN_returnsFalse() {
        // Проверяем, что реальный ИНН (как отдельное слово) все еще блокируется
        val text = """
            Крутая конференция
            Организатор: ООО "Рога и копыта"
            ИНН 1234567890
            Дата: 27.12.2025
        """.trimIndent()

        assertFalse(
            "Текст с ИНН организации не должен распознаваться как событие",
            heuristics.looksLikeEvent(text)
        )
    }
}