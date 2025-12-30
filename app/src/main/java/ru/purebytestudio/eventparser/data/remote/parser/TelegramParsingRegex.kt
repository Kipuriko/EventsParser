package ru.purebytestudio.eventparser.data.remote.parser

/**
 * Единое место для regex-паттернов, используемых при парсинге Telegram web-preview.
 *
 * Важно: эти regex'ы являются частью «контракта» качества парсинга; любые изменения должны
 * сопровождаться unit-тестами-ограждениями, чтобы не вносить незаметные регрессии.
 */
internal object TelegramParsingRegex {
    private const val MONTHS_REGEX =
        "января|февраля|марта|апреля|мая|июня|июля|августа|сентября|октября|ноября|декабря|январь|февраль|март|апрель|май|июнь|июль|август|сентябрь|октябрь|ноябрь|декабрь"

    val timeRegex: Regex = Regex("""\b(\d{1,2})[:.](\d{2})\b""")

    val numericDateRegex: Regex = Regex("""\b(\d{1,2})\.(\d{1,2})\.(\d{2,4})\b""")

    val monthDateRegex: Regex =
        Regex(
            """\b(\d{1,2})\s+($MONTHS_REGEX)\s*(\d{4})?\b""",
            RegexOption.IGNORE_CASE
        )

    val monthDateRangeRegex: Regex =
        Regex(
            """\b(\d{1,2})\s*[–-]\s*(\d{1,2})\s+($MONTHS_REGEX)\s*(\d{4})?\b""",
            RegexOption.IGNORE_CASE
        )

    val emojiLikeRegex: Regex = Regex("""[\p{So}\p{Cn}]""")
    val urlRegex: Regex = Regex(
        """https?://\S+""",
        RegexOption.IGNORE_CASE
    )
    val tmeRegex: Regex = Regex(
        """\bt\.me/\S+""",
        RegexOption.IGNORE_CASE
    )
    val bracketUrlRegex: Regex = Regex(
        """\([^)]*https?://[^)]*\)""",
        RegexOption.IGNORE_CASE
    )

    val telegramQueryArtifactsRegex: Regex = Regex(
        """\(\s*\?q=[^)]+\)""",
        RegexOption.IGNORE_CASE
    )

    val telegramQueryUrlRegex: Regex =
        Regex(
            """https?://t\.me/s/[^)\s]+\?q=%23[^)\s]+""",
            RegexOption.IGNORE_CASE
        )

    val directImageUrlRegex: Regex =
        Regex("""(?i)^https?://\S+\.(?:jpg|jpeg|png|webp|gif)(?:\?\S+)?$""")
}
