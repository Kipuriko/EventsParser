package ru.purebytestudio.eventparser.di

import kotlinx.serialization.json.Json
import org.koin.dsl.module

val serializationModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = true
        }
    }
}
