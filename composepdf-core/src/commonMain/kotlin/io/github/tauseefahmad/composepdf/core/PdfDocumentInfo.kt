package io.github.tauseefahmad.composepdf.core

/** Metadata available after a document has been opened. */
data class PdfDocumentInfo(
    val pageCount: Int,
    val title: String? = null,
    val author: String? = null,
    val subject: String? = null,
    val creator: String? = null,
    val producer: String? = null,
)
