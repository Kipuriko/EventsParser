package ru.purebytestudio.eventparser.data.remote.parser

/**
 * Извлечение заголовка (title) события из текста Telegram-поста.
 *
 * Важно: заголовок часто “шумный” (эмодзи/ссылки/хэштеги), поэтому здесь много эвристик.
 */
internal class TelegramTitleExtractor {
    fun extractTitle(text: String): String {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        for (rawLine in lines) {
            if (isHashtagsLine(rawLine)) continue
            val cleaned = cleanTitleLine(rawLine)
            val candidate = pickNamePart(cleaned)
            if (candidate.length >= 4) return candidate.take(120)
        }

        return ""
    }

    fun isHashtagsLine(line: String): Boolean {
        val trimmed = line.trim()
        if (!trimmed.startsWith("#")) return false
        // Строка из одних #тегов и пробелов
        return trimmed.split(" ")
            .filter { it.isNotBlank() }
            .all { it.startsWith("#") }
    }

    fun cleanTitleLine(line: String): String {
        var s = line
        // Важно: сначала вырезаем "(https://...)", иначе после удаления URL получим "()" и потеряем контекст
        s = s.replace(
            TelegramParsingRegex.bracketUrlRegex,
            ""
        )
        s = s.replace(
            TelegramParsingRegex.urlRegex,
            ""
        ).replace(
            TelegramParsingRegex.tmeRegex,
            ""
        )
        s = s.replace(
            TelegramParsingRegex.telegramQueryArtifactsRegex,
            ""
        )
        s = s.replace(
            Regex(
                """\?q=%23[^\s)]+""",
                RegexOption.IGNORE_CASE
            ),
            ""
        )
        s = s.replace(
            TelegramParsingRegex.emojiLikeRegex,
            ""
        )
        // Убираем пустые/висячие скобки, которые часто остаются после чистки ссылок
        s = s.replace(
            Regex("""\(\s*\)"""),
            ""
        )
        s = s.replace(
            Regex("""\(\s*$"""),
            ""
        )
        s = s.replace(
            Regex("""^\s*\)"""),
            ""
        )

        s = s.trim().trim(
            '—',
            '-',
            ':',
            '–',
            '•',
            '·',
            '➖'
        )
        // Убираем остатки двойных пробелов
        s = s.replace(
            Regex("""\s+"""),
            " "
        ).trim()
        // Убираем висячие скобки по краям
        s = s.replace(
            Regex("""^\(+"""),
            ""
        ).replace(
            Regex("""\)+$"""),
            ""
        ).trim()
        return s
    }

    fun pickNamePart(cleanedLine: String): String {
        // Частый кейс: "Название — подзаголовок"
        val separators = listOf(
            " — ",
            " - ",
            " —",
            " -",
            "—",
            "-"
        )
        for (sep in separators) {
            val idx = cleanedLine.indexOf(sep)
            if (idx > 0) {
                val left = cleanedLine.take(idx).trim()
                if (left.length in 4..80 && left.any { it.isLetterOrDigit() }) return left
            }
        }
        return cleanedLine
    }
}
