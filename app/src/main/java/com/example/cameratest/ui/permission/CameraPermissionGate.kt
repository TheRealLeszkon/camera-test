package com.example.cameratest.ui.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.cameratest.R

/**
 * Gates [content] behind the CAMERA runtime permission.
 *
 * Declaring the permission in the manifest is not enough: since API 23 the user grants it at
 * runtime and can revoke it at any time. Until it is granted, [content] is never composed —
 * so downstream camera code can assume permission exists.
 *
 * Handles the three real-world outcomes:
 * - **granted** → render [content];
 * - **denied, can ask again** → explain why, offer to re-request;
 * - **permanently denied** (user chose "don't allow" twice) → the system dialog will no
 *   longer show, so the only path forward is app settings.
 */
@Composable
fun CameraPermissionGate(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var hasPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    // True only after the user actively denied once: shouldShowRequestPermissionRationale
    // also returns false *before* the first ask, so it cannot distinguish
    // "never asked" from "permanently denied" on its own.
    var hasRequestedOnce by remember { mutableStateOf(false) }
    var showRationale by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        hasRequestedOnce = true
        showRationale = !granted &&
            activity?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == true
    }

    // The user may grant the permission in system settings and come back. Re-check on
    // resume, otherwise we would keep showing the "denied" UI over a now-usable camera.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = context.hasCameraPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Ask exactly once on first composition. LaunchedEffect(Unit) runs its block a single
    // time for the lifetime of this composable, so recomposition cannot re-trigger the dialog.
    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    when {
        hasPermission -> content()

        // Denied for good: the system dialog is suppressed from here on.
        hasRequestedOnce && !showRationale -> PermissionRequestPrompt(
            title = R.string.permission_rationale_title,
            body = R.string.permission_denied_body,
            actionLabel = R.string.permission_open_settings,
            onAction = { context.openAppSettings() },
            modifier = modifier,
        )

        // Denied, but we may ask again.
        else -> PermissionRequestPrompt(
            title = R.string.permission_rationale_title,
            body = R.string.permission_rationale_body,
            actionLabel = R.string.permission_grant,
            onAction = { launcher.launch(Manifest.permission.CAMERA) },
            modifier = modifier,
        )
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Compose's LocalContext is not guaranteed to be an Activity (it can be a ContextWrapper),
 * so unwrap it rather than casting and hoping.
 */
private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        "package:$packageName".toUri(),
    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    startActivity(intent)
}
