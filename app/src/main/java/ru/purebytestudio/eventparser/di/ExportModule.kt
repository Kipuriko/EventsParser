package ru.purebytestudio.eventparser.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ru.purebytestudio.eventparser.data.export.IcsExporter
import ru.purebytestudio.eventparser.data.io.DocumentTextStorage

val exportModule = module {
    singleOf(::DocumentTextStorage)
    singleOf(::IcsExporter)
}
