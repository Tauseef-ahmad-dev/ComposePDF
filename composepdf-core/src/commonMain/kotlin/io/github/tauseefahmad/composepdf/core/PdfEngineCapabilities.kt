package io.github.tauseefahmad.composepdf.core

/** Declares what a concrete rendering backend can actually do. */
data class PdfEngineCapabilities(
    val pageRendering: Boolean = true,
    val textExtraction: Boolean = false,
    val textSelection: Boolean = false,
    val annotations: Boolean = false,
    val forms: Boolean = false,
    val passwordProtection: Boolean = false,
    val hyperlinks: Boolean = false,
    val documentOutlines: Boolean = false,
)
