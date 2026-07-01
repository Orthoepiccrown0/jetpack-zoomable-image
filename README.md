# Jetpack Zoomable Image

Jetpack Zoomable Image is a lightweight Jetpack Compose image viewer for Android. It displays Coil-backed images with pinch zoom, pan, double-tap zoom, and optional overlay content.

The library is designed for product screens that need image inspection rather than a full gallery framework: photo previews, document scans, detection results, annotation tools, and before/after review surfaces.

## Features

- Pinch-to-zoom and drag-to-pan gestures
- Double-tap zoom toggle
- Translation clamping so zoomed content stays inside the viewport
- Overlay slot for detection boxes, annotations, crop handles, and custom UI
- `ZoomableImageState` for observing and controlling zoom state
- Coil 3 image loading through `rememberAsyncImagePainter`
- Pure transform math with unit test coverage

## Requirements

- Android min SDK 30
- Jetpack Compose
- Kotlin 2.4.0 or newer
- Android Gradle Plugin 9.2.1 or newer
- Coil Compose 3.x

## Installation

Add JitPack in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") {
            content {
                includeGroup("com.epiccrown")
            }
        }
    }
}
```

Add the dependency in `gradle/libs.versions.toml`:

```toml
[versions]
zoomable-image = "0.1.0"

[libraries]
epiccrown-jetpack-image = { group = "com.epiccrown", name = "zoomable-image", version.ref = "zoomable-image" }
```

Use it from your app module:

```kotlin
dependencies {
    implementation(libs.epiccrown.jetpack.image)
}
```

`com.epiccrown` requires the JitPack custom domain mapping for `epiccrown.com`. Configure a DNS TXT record for `git.epiccrown.com` pointing to `https://github.com/Orthoepiccrown0`, then build the tag at `https://jitpack.io/#com.epiccrown/jetpack-zoomable-image`.

## Basic Usage

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.epiccrown.zoomableimage.ZoomableImage
import com.epiccrown.zoomableimage.rememberZoomableImageState

@Composable
fun PhotoPreview(
    imageUrl: String,
    modifier: Modifier = Modifier,
) {
    val zoomState = rememberZoomableImageState()

    ZoomableImage(
        model = imageUrl,
        contentDescription = "Preview image",
        modifier = modifier,
        state = zoomState,
    )
}
```

`model` accepts the same kinds of values Coil supports, such as URLs, files, URIs, resource IDs, and custom image requests.

## Overlay Content

Use the `overlay` slot when additional UI needs to align with the displayed image. The slot receives the current `ZoomableImageState` and the fitted image rectangle inside the composable bounds.

```kotlin
ZoomableImage(
    model = imageUrl,
    contentDescription = "Annotated image",
    overlay = { state, contentRect ->
        // Draw boxes, labels, handles, or other image-relative content here.
        // Use state.scale and state.translation when content must track zoom/pan.
    },
)
```

This is useful for:

- ML detection bounding boxes
- Manual correction handles
- Crop guides
- Measurement overlays
- Read-only annotations

## State API

Create state with `rememberZoomableImageState()`:

```kotlin
val state = rememberZoomableImageState(
    minScale = 1f,
    maxScale = 5f,
)
```

`ZoomableImageState` exposes:

- `scale`: current zoom scale
- `translation`: current pan offset
- `isZoomed`: true when the scale is above the zoom threshold
- `snapTo(transform)`: immediately applies a `ZoomTransform`
- `reset()`: returns to minimum scale and zero translation
- `animateTo(transform)`: animates to a `ZoomTransform`

## Gesture Behavior

- One-finger dragging is enabled only after the image is zoomed.
- Pinch gestures zoom around the current gesture centroid.
- Double-tap zooms in to the configured double-tap scale, then double-tap again resets to minimum scale.
- Translation is clamped so the scaled content remains visible where possible.
- When the image is at or near minimum scale, translation is reset to `Offset.Zero`.

## Local Development

Run unit tests:

```powershell
.\gradlew.bat testDebugUnitTest
```

Build the release AAR:

```powershell
.\gradlew.bat assembleRelease
```

Publish to Maven Local for local app integration:

```powershell
.\gradlew.bat publishToMavenLocal
```

## License

MIT License. See [LICENSE](LICENSE).
