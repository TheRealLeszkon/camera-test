package com.example.cameratest.camera

import android.content.Context
import androidx.annotation.MainThread
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.lifecycle.LifecycleOwner

/**
 * Owns the CameraX session: the [ProcessCameraProvider], the use cases, and the
 * binding between them and a [LifecycleOwner].
 *
 * This is the *only* class in the app that knows CameraX exists. Composables never
 * talk to CameraX directly, and the ViewModel only forwards to this manager. That
 * boundary is what lets us add ImageCapture / ImageAnalysis later without touching UI code.
 *
 * ## Lifetime rules this class enforces
 * - The provider is fetched **once** per instance and cached ([initialize] is idempotent).
 *   Because the ViewModel owns this manager, the provider survives configuration changes:
 *   rotating the device does not re-initialize the camera stack.
 * - It holds only the **application** [Context]. Never an Activity, never a View — that
 *   would leak the whole Activity for as long as the ViewModel lives.
 * - The [Preview.SurfaceProvider] (which *is* View-backed) is passed in at [bind] time and
 *   released in [unbind]. It is never stored beyond the binding.
 */
class CameraSessionManager(context: Context) {

    private val appContext: Context = context.applicationContext

    /** Cached after the first successful [initialize]. Null until then. */
    private var cameraProvider: ProcessCameraProvider? = null

    /**
     * A single, long-lived [Preview] use case. We reuse it across binds instead of
     * rebuilding it: constructing use cases is not free, and rebuilding them is one of
     * the most common causes of preview flicker.
     */
    private val preview: Preview by lazy { Preview.Builder().build() }

    /**
     * The bound camera. Exposes [CameraControl] (zoom, torch, focus) and [CameraInfo]
     * (zoom range, torch availability). Upcoming features drive the camera *through these*
     * rather than by rebinding use cases — rebinding tears the capture session down and
     * visibly stalls the preview.
     */
    private var camera: Camera? = null

    val cameraControl: CameraControl?
        get() = camera?.cameraControl

    val cameraInfo: CameraInfo?
        get() = camera?.cameraInfo

    /**
     * Obtains the process-wide [ProcessCameraProvider], suspending until CameraX has
     * finished starting up. Safe to call repeatedly; only the first call does work.
     *
     * `awaitInstance` is the coroutine-friendly form of `getInstance()`, which otherwise
     * returns a `ListenableFuture` you would have to bridge by hand.
     *
     * @throws Exception if CameraX fails to initialize (no camera, hardware in a bad state).
     */
    suspend fun initialize() {
        if (cameraProvider != null) return
        cameraProvider = ProcessCameraProvider.awaitInstance(appContext)
    }

    /**
     * Binds the [Preview] use case to [lifecycleOwner] and starts streaming frames into
     * [surfaceProvider].
     *
     * CameraX observes the lifecycle itself: the camera opens at STARTED and closes at
     * STOPPED, with no manual open/close code. That is the core value of `camera-lifecycle`,
     * and it is why backgrounding the app releases the camera automatically.
     *
     * Must run on the main thread — a CameraX requirement for [ProcessCameraProvider.bindToLifecycle].
     *
     * @return the bound [Camera], or null if the provider is not initialized yet.
     */
    @MainThread
    fun bind(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    ): Camera? {
        val provider = cameraProvider ?: return null

        preview.surfaceProvider = surfaceProvider

        val selector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        // A use case may only be bound to one lifecycle at a time, so unbind first.
        // On a fresh start this is a no-op; on re-entry it releases the previous binding.
        provider.unbindAll()

        return provider.bindToLifecycle(lifecycleOwner, selector, preview).also { camera = it }
    }

    /**
     * Unbinds all use cases and, critically, drops the reference to the
     * [Preview.SurfaceProvider].
     *
     * Without clearing the surface provider, the long-lived [preview] would keep pointing at
     * a destroyed `PreviewView` — leaking the Activity that hosted it across every rotation.
     */
    @MainThread
    fun unbind() {
        preview.surfaceProvider = null
        cameraProvider?.unbindAll()
        camera = null
    }

    /**
     * Releases the session. Called from `ViewModel.onCleared()`, i.e. when the screen is
     * gone for good rather than merely rotating.
     *
     * Future features that add background work (ImageCapture's callback executor,
     * ImageAnalysis's analyzer executor) shut that work down here.
     */
    @MainThread
    fun shutdown() {
        unbind()
        cameraProvider = null
    }
}
