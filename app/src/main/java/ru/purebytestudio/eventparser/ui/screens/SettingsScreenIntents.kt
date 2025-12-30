package ru.purebytestudio.eventparser.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

internal fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(
            Settings.EXTRA_APP_PACKAGE,
            context.packageName
        )
    }
    runCatching { context.startActivity(intent) }
        .onFailure { openAppDetailsSettings(context) }
}

internal fun openAppDetailsSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:${context.packageName}".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

internal fun openUrlSafely(
    context: Context,
    url: String
) {
    val intent = Intent(
        Intent.ACTION_VIEW,
        url.toUri()
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
