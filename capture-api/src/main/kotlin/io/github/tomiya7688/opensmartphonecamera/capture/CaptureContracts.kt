package io.github.tomiya7688.opensmartphonecamera.capture

import java.nio.ByteBuffer

@JvmInline
value class CameraId(val value: String)

@JvmInline
value class FrameId(val value: Long)

data class IntSize(
    val width: Int,
    val height: Int,
) {
    init {
        require(width > 0) { "width must be > 0" }
        require(height > 0) { "height must be > 0" }
    }
}

enum class LensFacing {
    FRONT,
    BACK,
    EXTERNAL,
    UNKNOWN,
}

enum class PixelFormat {
    RAW_SENSOR,
    RAW10,
    RAW12,
    YUV_420_888,
    JPEG,
    HEIF,
    UNKNOWN,
}

enum class CfaPattern {
    RGGB,
    GRBG,
    GBRG,
    BGGR,
    MONO,
    UNKNOWN,
}

data class Matrix3x3 private constructor(
    val values: List<Double>,
) {
    companion object {
        fun of(values: List<Double>): Matrix3x3 {
            require(values.size == 9) { "Matrix3x3 requires exactly 9 values" }
            return Matrix3x3(values.toList())
        }
    }
}

data class BlackLevel(
    val channel0: Double,
    val channel1: Double,
    val channel2: Double,
    val channel3: Double,
)

data class CameraDescriptor(
    val id: CameraId,
    val lensFacing: LensFacing,
    val physicalCameraIds: Set<CameraId> = emptySet(),
)

data class CameraCapabilities(
    val camera: CameraDescriptor,
    val outputFormats: Set<PixelFormat>,
    val supportsManualSensor: Boolean = false,
    val supportsBurstCapture: Boolean = false,
    val exposureTimeRangeNs: LongRange? = null,
    val sensitivityRangeIso: IntRange? = null,
    val outputSizes: Map<PixelFormat, List<IntSize>> = emptyMap(),
)

data class CaptureRequest(
    val format: PixelFormat,
    val targetSize: IntSize? = null,
    val exposureTimeNs: Long? = null,
    val sensitivityIso: Int? = null,
)

data class CaptureMetadata(
    val timestampNs: Long,
    val exposureTimeNs: Long? = null,
    val sensitivityIso: Int? = null,
    val aperture: Double? = null,
    val focalLengthMm: Double? = null,
    val rotationDegrees: Int? = null,
    val blackLevel: BlackLevel? = null,
    val whiteLevel: Int? = null,
    val cfaPattern: CfaPattern? = null,
    val sensorToXyz: Matrix3x3? = null,
)

/**
 * One plane of project-owned image memory.
 *
 * Implementations should expose a read-only buffer to consumers whenever possible.
 * No Android camera object may be required to read this plane.
 */
data class FramePlane(
    val bytes: ByteBuffer,
    val rowStrideBytes: Int,
    val pixelStrideBytes: Int,
)

/**
 * Owns image memory after it has crossed the platform/backend boundary.
 *
 * Closing a FrameBuffer must never require processing code to know whether the
 * source was Camera2, CameraX, a file, or another backend.
 */
interface FrameBuffer : AutoCloseable {
    val planes: List<FramePlane>
    val isClosed: Boolean

    override fun close()
}

data class CapturedFrame(
    val id: FrameId,
    val size: IntSize,
    val format: PixelFormat,
    val buffer: FrameBuffer,
    val metadata: CaptureMetadata,
) : AutoCloseable {
    override fun close() = buffer.close()
}

interface CameraSession : AutoCloseable {
    val camera: CameraDescriptor
    val capabilities: CameraCapabilities

    suspend fun capture(request: CaptureRequest): CapturedFrame

    override fun close()
}

/**
 * The only camera entry point visible to application orchestration.
 *
 * Camera2/CameraX/future APIs implement this interface in platform adapters.
 */
interface CameraBackend {
    suspend fun enumerateCameras(): List<CameraDescriptor>

    suspend fun queryCapabilities(cameraId: CameraId): CameraCapabilities

    suspend fun open(cameraId: CameraId): CameraSession
}
