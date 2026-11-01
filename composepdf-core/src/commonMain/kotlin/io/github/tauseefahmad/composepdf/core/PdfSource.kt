package io.github.tauseefahmad.composepdf.core

/**
 * Describes where a PDF document should be loaded from.
 *
 * Platform-specific adapters are responsible for interpreting file paths and URIs.
 */
sealed interface PdfSource {
    data class File(val path: String) : PdfSource

    data class Bytes(val data: ByteArray) : PdfSource

    data class Url(
        val url: String,
        val headers: Map<String, String> = emptyMap(),
    ) : PdfSource

    data class Uri(val value: String) : PdfSource
}
