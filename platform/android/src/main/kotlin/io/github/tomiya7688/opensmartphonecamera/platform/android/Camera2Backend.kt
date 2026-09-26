package io.github.tomiya7688.opensmartphonecamera.platform.android

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import io.github.tomiya7688.opensmartphonecamera.capture.CameraBackend
import io.github.tomiya7688.opensmartphonecamera.capture.CameraCapabilities
import io.github.tomiya7688.opensmartphonecamera.capture.CameraDescriptor
import io.github.tomiya7688.opensmartphonecamera.capture.CameraId
import io.github.tomiya7688.opensmartphonecamera.capture.CameraSession
import io.github.tomiya7688.opensmartphonecamera.capture.IntSize
import io.github.tomiya7688.opensmartphonecamera.capture.LensFacing
import io.github.tomiya7688.opensmartphonecamera.capture.PixelFormat

internal class Camera2Backend(
    context: Context,
) : CameraBackend {

    private val cameraManager: CameraManager =
        context.getSystemService(CameraManager::class.java)

    override suspend fun enumerateCameras(): List<CameraDescriptor> =
        cameraManager.cameraIdList.map { platformCameraId ->
            val characteristics =
                cameraManager.getCameraCharacteristics(platformCameraId)

            characteristics.toProjectDescriptor(platformCameraId)
        }

    override suspend fun queryCapabilities(
        cameraId: CameraId,
    ): CameraCapabilities {
        val characteristics =
            cameraManager.getCameraCharacteristics(cameraId.value)

        val streamConfigurationMap =
            characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
            )

        val platformFormats =
            streamConfigurationMap
                ?.outputFormats
                ?: intArrayOf()

        val outputFormats =
            platformFormats
                .mapNotNull { platformFormat ->
                    platformFormat.toProjectPixelFormat()
                }
                .toSet()

        val outputSizes =
            buildMap<PixelFormat, List<IntSize>> {
                platformFormats
                    .forEach { platformFormat ->
                        val projectFormat =
                            platformFormat.toProjectPixelFormat()
                                ?: return@forEach

                        val mappedSizes =
                            streamConfigurationMap
                                ?.getOutputSizes(platformFormat)
                                .orEmpty()
                                .map { size ->
                                    IntSize(
                                        width = size.width,
                                        height = size.height,
                                    )
                                }
                                .distinct()

                        if (mappedSizes.isNotEmpty()) {
                            put(
                                projectFormat,
                                (get(projectFormat).orEmpty() + mappedSizes)
                                    .distinct(),
                            )
                        }
                    }
            }

        val availableCapabilities =
            characteristics.get(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
            )
                ?.toSet()
                .orEmpty()

        val exposureRange =
            characteristics.get(
                CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE
            )

        val sensitivityRange =
            characteristics.get(
                CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE
            )

        return CameraCapabilities(
            camera =
                characteristics.toProjectDescriptor(cameraId.value),
            outputFormats = outputFormats,
            supportsManualSensor =
                CameraCharacteristics
                    .REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR in
                    availableCapabilities,
            supportsBurstCapture =
                CameraCharacteristics
                    .REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE in
                    availableCapabilities,
            exposureTimeRangeNs =
                exposureRange?.let { range ->
                    range.lower..range.upper
                },
            sensitivityRangeIso =
                sensitivityRange?.let { range ->
                    range.lower..range.upper
                },
            outputSizes = outputSizes,
        )
    }

    override suspend fun open(
        cameraId: CameraId,
    ): CameraSession =
        TODO("CameraSession is implemented in the next M0 capture step")
}

private fun CameraCharacteristics.toProjectDescriptor(
    platformCameraId: String,
): CameraDescriptor =
    CameraDescriptor(
        id = CameraId(platformCameraId),
        lensFacing =
            get(CameraCharacteristics.LENS_FACING)
                .toProjectLensFacing(),
        physicalCameraIds =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                physicalCameraIds
                    .map(::CameraId)
                    .toSet()
            } else {
                emptySet()
            },
    )

private fun Int?.toProjectLensFacing(): LensFacing =
    when (this) {
        CameraCharacteristics.LENS_FACING_FRONT ->
            LensFacing.FRONT

        CameraCharacteristics.LENS_FACING_BACK ->
            LensFacing.BACK

        CameraCharacteristics.LENS_FACING_EXTERNAL ->
            LensFacing.EXTERNAL

        else ->
            LensFacing.UNKNOWN
    }

private fun Int.toProjectPixelFormat(): PixelFormat? =
    when {
        this == ImageFormat.RAW_SENSOR ->
            PixelFormat.RAW_SENSOR

        this == ImageFormat.RAW10 ->
            PixelFormat.RAW10

        this == ImageFormat.RAW12 ->
            PixelFormat.RAW12

        this == ImageFormat.YUV_420_888 ->
            PixelFormat.YUV_420_888

        this == ImageFormat.JPEG ->
            PixelFormat.JPEG

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            this == ImageFormat.HEIC ->
            PixelFormat.HEIF

        else ->
            null
    }
