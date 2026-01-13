package ru.purebytestudio.eventparser.domain.util

/**
 * Утилита для вычисления сходства между строками.
 */
object TextSimilarity {
    // Ключевые слова, указывающие на название события
    private val eventKeywords = setOf(
        "conf",
        "conference",
        "конференция",
        "meetup",
        "митап",
        "summit",
        "саммит",
        "hackathon",
        "хакатон",
        "fest",
        "festival",
        "фестиваль",
        "forum",
        "форум",
        "days",
        "week",
        "неделя"
    )
    /**
     * Вычисляет коэффициент Жаккара (Jaccard similarity) между двумя строками.
     * Возвращает значение от 0.0 (совершенно различные) до 1.0 (идентичные).
     */
    fun jaccardSimilarity(
        str1: String,
        str2: String
    ): Double {
        val words1 = str1.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }.toSet()
        val words2 = str2.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }.toSet()

        if (words1.isEmpty() && words2.isEmpty()) return 1.0
        if (words1.isEmpty() || words2.isEmpty()) return 0.0

        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size

        return intersection.toDouble() / union.toDouble()
    }

    /**
     * Вычисляет расстояние Левенштейна между двумя строками.
     * Возвращает количество операций (вставка, удаление, замена) для преобразования одной строки в другую.
     */
    fun levenshteinDistance(
        str1: String,
        str2: String
    ): Int {
        val m = str1.length
        val n = str2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) {
            dp[i][0] = i
        }

        for (j in 0..n) {
            dp[0][j] = j
        }

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // удаление
                    dp[i][j - 1] + 1,      // вставка
                    dp[i - 1][j - 1] + cost // замена
                )
            }
        }

        return dp[m][n]
    }

    /**
     * Нормализованное сходство на основе расстояния Левенштейна.
     * Возвращает значение от 0.0 (совершенно различные) до 1.0 (идентичные).
     */
    fun levenshteinSimilarity(
        str1: String,
        str2: String
    ): Double {
        val distance = levenshteinDistance(
            str1.lowercase(),
            str2.lowercase()
        )
        val maxLength = maxOf(
            str1.length,
            str2.length
        )

        if (maxLength == 0) return 1.0

        return 1.0 - (distance.toDouble() / maxLength.toDouble())
    }

    /**
     * Проверяет, являются ли две строки похожими на основе нормализованных ключей.
     *
     * @param title1 Первый заголовок
     * @param title2 Второй заголовок
     * @param threshold Порог сходства (от 0.0 до 1.0), по умолчанию 0.8
     * @return true, если строки похожи
     */
    fun areSimilar(
        title1: String,
        title2: String,
        threshold: Double = 0.8
    ): Boolean {
        // Нормализуем строки: lowercase, убираем лишние пробелы
        val normalized1 = title1.lowercase().trim().replace(
            Regex("\\s+"),
            " "
        )
        val normalized2 = title2.lowercase().trim().replace(
            Regex("\\s+"),
            " "
        )

        // Если строки идентичны после нормализации
        if (normalized1 == normalized2) return true

        // Проверяем наличие общих ключевых названий событий
        if (hasCommonEventNames(
                title1,
                title2
            )
        ) {
            return true
        }

        // Используем комбинированный подход: Жаккар + Левенштейн
        val jaccardScore = jaccardSimilarity(
            normalized1,
            normalized2
        )
        val levenshteinScore = levenshteinSimilarity(
            normalized1,
            normalized2
        )

        // Берём среднее двух метрик
        val combinedScore = (jaccardScore + levenshteinScore) / 2.0

        return combinedScore >= threshold
    }

    /**
     * Проверяет, есть ли в двух текстах общие ключевые названия событий.
     */
    fun hasCommonEventNames(
        text1: String,
        text2: String
    ): Boolean {
        val names1 = extractEventNames(text1)
        if (names1.isEmpty()) return false

        val names2 = extractEventNames(text2)
        if (names2.isEmpty()) return false

        val normalizedNames1 = names1.mapTo(HashSet()) { it.lowercase().trim() }

        return names2.any { name ->
            val normalized = name.lowercase().trim()
            normalized.length > 5 && normalized in normalizedNames1
        }
    }

    /**
     * Извлекает потенциальные названия событий из текста.
     * Оптимизированный алгоритм на основе n-грамм и ключевых слов.
     */
    private fun extractEventNames(text: String): List<String> {
        val names = mutableSetOf<String>()
        val words = text.split(Regex("""[\s\n]+""")).filter { it.isNotEmpty() }

        if (words.isEmpty()) return emptyList()

        // Предварительная проверка: есть ли вообще ключевые слова
        val textLower = text.lowercase()
        val hasAnyKeyword = eventKeywords.any { textLower.contains(it) }

        if (hasAnyKeyword) {
            // Ищем n-граммы (2-5 слов) с ключевыми словами
            for (i in words.indices) {
                for (n in 2..5) {
                    if (i + n > words.size) break

                    val ngram = words.subList(
                        i,
                        i + n
                    )

                    // Ранний выход: проверяем заглавную букву
                    if (ngram[0].firstOrNull()?.isUpperCase() != true) continue

                    // Проверяем наличие ключевого слова
                    val ngramLower = ngram.joinToString(" ").lowercase()
                    val hasKeyword = eventKeywords.any { ngramLower.contains(it) }

                    if (hasKeyword) {
                        val ngramText = ngram.joinToString(" ")
                        if (ngramText.length in 5..60) {
                            names.add(
                                ngramText.trimEnd(
                                    '.',
                                    ',',
                                    '!',
                                    '?',
                                    ':',
                                    ';'
                                )
                            )
                        }
                    }
                }
            }
        }

        // Ищем составные названия (KotlinConf, DevOps)
        Regex("""[A-ZА-Я][a-zа-я]+[A-ZА-Я][A-Za-zА-Яа-я0-9]+""")
            .findAll(text)
            .mapNotNull { it.value.takeIf { name -> name.length in 5..40 } }
            .forEach { names.add(it) }

        return names.toList()
    }
}
