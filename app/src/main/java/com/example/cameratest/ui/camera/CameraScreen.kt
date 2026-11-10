package com.example.cameratest.ui.camera

import androidx.camera.core.Preview
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cameratest.R
import com.example.cameratest.ui.components.MessagePanel
import com.example.cameratest.ui.permission.CameraPermissionGate

/**
 * The camera screen: permission gate, then whatever the camera state calls for.
 *
 * Stateful ("screen") composable — it reads the ViewModel and passes plain values and
 * lambdas down to stateless children. That split keeps the children previewable and testable
 * without a ViewModel.
 */
@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = viewModel(factory = CameraViewModel.Factory),
) {
    // collectAsStateWithLifecycle stops collecting when the screen drops below STARTED,
    // so a backgrounded app does no recomposition work. (The plain collectAsState() would
    // keep collecting.)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        CameraPermissionGate {
            CameraContent(
                uiState = uiState,
                onBind = viewModel::bindPreview,
                onUnbind = viewModel::unbindPreview,
                onRetry = viewModel::initializeCamera,
            )
        }
    }
}

/**
 * Stateless rendering of [CameraUiState]. Knows nothing about the ViewModel.
 */
@Composable
private fun CameraContent(
    uiState: CameraUiState,
    onBind: (LifecycleOwner, Preview.SurfaceProvider) -> Unit,
    onUnbind: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Crossfade softens the Initializing → Ready transition. Camera start-up is fast but not
    // instant, and a hard cut from spinner to video reads as a flash.
    Crossfade(
        targetState = uiState,
        label = "CameraStateCrossfade",
        modifier = modifier.fillMaxSize(),
    ) { state ->
        when (state) {
            CameraUiState.Initializing -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            CameraUiState.Ready -> {
                val description = stringResource(R.string.camera_preview_description)
                CameraPreview(
                    onBind = onBind,
                    onUnbind = onUnbind,
                    modifier = Modifier
                        .fillMaxSize()
                        // The preview is a live view of the world; screen readers get a
                        // label rather than an unlabelled black rectangle.
                        .semantics { contentDescription = description },
                )
            }

            is CameraUiState.Error -> MessagePanel(
                icon = Icons.Rounded.Warning,
                title = stringResource(R.string.camera_error_title),
                body = state.message,
                actionLabel = stringResource(R.string.camera_retry),
                onAction = onRetry,
            )
        }
    }
}
