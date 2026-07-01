package com.epiccrown.zoomableimage

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs

/**
 * Immutable zoom transform used by [ZoomableImageState].
 */
data class ZoomTransform(
    val scale: Float,
    val translation: Offset,
)

/**
 * Returns the rectangle occupied by fit-scaled content inside [container].
 */
fun fittedImageRect(
    container: IntSize,
    imageWidth: Int,
    imageHeight: Int,
): Rect {
    if (container.width <= 0 || container.height <= 0 || imageWidth <= 0 || imageHeight <= 0) {
        return Rect.Zero
    }

    val containerRatio = container.width.toFloat() / container.height
    val imageRatio = imageWidth.toFloat() / imageHeight
    return if (imageRatio > containerRatio) {
        val width = container.width.toFloat()
        val height = width / imageRatio
        val top = (container.height - height) / 2f
        Rect(0f, top, width, top + height)
    } else {
        val height = container.height.toFloat()
        val width = height * imageRatio
        val left = (container.width - width) / 2f
        Rect(left, 0f, left + width, height)
    }
}

/**
 * Converts a displayed point back into the unscaled, untranslated content coordinate space.
 */
fun Offset.toUntransformedContentPoint(
    scale: Float,
    translation: Offset,
    minScale: Float = ZoomableImageDefaults.MinScale,
): Offset = (this - translation) / scale.coerceAtLeast(minScale)

/**
 * Computes a transform for pinch-zoom and pan gestures.
 */
fun zoomTransformForGesture(
    currentScale: Float,
    currentTranslation: Offset,
    zoomChange: Float,
    pan: Offset,
    centroid: Offset,
    containerSize: IntSize,
    contentRect: Rect,
    minScale: Float = ZoomableImageDefaults.MinScale,
    maxScale: Float = ZoomableImageDefaults.MaxScale,
): ZoomTransform {
    val targetScale = (currentScale * zoomChange).coerceIn(minScale, maxScale)
    return zoomTransformForTargetScale(
        currentScale = currentScale,
        currentTranslation = currentTranslation,
        targetScale = targetScale,
        anchor = centroid,
        pan = pan,
        containerSize = containerSize,
        contentRect = contentRect,
        minScale = minScale,
        maxScale = maxScale,
    )
}

/**
 * Computes a transform that keeps [anchor] visually stable while changing scale.
 */
fun zoomTransformForTargetScale(
    currentScale: Float,
    currentTranslation: Offset,
    targetScale: Float,
    anchor: Offset,
    pan: Offset = Offset.Zero,
    containerSize: IntSize,
    contentRect: Rect,
    minScale: Float = ZoomableImageDefaults.MinScale,
    maxScale: Float = ZoomableImageDefaults.MaxScale,
): ZoomTransform {
    val clampedScale = targetScale.coerceIn(minScale, maxScale)
    if (
        clampedScale <= ZoomableImageDefaults.MinZoomedScale ||
        containerSize.width <= 0 ||
        containerSize.height <= 0 ||
        contentRect == Rect.Zero
    ) {
        return ZoomTransform(scale = minScale, translation = Offset.Zero)
    }

    val contentPoint = anchor.toUntransformedContentPoint(
        scale = currentScale,
        translation = currentTranslation,
        minScale = minScale,
    )
    val anchoredTranslation = anchor - (contentPoint * clampedScale)
    val unclamped = anchoredTranslation + pan
    return ZoomTransform(
        scale = clampedScale,
        translation = clampZoomTranslation(
            value = unclamped,
            scale = clampedScale,
            containerSize = containerSize,
            contentRect = contentRect,
        ),
    )
}

/**
 * Clamps translation so scaled content remains covering the viewport where possible.
 */
fun clampZoomTranslation(
    value: Offset,
    scale: Float,
    containerSize: IntSize,
    contentRect: Rect,
): Offset {
    if (scale <= ZoomableImageDefaults.MinZoomedScale) return Offset.Zero
    return Offset(
        x = clampAxisTranslation(
            value = value.x,
            scaledContentStart = contentRect.left * scale,
            scaledContentEnd = contentRect.right * scale,
            viewportSize = containerSize.width.toFloat(),
        ),
        y = clampAxisTranslation(
            value = value.y,
            scaledContentStart = contentRect.top * scale,
            scaledContentEnd = contentRect.bottom * scale,
            viewportSize = containerSize.height.toFloat(),
        ),
    )
}

internal fun isMeaningfulZoom(zoom: Float): Boolean =
    abs(zoom - 1f) > ZoomableImageDefaults.ZoomEpsilon

internal fun isMeaningfulPan(pan: Offset): Boolean =
    abs(pan.x) > ZoomableImageDefaults.PanEpsilon || abs(pan.y) > ZoomableImageDefaults.PanEpsilon

private fun clampAxisTranslation(
    value: Float,
    scaledContentStart: Float,
    scaledContentEnd: Float,
    viewportSize: Float,
): Float {
    val minTranslation = viewportSize - scaledContentEnd
    val maxTranslation = -scaledContentStart
    return if (minTranslation <= maxTranslation) {
        value.coerceIn(minTranslation, maxTranslation)
    } else {
        (minTranslation + maxTranslation) / 2f
    }
}

object ZoomableImageDefaults {
    const val MinScale: Float = 1f
    const val MaxScale: Float = 5f
    const val DoubleTapScale: Float = 2.5f
    const val MinZoomedScale: Float = 1.01f
    internal const val ZoomEpsilon: Float = 0.01f
    internal const val PanEpsilon: Float = 0.5f
}
