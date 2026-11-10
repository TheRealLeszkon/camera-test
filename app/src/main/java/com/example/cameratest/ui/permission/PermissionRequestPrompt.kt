package com.example.cameratest.ui.permission

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cameratest.ui.components.MessagePanel

/**
 * Explains why the camera permission is needed and offers the single relevant action —
 * either "ask again" or "open settings", depending on what the system will still allow.
 *
 * A thin wrapper over [MessagePanel]: it only resolves string resources, so the layout
 * stays in one place.
 */
@Composable
internal fun PermissionRequestPrompt(
    @StringRes title: Int,
    @StringRes body: Int,
    @StringRes actionLabel: Int,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MessagePanel(
        icon = Icons.Rounded.PhotoCamera,
        title = stringResource(title),
        body = stringResource(body),
        actionLabel = stringResource(actionLabel),
        onAction = onAction,
        modifier = modifier,
    )
}
