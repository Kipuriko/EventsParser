package ru.purebytestudio.eventparser.data.remote

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import ru.purebytestudio.eventparser.data.remote.parser.EventParser
import ru.purebytestudio.eventparser.domain.model.Event
import ru.purebytestudio.eventparser.domain.model.EventCategory
import timber.log.Timber

/**
 * Агрегатор, объединяющий результаты от множества парсеров событий.
 *
 * Поведение:
 * - запускает парсеры параллельно (каждый парсер отвечает за свой источник);
 * - ошибки отдельного парсера не «роняют» общий результат — в этом случае источник просто даёт пустой список;
 * - на выходе выполняется `distinctBy(id)`, чтобы не получить одинаковое событие дважды.
 *
 * Почему это важно:
 * - разные источники могут пересекаться по событиям,
 * - парсеры могут временно «ломаться» из‑за изменений вёрстки/доступности страниц.
 */
class EventParserAggregator(private val parsers: List<EventParser>) {
    suspend fun parseAllEvents(categories: List<EventCategory>): List<Event> = coroutineScope {
        val deferredResults = parsers.map { parser ->
            async {
                val name = parser.source.name
                try {
                    Timber.d(message = "Парсинг $name для категорий $categories")
                    val result = parser.parseEvents(categories).getOrElse { emptyList() }
                    Timber.d(message = "Спарсено ${result.size} событий из $name")
                    result
                } catch (e: Exception) {
                    Timber.e(
                        t = e,
                        message = "Парсер $name упал с ошибкой"
                    )
                    emptyList()
                }
            }
        }

        deferredResults.awaitAll().flatten().distinctBy { it.id }
    }
}