package ru.purebytestudio.eventparser.data.export

import android.content.Context
import androidx.core.content.FileProvider
import ru.purebytestudio.eventparser.domain.model.Event
import ru.purebytestudio.eventparser.platform.TimeProvider
import java.io.File
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Экспорт события в .ics (iCalendar) файл.
 *
 * Файл создаётся в cacheDir и возвращается как content:// Uri через FileProvider.
 */
class IcsExporter(
    private val context: Context,
    private val timeProvider: TimeProvider
) {
    fun exportToCache(event: Event): android.net.Uri {
        val icsText = buildIcs(event)

        val dir = File(
            context.cacheDir,
            "exports"
        ).apply { mkdirs() }
        val safeName = "event_${
            event.id.replace(
                Regex("""[^\w\-]+"""),
                "_"
            )
        }.ics"
        val file = File(
            dir,
            safeName
        )
        file.writeText(
            icsText,
            Charsets.UTF_8
        )

        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(
            context,
            authority,
            file
        )
    }

    private fun buildIcs(event: Event): String {
        val uid = "${event.id}@eventparser"
        val dtStamp = formatUtc(timeProvider.now())

        val start = event.dateTime
        requireNotNull(start) { "event.dateTime is required for .ics export" }

        val end = event.endDateTime ?: start.plusHours(2)

        val summary = escapeIcs(event.title)
        val description = escapeIcs(buildString {
            append(event.description)
            append("\n\n")
            append(event.url)
        })
        val location = event.location?.let { escapeIcs(it) }
        val url = escapeIcs(event.url)

        // RFC 5545 expects CRLF line endings.
        val crlf = "\r\n"
        return buildString {
            append("BEGIN:VCALENDAR").append(crlf)
            append("VERSION:2.0").append(crlf)
            append("PRODID:-//PureByte Studio//EventParser//RU").append(crlf)
            append("CALSCALE:GREGORIAN").append(crlf)
            append("METHOD:PUBLISH").append(crlf)
            append("BEGIN:VEVENT").append(crlf)
            append("UID:").append(uid).append(crlf)
            append("DTSTAMP:").append(dtStamp).append(crlf)
            append("DTSTART:").append(formatUtc(start)).append(crlf)
            append("DTEND:").append(formatUtc(end)).append(crlf)
            append("SUMMARY:").append(summary).append(crlf)
            append("DESCRIPTION:").append(description).append(crlf)
            location?.let { append("LOCATION:").append(it).append(crlf) }
            append("URL:").append(url).append(crlf)
            append("END:VEVENT").append(crlf)
            append("END:VCALENDAR").append(crlf)
        }
    }

    private fun formatUtc(dateTime: java.time.LocalDateTime): String {
        val instant = dateTime.atZone(ZoneId.systemDefault()).toInstant()
        val utc = instant.atOffset(ZoneOffset.UTC)
        return utc.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))
    }

    private fun escapeIcs(value: String): String {
        // Минимально необходимое экранирование текстовых значений по RFC 5545.
        return value
            .replace(
                "\\",
                "\\\\"
            )
            .replace(
                ";",
                "\\;"
            )
            .replace(
                ",",
                "\\,"
            )
            .replace(
                "\r\n",
                "\n"
            )
            .replace(
                "\n",
                "\\n"
            )
    }
}

