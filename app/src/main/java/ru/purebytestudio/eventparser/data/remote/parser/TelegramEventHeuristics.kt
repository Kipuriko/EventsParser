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
        val promoMarkers = listOf(
            "реклама",
            "инн",
            "ооо",
            "акция",
            "скидк",
            "распродаж",
            "промокод",
            "купите",
            "закажите"
        )
        if (promoMarkers.any { lower.contains(it) }) return false

        // должен быть хотя бы один маркер “мероприятия” (тип или явные слова)
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
            "челлендж"
        )
        return eventMarkers.any { lower.contains(it) }
    }
}
