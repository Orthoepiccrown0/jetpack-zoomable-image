package com.epiccrown.zoomableimage

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Coil-backed Compose image with pinch zoom, pan, double-tap zoom, and overlay support.
 */
@Composable
fun ZoomableImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    minScale: Float = ZoomableImageDefaults.MinScale,
    maxScale: Float = ZoomableImageDefaults.MaxScale,
    doubleTapScale: Float = ZoomableImageDefaults.DoubleTapScale,
    enabled: Boolean = true,
    state: ZoomableImageState = rememberZoomableImageState(minScale = minScale, maxScale = maxScale),
    onTap: ((contentPoint: Offset, contentRect: Rect) -> Unit)? = null,
    onZoomChanged: (Boolean) -> Unit = {},
    overlay: @Composable BoxScope.(state: ZoomableImageState, contentRect: Rect) -> Unit = { _, _ -> },
) {
    require(minScale > 0f) { "minScale must be greater than zero." }
    require(maxScale >= minScale) { "maxScale must be greater than or equal to minScale." }

    val painter = rememberAsyncImagePainter(model = model)
    val scope = rememberCoroutineScope()
    var size by remember { mutableStateOf(IntSize.Zero) }
    var zoomAnimation by remember { mutableStateOf<Job?>(null) }
    val intrinsicSize = painter.intrinsicSize.takeIf { it.isSpecified && it.width > 0f && it.height > 0f }
    val contentRect = remember(size, intrinsicSize) {
        intrinsicSize?.let { fittedImageRect(size, it.width.toInt(), it.height.toInt()) } ?: fullRect(size)
    }

    fun stopZoomAnimation() {
        zoomAnimation?.cancel()
        zoomAnimation = null
    }

    fun animateZoom(target: ZoomTransform) {
        stopZoomAnimation()
        zoomAnimation = scope.launch {
            state.animateTo(target)
            zoomAnimation = null
        }
    }

    LaunchedEffect(state.isZoomed) {
        onZoomChanged(state.isZoomed)
    }

    LaunchedEffect(enabled) {
        if (!enabled) {
            stopZoomAnimation()
            state.reset()
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { size = it }
            .pointerInput(model, enabled, size, contentRect, minScale, maxScale, doubleTapScale) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        onTap?.invoke(
                            tapOffset.toUntransformedContentPoint(
                                scale = state.scale,
                                translation = state.translation,
                                minScale = minScale,
                            ),
                            contentRect,
                        )
                    },
                    onDoubleTap = { tapOffset ->
                        if (!enabled) return@detectTapGestures
                        val target = if (state.isZoomed) {
                            ZoomTransform(scale = minScale, translation = Offset.Zero)
                        } else {
                            zoomTransformForTargetScale(
                                currentScale = state.scale,
                                currentTranslation = state.translation,
                                targetScale = doubleTapScale,
                                anchor = tapOffset,
                                containerSize = size,
                                contentRect = contentRect,
                                minScale = minScale,
                                maxScale = maxScale,
                            )
                        }
                        animateZoom(target)
                    },
                )
            }
            .pointerInput(model, enabled, size, contentRect, minScale, maxScale) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var hasPressedPointers: Boolean
                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        if (enabled && (pressedCount > 1 || state.isZoomed)) {
                            stopZoomAnimation()
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            if (!centroid.isSpecified) return@awaitEachGesture

                            val transform = zoomTransformForGesture(
                                currentScale = state.scale,
                                currentTranslation = state.translation,
                                zoomChange = zoom,
                                pan = pan,
                                centroid = centroid,
                                containerSize = size,
                                contentRect = contentRect,
                                minScale = minScale,
                                maxScale = maxScale,
                            )
                            state.snapTo(transform)

                            if (isMeaningfulZoom(zoom) || isMeaningfulPan(pan)) {
                                event.changes.forEach { it.consume() }
                            }
                        }
                        hasPressedPointers = event.changes.any { it.pressed }
                    } while (hasPressedPointers)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = state.scale
                    scaleY = state.scale
                    translationX = state.translation.x
                    translationY = state.translation.y
                    transformOrigin = TransformOrigin(0f, 0f)
                },
        ) {
            Image(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
            overlay(state, contentRect)
        }
    }
}

private fun fullRect(size: IntSize): Rect =
    Rect(0f, 0f, size.width.toFloat(), size.height.toFloat())
