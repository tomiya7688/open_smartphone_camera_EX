package io.github.tomiya7688.opensmartphonecamera.platform.android

import android.content.Context
import io.github.tomiya7688.opensmartphonecamera.capture.CameraBackend

object AndroidCameraBackendFactory {
    fun create(context: Context): CameraBackend =
        Camera2Backend(context.applicationContext)
}
