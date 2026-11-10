package com.example.cameratest.ui.camera

import android.app.Application
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cameratest.camera.CameraSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Holds the camera session and exposes the screen's state.
 *
 * The ViewModel outlives configuration changes, which is exactly what we want for the
 * CameraX provider: rotating the device must not tear down and rebuild the camera stack.
 *
 * It exposes state as a [StateFlow] — an observable value holder that always has a current
 * value, so a newly-composed (or re-composed after rotation) UI immediately renders the
 * latest state rather than waiting for the next emission.
 */
class CameraViewModel(
    private val sessionManager: CameraSessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.Initializing)

    /** Read-only view of the state. Only the ViewModel may mutate it. */
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    init {
        initializeCamera()
    }

    /**
     * Starts CameraX. Runs in [viewModelScope], so the work is cancelled automatically if
     * the ViewModel is cleared mid-initialization — and, being a coroutine, it never blocks
     * the main thread while CameraX starts up.
     */
    fun initializeCamera() {
        viewModelScope.launch {
            _uiState.value = CameraUiState.Initializing
            runCatching { sessionManager.initialize() }
                .onSuccess { _uiState.value = CameraUiState.Ready }
                .onFailure { throwable ->
                    _uiState.value = CameraUiState.Error(
                        throwable.message ?: "Failed to initialize the camera.",
                    )
                }
        }
    }

    /**
     * Binds the preview to the given lifecycle and surface.
     *
     * The UI passes the [Preview.SurfaceProvider] *down* at bind time; the ViewModel never
     * stores it. Called from a DisposableEffect, so it runs on lifecycle changes rather than
     * on every recomposition.
     */
    fun bindPreview(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        sessionManager.bind(lifecycleOwner, surfaceProvider)
    }

    /** Releases the binding and the surface when the preview leaves the composition. */
    fun unbindPreview() {
        sessionManager.unbind()
    }

    /** The screen is gone for good (not merely rotating): release the camera session. */
    override fun onCleared() {
        sessionManager.shutdown()
    }

    companion object {
        /**
         * Manual factory instead of a DI framework: the dependency is a single object built
         * from the Application context. [CreationExtras] hands us that context without the
         * ViewModel ever touching an Activity.
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                CameraViewModel(CameraSessionManager(application))
            }
        }
    }
}
