package ru.purebytestudio.eventparser.data.remote.parser

/**
 * Удаляет “артефакты” Telegram web-preview из уже извлечённого текста.
 *
 * Важно: логика чувствительна к реальным постам; менять аккуратно и с тестами.
 */
internal class TelegramTextSanitizer {
    fun sanitizeTelegramArtifacts(text: String): String {
        return text
            // убираем добавленные “поисковые” хвосты от Telegram
            .replace(
                TelegramParsingRegex.telegramQueryArtifactsRegex,
                ""
            )
            .replace(
                TelegramParsingRegex.telegramQueryUrlRegex,
                ""
            )
            // убираем “пустые” скобки, оставшиеся после чистки
            .replace(
                Regex("""\(\s*\)"""),
                ""
            )
            // нормализуем пробелы вокруг скобок
            .replace(
                Regex("""\s+\)"""),
                ")"
            )
            .replace(
                Regex("""\(\s+"""),
                "("
            )
            .replace(
                Regex("""[ \t]{2,}"""),
                " "
            )
            .lines()
            .joinToString("\n") { it.trimEnd() }
            .trim()
    }
}
