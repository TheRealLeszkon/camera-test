package com.example.cameratest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.cameratest.ui.camera.CameraScreen
import com.example.cameratest.ui.theme.CameraTestTheme

/**
 * Single Activity host.
 *
 * It does almost nothing on purpose: it installs the Compose content and gets out of the
 * way. All camera work lives behind the ViewModel, which is what lets the Activity be
 * destroyed and recreated on rotation without disturbing the camera session.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw behind the system bars. A camera preview should fill the display, not sit in
        // a letterbox between status and navigation bars.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CameraTestTheme {
                CameraScreen()
            }
        }
    }
}
