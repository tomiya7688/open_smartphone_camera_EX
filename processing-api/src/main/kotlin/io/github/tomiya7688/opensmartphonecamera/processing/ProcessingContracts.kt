package io.github.tomiya7688.opensmartphonecamera.processing

import io.github.tomiya7688.opensmartphonecamera.capture.CapturedFrame
import java.nio.ByteBuffer

@JvmInline
value class ProcessingStageId(val value: String)

enum class QualityMode {
    FAST,
    BALANCED,
    QUALITY,
    MAX,
}

/**
 * Stage switches are generic by design.
 *
 * AI features are stage IDs just like denoise, HDR, sharpening, or any future
 * processor. Capture code does not need a special AI mode or AI dependency.
 */
data class ProcessingProfile(
    val name: String,
    val qualityMode: QualityMode = QualityMode.BALANCED,
    val enabledStages: Set<ProcessingStageId> = emptySet(),
    val disabledStages: Set<ProcessingStageId> = emptySet(),
)

data class ProcessingRequest(
    val frames: List<CapturedFrame>,
    val profile: ProcessingProfile,
) {
    init {
        require(frames.isNotEmpty()) { "At least one frame is required" }
    }
}

sealed interface ProcessingResult {
    /**
     * Temporary M0 result for proving Capture -> Pipeline wiring.
     * The caller still owns and must close the original frame.
     */
    data class Passthrough(
        val frame: CapturedFrame,
    ) : ProcessingResult

    data class EncodedImage(
        val bytes: ByteBuffer,
        val mimeType: String,
        val width: Int,
        val height: Int,
    ) : ProcessingResult
}

/**
 * Stable application-facing processing boundary.
 *
 * The implementation may later be Kotlin, C++, SIMD, Vulkan, AI runtime, or a
 * combination of them without changing capture backends.
 */
interface ImageProcessor {
    suspend fun process(request: ProcessingRequest): ProcessingResult
}
