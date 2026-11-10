package com.example.cameratest.ui.camera

import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Renders the live camera preview.
 *
 * This composable is pure UI plumbing: it creates the [PreviewView], hands its
 * [Preview.SurfaceProvider] to the camera layer, and takes it back on disposal. It contains
 * no CameraX orchestration of its own.
 *
 * @param onBind called when the preview enters the composition, with the surface to stream into.
 * @param onUnbind called when it leaves, so the camera layer can drop the surface reference.
 */
@Composable
fun CameraPreview(
    onBind: (LifecycleOwner, Preview.SurfaceProvider) -> Unit,
    onUnbind: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // `remember` keeps this single PreviewView across recompositions. Creating it inside the
    // AndroidView factory would be enough on its own, but remembering it makes the instance
    // available here for the DisposableEffect below — and makes the "created once" intent explicit.
    val previewView = remember(context) {
        PreviewView(context).apply {
            // PERFORMANCE uses a SurfaceView under the hood: the frames go straight to the
            // display compositor instead of through the View hierarchy. Lower latency and
            // power than COMPATIBLE (a TextureView), at the cost of not being freely
            // animatable/rotatable. For a full-screen preview that is the right trade.
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            // Fill the viewport, cropping the overflow, so there are no letterbox bars when
            // the sensor aspect ratio differs from the screen's.
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Binding is a lifecycle concern, not a recomposition concern. DisposableEffect runs the
    // bind once per (lifecycleOwner, previewView) and guarantees the matching cleanup — this
    // is what keeps us from rebinding use cases on every frame of state change.
    DisposableEffect(lifecycleOwner, previewView) {
        onBind(lifecycleOwner, previewView.surfaceProvider)
        onDispose { onUnbind() }
    }

    // AndroidView is the bridge from Compose to the classic View system, which we need
    // because CameraX renders into a real View (PreviewView). The factory runs once; update
    // runs on recomposition — we have nothing to push into the View, so it is omitted.
    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize(),
    )
}
