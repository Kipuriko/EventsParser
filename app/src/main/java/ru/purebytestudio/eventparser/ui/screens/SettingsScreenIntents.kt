package ru.purebytestudio.eventparser.ui.screens

import android.content.ActivityNotFoundException
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

internal fun openEmailComposer(
    context: Context,
    email: String,
    subject: String,
    body: String,
    chooserTitle: String
) {
    // Gmail часто игнорирует EXTRA_* для ACTION_SENDTO (mailto:),
    // поэтому сначала пытаемся открыть именно Gmail через ACTION_SEND.
    val gmailPackage = "com.google.android.gm"

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "message/rfc822"
        putExtra(
            Intent.EXTRA_EMAIL,
            arrayOf(email)
        )
        putExtra(
            Intent.EXTRA_SUBJECT,
            subject
        )
        putExtra(
            Intent.EXTRA_TEXT,
            body
        )
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val gmailIntent = Intent(sendIntent).apply { setPackage(gmailPackage) }

    try {
        context.startActivity(gmailIntent)
        return
    } catch (_: ActivityNotFoundException) {
        // Gmail не установлен/недоступен — fallback ниже.
    }

    // Fallback: любой почтовый клиент
    val sendToIntent = Intent(Intent.ACTION_SENDTO).apply {
        data = "mailto:".toUri()
        putExtra(
            Intent.EXTRA_EMAIL,
            arrayOf(email)
        )
        putExtra(
            Intent.EXTRA_SUBJECT,
            subject
        )
        putExtra(
            Intent.EXTRA_TEXT,
            body
        )
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val chooser = Intent.createChooser(
        sendToIntent,
        chooserTitle
    )
    runCatching { context.startActivity(chooser) }
}