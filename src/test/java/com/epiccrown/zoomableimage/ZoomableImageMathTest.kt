package com.epiccrown.zoomableimage

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomableImageMathTest {

    @Test
    fun `fitted rect letterboxes wide image vertically`() {
        val rect = fittedImageRect(
            container = IntSize(1_000, 1_000),
            imageWidth = 1_000,
            imageHeight = 500
        )

        assertEquals(Rect(0f, 250f, 1_000f, 750f), rect)
    }

    @Test
    fun `fitted rect letterboxes tall image horizontally`() {
        val rect = fittedImageRect(
            container = IntSize(1_000, 1_000),
            imageWidth = 500,
            imageHeight = 1_000
        )

        assertEquals(Rect(250f, 0f, 750f, 1_000f), rect)
    }

    @Test
    fun `fitted rect fills square container for square image`() {
        val rect = fittedImageRect(
            container = IntSize(1_000, 1_000),
            imageWidth = 300,
            imageHeight = 300
        )

        assertEquals(Rect(0f, 0f, 1_000f, 1_000f), rect)
    }

    @Test
    fun `fitted rect returns zero for invalid dimensions`() {
        assertEquals(Rect.Zero, fittedImageRect(IntSize(0, 1_000), 100, 100))
        assertEquals(Rect.Zero, fittedImageRect(IntSize(1_000, 0), 100, 100))
        assertEquals(Rect.Zero, fittedImageRect(IntSize(1_000, 1_000), 0, 100))
        assertEquals(Rect.Zero, fittedImageRect(IntSize(1_000, 1_000), 100, 0))
    }

    @Test
    fun `target zoom preserves content point under anchor`() {
        val anchor = Offset(250f, 300f)
        val before = anchor.toUntransformedContentPoint(
            scale = ZoomableImageDefaults.MinScale,
            translation = Offset.Zero
        )

        val transform = zoomTransformForTargetScale(
            currentScale = ZoomableImageDefaults.MinScale,
            currentTranslation = Offset.Zero,
            targetScale = 2f,
            anchor = anchor,
            containerSize = IntSize(1_000, 1_000),
            contentRect = Rect(0f, 0f, 1_000f, 1_000f)
        )
        val after = anchor.toUntransformedContentPoint(
            scale = transform.scale,
            translation = transform.translation
        )

        assertEquals(before.x, after.x, 0.001f)
        assertEquals(before.y, after.y, 0.001f)
    }

    @Test
    fun `target zoom clamps below min scale and clears translation`() {
        val transform = zoomTransformForTargetScale(
            currentScale = 2f,
            currentTranslation = Offset(-200f, -200f),
            targetScale = 0.25f,
            anchor = Offset(500f, 500f),
            containerSize = IntSize(1_000, 1_000),
            contentRect = Rect(0f, 0f, 1_000f, 1_000f)
        )

        assertEquals(ZoomableImageDefaults.MinScale, transform.scale, 0.001f)
        assertEquals(Offset.Zero, transform.translation)
    }

    @Test
    fun `target zoom clamps above max scale`() {
        val transform = zoomTransformForTargetScale(
            currentScale = 2f,
            currentTranslation = Offset.Zero,
            targetScale = 100f,
            anchor = Offset(500f, 500f),
            containerSize = IntSize(1_000, 1_000),
            contentRect = Rect(0f, 0f, 1_000f, 1_000f)
        )

        assertEquals(ZoomableImageDefaults.MaxScale, transform.scale, 0.001f)
    }

    @Test
    fun `gesture zoom applies zoom change and pan`() {
        val transform = zoomTransformForGesture(
            currentScale = 2f,
            currentTranslation = Offset(-250f, -250f),
            zoomChange = 1.25f,
            pan = Offset(30f, -20f),
            centroid = Offset(500f, 500f),
            containerSize = IntSize(1_000, 1_000),
            contentRect = Rect(0f, 0f, 1_000f, 1_000f)
        )

        assertEquals(2.5f, transform.scale, 0.001f)
        assertTrue(transform.translation.x < -200f)
        assertTrue(transform.translation.y < -200f)
    }

    @Test
    fun `translation clamps against fitted wide content`() {
        val transform = zoomTransformForTargetScale(
            currentScale = ZoomableImageDefaults.MinScale,
            currentTranslation = Offset.Zero,
            targetScale = 2f,
            anchor = Offset(500f, 500f),
            pan = Offset(0f, -10_000f),
            containerSize = IntSize(1_000, 1_000),
            contentRect = Rect(0f, 250f, 1_000f, 750f)
        )

        assertEquals(-500f, transform.translation.y, 0.001f)
    }

    @Test
    fun `translation clamps against fitted tall content`() {
        val transform = zoomTransformForTargetScale(
            currentScale = ZoomableImageDefaults.MinScale,
            currentTranslation = Offset.Zero,
            targetScale = 2f,
            anchor = Offset(500f, 500f),
            pan = Offset(-10_000f, 0f),
            containerSize = IntSize(1_000, 1_000),
            contentRect = Rect(250f, 0f, 750f, 1_000f)
        )

        assertEquals(-500f, transform.translation.x, 0.001f)
    }

    @Test
    fun `translation recenters when scaled content is smaller than viewport`() {
        val translation = clampZoomTranslation(
            value = Offset(400f, -400f),
            scale = 2f,
            containerSize = IntSize(1_000, 1_000),
            contentRect = Rect(300f, 300f, 500f, 500f)
        )

        assertEquals(-300f, translation.x, 0.001f)
        assertEquals(-300f, translation.y, 0.001f)
    }

    @Test
    fun `minimum zoom clears translation`() {
        val translation = clampZoomTranslation(
            value = Offset(-500f, -500f),
            scale = ZoomableImageDefaults.MinZoomedScale,
            containerSize = IntSize(1_000, 1_000),
            contentRect = Rect(0f, 0f, 1_000f, 1_000f)
        )

        assertEquals(Offset.Zero, translation)
    }

    @Test
    fun `content point conversion respects minimum scale`() {
        val point = Offset(20f, 40f).toUntransformedContentPoint(
            scale = 0.1f,
            translation = Offset(10f, 20f)
        )

        assertEquals(10f, point.x, 0.001f)
        assertEquals(20f, point.y, 0.001f)
    }

    @Test
    fun `gesture threshold helpers ignore tiny changes`() {
        assertFalse(isMeaningfulZoom(1.005f))
        assertTrue(isMeaningfulZoom(1.02f))
        assertFalse(isMeaningfulPan(Offset(0.25f, 0.5f)))
        assertTrue(isMeaningfulPan(Offset(0.25f, 0.51f)))
    }
}
