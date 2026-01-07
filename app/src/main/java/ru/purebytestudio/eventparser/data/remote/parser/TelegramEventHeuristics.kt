package ru.purebytestudio.eventparser.data.remote.parser

import java.time.LocalDateTime
import java.util.Locale

internal class TelegramEventHeuristics {
    private val ruLocale = Locale.forLanguageTag("ru")

    fun isInAllowedWindow(start: LocalDateTime): Boolean {
        val now = LocalDateTime.now()
        return !start.isBefore(now.minusMonths(3)) && !start.isAfter(now.plusMonths(3))
    }

    fun looksLikeEvent(text: String): Boolean {
        val lower = text.lowercase(ruLocale)

        // жёсткий стоп-лист для рекламы/промо
        // Важно: некоторые маркеры требуют более точного сопоставления (word boundaries),
        // чтобы не блокировать валидные слова типа "инновации"
        val promoMarkers = listOf(
            "реклама",
            "ооо",
            "акция",
            "скидк",
            "распродаж",
            "промокод",
            "купите",
            "закажите"
        )
        if (promoMarkers.any { lower.contains(it) }) return false

        // Специальные проверки для "инн" - только если это отдельное слово (ИНН организации)
        // Не блокируем "инновации", "инновационный", "машинный" и т.д.
        if (Regex("""\bинн\b""").find(lower) != null) return false

        // должен быть хотя бы один маркер "мероприятия" (тип или явные слова)
        val eventMarkers = listOf(
            "митап",
            "конференц",
            "воркшоп",
            "хакатон",
            "геймджем",
            "gamejam",
            "вебинар",
            "стрим",
            "турнир",
            "чемпионат",
            "соревнован",
            "встреча",
            "форум",
            "саммит",
            "лекция",
            "фестиваль",
            "дайджест",
            "афиша",
            "челлендж",
            "выстав",
            "конкурс",
            "awards",
            "прием заявок",
            "регистрац",
            "экспо"
        )
        return eventMarkers.any { lower.contains(it) }
    }
}