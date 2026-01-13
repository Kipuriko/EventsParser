package ru.purebytestudio.eventparser.domain.util

/**
 * Утилита для вычисления сходства между строками.
 */
object TextSimilarity {
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
}
