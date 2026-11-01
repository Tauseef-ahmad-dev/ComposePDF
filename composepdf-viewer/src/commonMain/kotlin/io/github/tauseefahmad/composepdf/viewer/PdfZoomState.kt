package io.github.tauseefahmad.composepdf.viewer

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset

/** Controls how a page is sized inside the available viewer viewport. */
enum class PdfFitMode {
    /** Scale the page so its width matches the viewport. */
    FitWidth,

    /** Scale the entire page to fit inside the viewport. */
    FitPage,

    /** Display the page at its intrinsic 1:1 scale. */
    ActualSize,
}

/**
 * Common zoom and viewport state shared by all Compose Multiplatform targets.
 *
 * [scale] is a user-applied multiplier on top of the selected [fitMode].
 * Pan offsets are expressed in pixels in viewport coordinates.
 */
class PdfZoomState(
    initialScale: Float = 1f,
    val minScale: Float = 1f,
    val maxScale: Float = 5f,
) {
    private val _scale = mutableStateOf(initialScale.coerceIn(minScale, maxScale))
    val scale: State<Float> = _scale

    private val _fitMode = mutableStateOf(PdfFitMode.FitWidth)
    val fitMode: State<PdfFitMode> = _fitMode

    private val _pan = mutableStateOf(Offset.Zero)
    val pan: State<Offset> = _pan

    init {
        require(minScale > 0f) { "minScale must be greater than zero." }
        require(maxScale >= minScale) { "maxScale must be greater than or equal to minScale." }
    }

    fun zoomIn(step: Float = 0.25f) = setScale(_scale.value + step)

    fun zoomOut(step: Float = 0.25f) = setScale(_scale.value - step)

    fun setScale(scale: Float) {
        _scale.value = scale.coerceIn(minScale, maxScale)
        if (_scale.value <= 1f) resetPan()
    }

    fun setFitMode(mode: PdfFitMode) {
        _fitMode.value = mode
        reset()
    }

    fun toggleDoubleTapZoom() {
        setScale(if (_scale.value > 1f) 1f else 2f)
    }

    fun transform(zoomChange: Float, panChange: Offset) {
        val nextScale = (_scale.value * zoomChange).coerceIn(minScale, maxScale)
        _scale.value = nextScale
        _pan.value = if (nextScale <= 1f) Offset.Zero else _pan.value + panChange
    }

    fun setPan(offset: Offset) {
        _pan.value = if (_scale.value <= 1f) Offset.Zero else offset
    }

    fun resetPan() {
        _pan.value = Offset.Zero
    }

    fun reset() {
        _scale.value = 1f
        _pan.value = Offset.Zero
    }
}

/** Creates a remembered zoom state for a PDF viewer. */
@androidx.compose.runtime.Composable
fun rememberPdfZoomState(
    initialScale: Float = 1f,
    minScale: Float = 1f,
    maxScale: Float = 5f,
): PdfZoomState = androidx.compose.runtime.remember(initialScale, minScale, maxScale) {
    PdfZoomState(initialScale, minScale, maxScale)
}
