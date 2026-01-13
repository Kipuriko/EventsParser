package ru.purebytestudio.eventparser.ui.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import ru.purebytestudio.eventparser.R
import ru.purebytestudio.eventparser.ui.components.LocalNotifier

/**
 * Android 13+ (API 33+) требует runtime-разрешение POST_NOTIFICATIONS.
 * Этот composable запрашивает его один раз (по флагу shouldRequest) и не спамит диалогом каждый запуск.
 */
@Composable
fun PostNotificationsPermissionRequester(
    shouldRequest: Boolean,
    markRequested: suspend () -> Unit
) {
    val context = LocalContext.current
    val notifier = LocalNotifier.current
    val permissionDeniedText = stringResource(R.string.notifications_permission_denied)
    val permissionDeniedTextState = rememberUpdatedState(permissionDeniedText)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            notifier.showInfo(permissionDeniedTextState.value)
        }
    }

    LaunchedEffect(shouldRequest) {
        if (!shouldRequest) return@LaunchedEffect

        // На API < 33 это разрешение не нужно, но флаг "уже спрашивали" логично выставить,
        // чтобы в будущем не пытаться "просить" его на каждом запуске.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            markRequested()
            return@LaunchedEffect
        }

        val permission = Manifest.permission.POST_NOTIFICATIONS
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        // Помечаем, что попытка запроса уже была, чтобы не раздражать пользователя повторными диалогами.
        markRequested()

        if (!alreadyGranted) {
            launcher.launch(permission)
        }
    }
}