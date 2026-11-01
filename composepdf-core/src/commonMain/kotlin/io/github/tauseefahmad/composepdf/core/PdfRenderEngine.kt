package io.github.tauseefahmad.composepdf.core

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Platform rendering contract.
 *
 * Implementations must document their threading constraints and release all native
 * document/page handles from [close].
 */
interface PdfRenderEngine {
    val name: String
    val capabilities: PdfEngineCapabilities

    suspend fun open(source: PdfSource, password: String? = null): PdfDocumentInfo

    suspend fun getPageMetadata(pageIndex: Int): PdfPageMetadata

    suspend fun renderPage(
        pageIndex: Int,
        options: PdfRenderOptions,
    ): ImageBitmap

    suspend fun extractText(pageIndex: Int): PdfPageText {
        throw UnsupportedOperationException("Text extraction is not supported by $name.")
    }

    suspend fun close()
}

/** Creates an engine instance for a viewer document session. */
fun interface PdfEngineFactory {
    fun create(): PdfRenderEngine
}
