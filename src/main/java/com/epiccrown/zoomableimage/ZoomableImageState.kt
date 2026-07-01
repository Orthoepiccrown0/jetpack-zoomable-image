package com.epiccrown.zoomableimage

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * State holder for [ZoomableImage].
 */
@Stable
class ZoomableImageState internal constructor(
    val minScale: Float,
    val maxScale: Float,
) {
    var scale by mutableFloatStateOf(minScale)
        private set

    var translation by mutableStateOf(Offset.Zero)
        private set

    val isZoomed: Boolean
        get() = scale > ZoomableImageDefaults.MinZoomedScale

    fun snapTo(transform: ZoomTransform) {
        scale = transform.scale.coerceIn(minScale, maxScale)
        translation = if (scale <= ZoomableImageDefaults.MinZoomedScale) {
            Offset.Zero
        } else {
            transform.translation
        }
    }

    fun reset() {
        snapTo(ZoomTransform(scale = minScale, translation = Offset.Zero))
    }

    suspend fun animateTo(
        transform: ZoomTransform,
        animationSpec: AnimationSpec<Float> = spring(),
    ) {
        val startScale = scale
        val startTranslation = translation
        val targetScale = transform.scale.coerceIn(minScale, maxScale)
        val targetTranslation = if (targetScale <= ZoomableImageDefaults.MinZoomedScale) {
            Offset.Zero
        } else {
            transform.translation
        }
        Animatable(0f).animateTo(1f, animationSpec) {
            val fraction = value
            scale = lerp(startScale, targetScale, fraction)
            translation = Offset(
                x = lerp(startTranslation.x, targetTranslation.x, fraction),
                y = lerp(startTranslation.y, targetTranslation.y, fraction),
            )
        }
        scale = targetScale
        translation = targetTranslation
    }
}

@Composable
fun rememberZoomableImageState(
    minScale: Float = ZoomableImageDefaults.MinScale,
    maxScale: Float = ZoomableImageDefaults.MaxScale,
): ZoomableImageState = remember(minScale, maxScale) {
    ZoomableImageState(
        minScale = minScale,
        maxScale = maxScale,
    )
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + ((stop - start) * fraction)
