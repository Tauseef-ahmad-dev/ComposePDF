package io.github.tauseefahmad.composepdf.core

/** Extracted text and its page-relative bounds. Coordinates use PDF points. */
data class PdfPageText(
    val pageIndex: Int,
    val text: String,
    val blocks: List<PdfTextBlock> = emptyList(),
)

data class PdfTextBlock(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)
