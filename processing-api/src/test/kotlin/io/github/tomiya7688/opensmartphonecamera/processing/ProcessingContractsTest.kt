package io.github.tomiya7688.opensmartphonecamera.processing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProcessingContractsTest {

    @Test
    fun processingRequestRequiresAtLeastOneFrame() {
        assertFailsWith<IllegalArgumentException> {
            ProcessingRequest(
                frames = emptyList(),
                profile = ProcessingProfile(name = "test"),
            )
        }
    }

    @Test
    fun processingProfileAcceptsArbitraryStageIds() {
        val ai = ProcessingStageId("ai")
        val denoise = ProcessingStageId("denoise")

        val profile =
            ProcessingProfile(
                name = "custom",
                enabledStages = setOf(ai),
                disabledStages = setOf(denoise),
            )

        assertTrue(ai in profile.enabledStages)
        assertTrue(denoise in profile.disabledStages)
        assertEquals(QualityMode.BALANCED, profile.qualityMode)
    }
}
