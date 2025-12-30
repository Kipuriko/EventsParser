package ru.purebytestudio.eventparser.data.crash

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Базовый обработчик критических ошибок приложения
 * Логирует ошибки в файл и системный лог
 */
class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val dateFormat = SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss",
        Locale.getDefault()
    )

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(
        thread: Thread,
        exception: Throwable
    ) {
        try {
            logCrash(exception)
        } catch (e: Exception) {
            Timber.e(
                t = e,
                message = "Failed to log crash"
            )
        } finally {
            // Вызываем стандартный обработчик
            defaultHandler?.uncaughtException(
                thread,
                exception
            )
        }
    }

    private fun logCrash(exception: Throwable) {
        val crashLog = buildCrashLog(exception)

        // Логируем в Timber
        Timber.e(
            t = exception,
            message = "Uncaught exception occurred"
        )

        // Сохраняем в файл (опционально, для отладки)
        saveCrashToFile(crashLog)

        // Дублируем лог в системный лог через Timber
        Timber.tag("EventParser").e(
            exception,
            "CRASH: ${exception.message}"
        )
    }

    private fun buildCrashLog(exception: Throwable): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("=== CRASH REPORT ===\n")
        stringBuilder.append("Time: ${dateFormat.format(Date())}\n")
        stringBuilder.append("App Version: ${getAppVersion()}\n")
        stringBuilder.append("Android Version: ${android.os.Build.VERSION.RELEASE}\n")
        stringBuilder.append("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n")
        stringBuilder.append("\n")
        stringBuilder.append("Exception: ${exception.javaClass.name}\n")
        stringBuilder.append("Message: ${exception.message}\n")
        stringBuilder.append("\n")
        stringBuilder.append("Stack Trace:\n")

        val sw = java.io.StringWriter()
        val pw = PrintWriter(sw)
        exception.printStackTrace(pw)
        stringBuilder.append(sw.toString())
        stringBuilder.append("\n=== END CRASH REPORT ===\n\n")

        return stringBuilder.toString()
    }

    private fun saveCrashToFile(crashLog: String) {
        try {
            val crashDir = File(
                context.filesDir,
                "crashes"
            )
            if (!crashDir.exists()) {
                crashDir.mkdirs()
            }

            val crashFile = File(
                crashDir,
                "crash_${System.currentTimeMillis()}.txt"
            )
            FileWriter(crashFile).use { writer ->
                writer.write(crashLog)
            }

            // Ограничиваем количество файлов (храним последние 10)
            cleanupOldCrashFiles(crashDir)
        } catch (e: Exception) {
            Timber.e(
                e,
                "Failed to save crash log to file"
            )
        }
    }

    private fun cleanupOldCrashFiles(crashDir: File) {
        try {
            val files = crashDir.listFiles()?.sortedByDescending { it.lastModified() }
            files?.let {
                if (it.size > 10) {
                    it.drop(10).forEach { file ->
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(
                t = e,
                message = "Failed to cleanup old crash files"
            )
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                0
            )
            val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
            "${packageInfo.versionName} ($versionCode)"
        } catch (e: Exception) {
            Timber.w(
                t = e,
                message = "Failed to get app version"
            )
            "Unknown"
        }
    }
}