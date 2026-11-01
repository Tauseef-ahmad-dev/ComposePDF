package io.github.tauseefahmad.composepdf.core

/**
 * A navigable entry in a PDF document outline.
 *
 * [pageIndex] is zero-based. A null page index represents an outline entry
 * without a directly resolvable page destination.
 */
data class PdfBookmark(
    val title: String,
    val pageIndex: Int?,
    val children: List<PdfBookmark> = emptyList(),
)
