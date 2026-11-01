package io.github.tauseefahmad.composepdf.core

/** Options used to rasterize a PDF page. */
data class PdfRenderOptions(
    val width: Int,
    val height: Int,
    val renderAnnotations: Boolean = true,
    val renderFormFields: Boolean = true,
    val renderGrayscale: Boolean = false,
) {
    init {
        require(width > 0) { "width must be positive." }
        require(height > 0) { "height must be positive." }
    }
}
