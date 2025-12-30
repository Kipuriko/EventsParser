package ru.purebytestudio.eventparser.domain.util

import ru.purebytestudio.eventparser.domain.model.Event

/**
 * Нормализует идентификатор события для дедупликации между разными источниками.
 *
 * Логика нормализации намеренно агрессивная: она удаляет эмодзи, даты, города,
 * маркеры цен и общие описательные слова, чтобы два анонса, описывающие
 * одно и то же событие, но различающиеся форматированием, имели одинаковый ключ.
 */
object EventIdentityNormalizer {
    private val emojiRegex = Regex("""[🎅🎄🎮🏆💰📣🎉🎤📅👥🪇🎙🟡🟢🔵🟣🟠🔴❤️]+""")
    private val whitespaceRegex = Regex("""\s+""")
    private val punctuationRegex = Regex("""[^\w\sа-яё]""")
    private val monthWithDayRegex =
        Regex("""\b\d{1,2}\s*(январ|феврал|март|апрел|май|июн|июл|август|сентябр|октябр|ноябр|декабр)\w*\b""")
    private val dayWithMonthRegex =
        Regex("""\b(январ|феврал|март|апрел|май|июн|июл|август|сентябр|октябр|ноябр|декабр)\w*\s+\d{1,2}\b""")
    private val numericDateRegex = Regex("""\b\d{1,2}\.\d{1,2}\.\d{2,4}\b""")
    private val eventTypeRegex =
        Regex("""\b(митап|meetup|конференция|conference|ивент|event|gamejam|геймджем|джем|jam)\b""")
    private val eventWordRegex =
        Regex("""\b(встреча|встречи|собрание|сбор|запускает|старт|стартует|квест|quest)\b""")
    private val numberPatterns =
        listOf(
            Regex("""\b(lvl|level|уровень)\s*\d+\b"""),
            Regex("""\b\d+\s*(руб|рублей|руб\.|₽)\b"""),
            Regex("""\b№\s*\d+\b""")
        )
    private val cityRegex =
        Regex("""\b(москв|санктпетербург|питер|казан|новосибирск|екатеринбург|краснодар|перм|тюмен|воронеж|нижний\s+новгород|уф|владивосток|томск|якутск|онлайн|online|gamedev|геймдев)\w*\b""")
    private val descriptiveRegex =
        Regex("""\b(новогодний|новый|год|годовой|декабрьский|октябрьский|ноябрьский|январский|февральский|мартовский|апрельский|майский|июньский|июльский|августовский|сентябрьский)\b""")
    private val yearRegex = Regex("""\b(2023|2024|2025|2026)\b""")

    /**
     * Создает нормализованный ключ из объекта события.
     */
    fun fromEvent(event: Event): String =
        buildKey(
            event.title,
            event.dateTime?.toLocalDate()?.toString()
        )

    /**
     * Создает нормализованный ключ из сохраненных данных (заголовок и строка даты).
     * @param dateTime Ожидается дата в формате ISO-8601 (2023-12-31T10:00:00) или аналогичном, начинающемся с даты.
     */
    fun fromPersisted(
        title: String,
        dateTime: String?
    ): String =
        buildKey(
            title,
            dateTime?.take(10)
        )

    private fun buildKey(
        title: String,
        dateString: String?
    ): String {
        var normalizedTitle = title.lowercase()
            .replace(
                emojiRegex,
                ""
            )
            .replace(
                whitespaceRegex,
                " "
            )
            .replace(
                punctuationRegex,
                ""
            )
            .trim()

        normalizedTitle = normalizedTitle.replace(
            monthWithDayRegex,
            ""
        )
            .replace(
                dayWithMonthRegex,
                ""
            )
            .replace(
                numericDateRegex,
                ""
            )
            .replace(
                eventTypeRegex,
                ""
            )
            .replace(
                eventWordRegex,
                ""
            )
            .replace(
                cityRegex,
                ""
            )
            .replace(
                descriptiveRegex,
                ""
            )
            .replace(
                yearRegex,
                ""
            )

        numberPatterns.forEach { pattern ->
            normalizedTitle = normalizedTitle.replace(
                pattern,
                ""
            )
        }

        normalizedTitle = normalizedTitle.replace(
            whitespaceRegex,
            " "
        ).trim()

        val fallback =
            title.lowercase().replace(
                emojiRegex,
                ""
            ).replace(
                punctuationRegex,
                ""
            )
                .replace(
                    whitespaceRegex,
                    " "
                ).trim()

        normalizedTitle = if (normalizedTitle.length < 3) {
            fallback.split(" ").filter { it.length > 2 }.take(5).joinToString(" ").take(60)
        } else {
            normalizedTitle.split(" ").filter { it.length > 2 }.take(5).joinToString(" ").take(60)
        }

        val dateKey = dateString ?: "no_date"
        return "${normalizedTitle}_${dateKey}"
    }
}