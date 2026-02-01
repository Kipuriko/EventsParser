package ru.purebytestudio.eventparser.domain.util

import ru.purebytestudio.eventparser.domain.model.Event

/**
 * Нормализует идентификатор события для дедупликации между разными источниками.
 */
object EventIdentityNormalizer {
    private val noiseRegex = Regex(
        pattern = listOf(
            """[🎅🎄🎮🏆💰📣🎉🎤📅👥🪇🎙🟡🟢🔵🟣🟠🔴❤️]+""", // Emojis
            """[^\w\sа-яё]""", // Punctuation
            """\b(митап|meetup|конференция|conference|ивент|event|gamejam|геймджем|джем|jam)\b""", // Event types
            """\b(встреча|встречи|собрание|сбор|запускает|старт|стартует|квест|quest)\b""", // Event words
            """\b(lvl|level|уровень)\s*\d+\b""", // Levels
            """\b\d+\s*(руб|рублей|руб\.|₽)\b""", // Money
            """\b№\s*\d+\b""", // Numbers
            """\b(москв|санктпетербург|питер|казан|новосибирск|екатеринбург|краснодар|перм|тюмен|воронеж|нижний\s+новгород|уф|владивосток|томск|якутск|онлайн|online|gamedev|геймдев)\w*\b""", // Cities
            """\b(новогодний|новый|год|годовой|декабрьский|октябрьский|ноябрьский|январский|февральский|мартовский|апрельский|майский|июньский|июльский|августовский|сентябрьский)\b""", // Descriptive
            """\b(2023|2024|2025|2026)\b""", // Years
            """\b(правильный|правильного|правильным|правильной|лучший|лучшего|новый|старый|большой|маленький|крутой|классный|интересный|полезный|важный|главный)\b""" // Adjectives
        ).joinToString("|"),
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val whitespaceRegex = Regex("""\s+""")

    private val dateNoiseRegex = Regex(
        pattern = listOf(
            """\b\d{1,2}\s*(январ|феврал|март|апрел|май|июн|июл|август|сентябр|октябр|ноябр|декабр)\w*\b""",
            """\b(январ|феврал|март|апрел|май|июн|июл|август|сентябр|октябр|ноябр|декабр)\w*\s+\d{1,2}\b""",
            """\b\d{1,2}\.\d{1,2}\.\d{2,4}\b"""
        ).joinToString("|"),
        options = setOf(RegexOption.IGNORE_CASE)
    )

    fun fromEvent(event: Event): String =
        buildKey(event.title, event.dateTime?.toLocalDate()?.toString())

    fun fromPersisted(title: String, dateTime: String?): String =
        buildKey(title, dateTime?.take(10))

    private fun buildKey(title: String, dateString: String?): String {
        var normalizedTitle = title.lowercase()
            .replace(noiseRegex, " ")
            .replace(dateNoiseRegex, " ")

        normalizedTitle = normalizedTitle.replace(whitespaceRegex, " ").trim()

        val fallback = title.lowercase()
            .replace(noiseRegex, " ")
            .replace(whitespaceRegex, " ")
            .trim()

        normalizedTitle = if (normalizedTitle.length < 3) {
            fallback.splitToSequence(" ")
                .filter { it.length > 2 }
                .take(5)
                .joinToString(" ")
        } else {
            normalizedTitle.splitToSequence(" ")
                .filter { it.length > 2 }
                .take(5)
                .joinToString(" ")
        }

        if (normalizedTitle.length > 60) {
            normalizedTitle = normalizedTitle.take(60)
        }

        val dateKey = dateString ?: "no_date"
        return "${normalizedTitle}_${dateKey}"
    }
}