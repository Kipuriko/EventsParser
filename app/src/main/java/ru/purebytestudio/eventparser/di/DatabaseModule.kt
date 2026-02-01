package ru.purebytestudio.eventparser.di

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ru.purebytestudio.eventparser.data.local.database.AppDatabase
import ru.purebytestudio.eventparser.data.service.EventDeduplicationService

val databaseModule = module {
    single {
        val builder = Room.databaseBuilder(
            context = androidContext(),
            klass = AppDatabase::class.java,
            name = AppDatabase.DATABASE_NAME
        )
        builder.addMigrations(AppDatabase.MIGRATION_3_4)
        builder.build()
    }

    single { get<AppDatabase>().eventDao() }
    singleOf(::EventDeduplicationService)
}