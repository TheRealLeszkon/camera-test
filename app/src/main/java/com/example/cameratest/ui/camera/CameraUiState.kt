package com.example.cameratest.ui.camera

/**
 * Immutable state of the camera screen.
 *
 * A `sealed interface` makes the states mutually exclusive and exhaustive: the compiler
 * forces `when` in the UI to handle every case, so "initializing" and "error" can never be
 * silently forgotten (the usual cause of a black screen with no explanation).
 *
 * Camera *permission* is deliberately not modelled here. Requesting it requires Activity
 * APIs, so it stays in the UI layer (see CameraPermissionGate) and never reaches the
 * ViewModel — which must not hold an Activity.
 */
sealed interface CameraUiState {

    /** CameraX is starting up. Shown before the provider is ready. */
    data object Initializing : CameraUiState

    /** The provider is ready; the preview can be bound and rendered. */
    data object Ready : CameraUiState

    /** CameraX could not be initialized (no camera, hardware unavailable). */
    data class Error(val message: String) : CameraUiState
}
