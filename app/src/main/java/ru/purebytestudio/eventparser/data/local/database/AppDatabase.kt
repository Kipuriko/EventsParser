package ru.purebytestudio.eventparser.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.purebytestudio.eventparser.data.local.dao.EventDao
import ru.purebytestudio.eventparser.data.local.entity.EventEntity
import ru.purebytestudio.eventparser.data.local.entity.EventEntityConverters

@Database(
    entities = [EventEntity::class],
    version = 4,
    exportSchema = true,
)
@TypeConverters(EventEntityConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        const val DATABASE_NAME = "event_parser_db"

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `events_new` (
                        `id` TEXT NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `description` TEXT NOT NULL, 
                        `imageUrl` TEXT, 
                        `dateTime` TEXT, 
                        `endDateTime` TEXT, 
                        `location` TEXT, 
                        `isOnline` INTEGER NOT NULL, 
                        `format` TEXT, 
                        `url` TEXT NOT NULL, 
                        `source` TEXT NOT NULL, 
                        `category` TEXT NOT NULL, 
                        `eventType` TEXT NOT NULL, 
                        `organizer` TEXT, 
                        `price` TEXT, 
                        `isFree` INTEGER NOT NULL, 
                        `prizeFund` TEXT, 
                        `tags` TEXT NOT NULL, 
                        `isFavorite` INTEGER NOT NULL, 
                        `registrationDeadline` TEXT, 
                        `maxParticipants` INTEGER, 
                        `hasSpecificTime` INTEGER NOT NULL, 
                        `localImagePath` TEXT, 
                        `createdAt` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                val cursor = db.query("SELECT * FROM events LIMIT 0")
                val existingColumns = cursor.columnNames.toHashSet()
                cursor.close()

                val targetColumns = listOf(
                    "id", "title", "description", "imageUrl", "dateTime", "endDateTime", "location", 
                    "isOnline", "format", "url", "source", "category", "eventType", "organizer", 
                    "price", "isFree", "prizeFund", "tags", "isFavorite", "registrationDeadline", 
                    "maxParticipants", "hasSpecificTime", "localImagePath", "createdAt"
                )

                val selectColumns = targetColumns.joinToString(", ") { col ->
                    if (existingColumns.contains(col)) "`$col`" else "NULL"
                }

                val insertSql = """
                    INSERT INTO events_new (${targetColumns.joinToString(", ") { "`$it`" }})
                    SELECT $selectColumns FROM events
                """
                
                db.execSQL(insertSql)
                db.execSQL("DROP TABLE events")
                db.execSQL("ALTER TABLE events_new RENAME TO events")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_events_category` ON `events` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_events_source` ON `events` (`source`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_events_eventType` ON `events` (`eventType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_events_isFavorite` ON `events` (`isFavorite`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_events_dateTime` ON `events` (`dateTime`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_events_isFree` ON `events` (`isFree`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_events_title_description` ON `events` (`title`, `description`)")
            }
        }
    }
}