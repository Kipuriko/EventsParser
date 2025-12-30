package ru.purebytestudio.eventparser.data.io

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Чтение/запись текстовых документов через Storage Access Framework (content:// Uri).
 */
class DocumentTextStorage(private val context: Context) {
    suspend fun writeText(
        uri: Uri,
        text: String
    ) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(
            uri,
            "wt"
        )?.use { os ->
            OutputStreamWriter(
                os,
                Charsets.UTF_8
            ).use { writer ->
                writer.write(text)
            }
        } ?: error("Не удалось открыть OutputStream для записи")
    }

    suspend fun readText(uri: Uri): String = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { ins ->
            BufferedReader(
                InputStreamReader(
                    ins,
                    Charsets.UTF_8
                )
            ).use { it.readText() }
        } ?: error("Не удалось открыть InputStream для чтения")
    }
}
