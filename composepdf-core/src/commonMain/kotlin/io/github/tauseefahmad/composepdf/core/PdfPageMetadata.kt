package io.github.tauseefahmad.composepdf.core

/** Page dimensions are expressed in PDF points (1/72 inch). */
data class PdfPageMetadata(
    val pageIndex: Int,
    val width: Float,
    val height: Float,
    val rotationDegrees: Int = 0,
) {
    init {
        require(pageIndex >= 0) { "pageIndex must be zero or greater." }
        require(width > 0f && height > 0f) { "Page dimensions must be positive." }
    }
}
