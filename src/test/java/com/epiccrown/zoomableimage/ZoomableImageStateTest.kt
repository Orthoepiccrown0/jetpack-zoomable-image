package com.epiccrown.zoomableimage

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomableImageStateTest {

    @Test
    fun `snap coerces scale into configured bounds`() {
        val state = ZoomableImageState(minScale = 1f, maxScale = 3f)

        state.snapTo(ZoomTransform(scale = 10f, translation = Offset(-50f, -60f)))

        assertEquals(3f, state.scale, 0.001f)
        assertEquals(Offset(-50f, -60f), state.translation)
        assertTrue(state.isZoomed)
    }

    @Test
    fun `snap below zoomed threshold clears translation`() {
        val state = ZoomableImageState(minScale = 1f, maxScale = 3f)

        state.snapTo(ZoomTransform(scale = 1.005f, translation = Offset(-50f, -60f)))

        assertEquals(1.005f, state.scale, 0.001f)
        assertEquals(Offset.Zero, state.translation)
        assertFalse(state.isZoomed)
    }

    @Test
    fun `reset restores minimum scale and zero translation`() {
        val state = ZoomableImageState(minScale = 1f, maxScale = 3f)
        state.snapTo(ZoomTransform(scale = 2f, translation = Offset(-50f, -60f)))

        state.reset()

        assertEquals(1f, state.scale, 0.001f)
        assertEquals(Offset.Zero, state.translation)
        assertFalse(state.isZoomed)
    }
}
