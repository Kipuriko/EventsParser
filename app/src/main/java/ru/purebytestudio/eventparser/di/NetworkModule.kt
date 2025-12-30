package ru.purebytestudio.eventparser.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.purebytestudio.eventparser.data.remote.EventParserAggregator
import ru.purebytestudio.eventparser.data.remote.parser.EventParser
import ru.purebytestudio.eventparser.data.remote.parser.TelegramChannelProvider
import ru.purebytestudio.eventparser.data.remote.parser.TelegramDateExtractor
import ru.purebytestudio.eventparser.data.remote.parser.TelegramDigestParser
import ru.purebytestudio.eventparser.data.remote.parser.TelegramEventAttributesExtractor
import ru.purebytestudio.eventparser.data.remote.parser.TelegramEventHeuristics
import ru.purebytestudio.eventparser.data.remote.parser.TelegramImageExtractor
import ru.purebytestudio.eventparser.data.remote.parser.TelegramMessageHtmlTextExtractor
import ru.purebytestudio.eventparser.data.remote.parser.TelegramMessagePipeline
import ru.purebytestudio.eventparser.data.remote.parser.TelegramParser
import ru.purebytestudio.eventparser.data.remote.parser.TelegramTextSanitizer
import ru.purebytestudio.eventparser.data.remote.parser.TelegramTitleExtractor

/**
 * Модуль удалённых источников данных (Remote).
 *
 * Сейчас единственный источник — Telegram web preview (`t.me/s/...`), который парсится через Jsoup.
 * Модуль регистрирует:
 * - мелкие «шаги» парсинга (экстракторы/санитайзеры/эвристики),
 * - [TelegramMessagePipeline] — сборку этих шагов в единый процесс,
 * - [TelegramParser] как реализацию [EventParser],
 * - [EventParserAggregator] — агрегатор всех парсеров (на будущее, если появятся новые источники).
 */
val networkModule = module {
    singleOf(::TelegramChannelProvider)
    singleOf(::TelegramDateExtractor)
    singleOf(::TelegramMessageHtmlTextExtractor)
    singleOf(::TelegramTextSanitizer)
    singleOf(::TelegramTitleExtractor)
    singleOf(::TelegramEventHeuristics)
    singleOf(::TelegramEventAttributesExtractor)
    singleOf(::TelegramImageExtractor)
    singleOf(::TelegramDigestParser)
    singleOf(::TelegramMessagePipeline)
    singleOf(::TelegramParser) bind EventParser::class

    single {
        EventParserAggregator(
            parsers = getAll()
        )
    }
}