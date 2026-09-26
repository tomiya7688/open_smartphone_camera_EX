package io.github.tomiya7688.opensmartphonecamera.capture

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CaptureContractsTest {

    @Test
    fun intSizeRequiresPositiveDimensions() {
        assertFailsWith<IllegalArgumentException> {
            IntSize(width = 0, height = 1)
        }

        assertFailsWith<IllegalArgumentException> {
            IntSize(width = 1, height = 0)
        }
    }

    @Test
    fun matrix3x3RequiresExactlyNineValues() {
        assertFailsWith<IllegalArgumentException> {
            Matrix3x3.of(List(8) { 0.0 })
        }

        val matrix =
            Matrix3x3.of(
                listOf(
                    1.0, 0.0, 0.0,
                    0.0, 1.0, 0.0,
                    0.0, 0.0, 1.0,
                )
            )

        assertEquals(9, matrix.values.size)
    }
}
