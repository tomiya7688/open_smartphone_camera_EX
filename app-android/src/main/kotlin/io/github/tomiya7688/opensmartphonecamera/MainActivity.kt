package io.github.tomiya7688.opensmartphonecamera

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import io.github.tomiya7688.opensmartphonecamera.capture.CameraBackend
import io.github.tomiya7688.opensmartphonecamera.capture.CameraCapabilities
import io.github.tomiya7688.opensmartphonecamera.platform.android.AndroidCameraBackendFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : Activity() {

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main.immediate
        )

    private lateinit var cameraBackend: CameraBackend
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        statusView =
            TextView(this).apply {
                textSize = 16f
                setPadding(32, 64, 32, 32)
                text = "Initializing camera..."
            }

        setContentView(statusView)

        cameraBackend =
            AndroidCameraBackendFactory.create(this)

        if (
            checkSelfPermission(Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            loadCapabilityReport()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST,
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
        )

        if (
            requestCode == CAMERA_PERMISSION_REQUEST &&
            grantResults.firstOrNull() ==
            PackageManager.PERMISSION_GRANTED
        ) {
            loadCapabilityReport()
        } else {
            statusView.text = "Camera permission denied"
        }
    }

    private fun loadCapabilityReport() {
        statusView.text = "Reading camera capabilities..."

        scope.launch {
            runCatching {
                cameraBackend
                    .enumerateCameras()
                    .map { camera ->
                        cameraBackend.queryCapabilities(camera.id)
                    }
            }.onSuccess { capabilities ->
                val report =
                    if (capabilities.isEmpty()) {
                        "No cameras detected"
                    } else {
                        capabilities.joinToString(
                            separator = "\n\n",
                            transform = CameraCapabilities::toReadableReport,
                        )
                    }

                Log.i(TAG, report)
                statusView.text = report
            }.onFailure { error ->
                Log.e(TAG, "Capability enumeration failed", error)

                statusView.text =
                    buildString {
                        appendLine("Capability enumeration failed")
                        append(error.message ?: error::class.java.simpleName)
                    }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "OpenCameraEX"
        private const val CAMERA_PERMISSION_REQUEST = 100
    }
}

private fun CameraCapabilities.toReadableReport(): String =
    buildString {
        append("Camera ")
        append(camera.id.value)
        append(" (")
        append(camera.lensFacing)
        appendLine(")")

        if (camera.physicalCameraIds.isNotEmpty()) {
            append("  physical cameras: ")
            appendLine(
                camera.physicalCameraIds
                    .joinToString { it.value }
            )
        }

        append("  manual sensor: ")
        appendLine(supportsManualSensor)

        append("  burst capture: ")
        appendLine(supportsBurstCapture)

        append("  exposure time (ns): ")
        appendLine(exposureTimeRangeNs ?: "unknown")

        append("  ISO: ")
        appendLine(sensitivityRangeIso ?: "unknown")

        appendLine("  output formats:")

        outputFormats
            .sortedBy { it.name }
            .forEach { format ->
                val sizes = outputSizes[format].orEmpty()

                val largest =
                    sizes.maxByOrNull { size ->
                        size.width.toLong() * size.height
                    }

                append("    - ")
                append(format)
                append(": ")
                append(sizes.size)
                append(" size(s)")

                if (largest != null) {
                    append(", max ")
                    append(largest.width)
                    append('x')
                    append(largest.height)
                }

                appendLine()
            }
    }.trimEnd()
