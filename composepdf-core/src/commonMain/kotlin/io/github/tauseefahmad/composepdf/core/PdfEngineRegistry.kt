package io.github.tauseefahmad.composepdf.core

/**
 * Resolves a rendering engine for a document session.
 *
 * AUTO prefers the platform-native engine, then PDFium, then a custom fallback.
 * Factories are invoked lazily and must return a fresh engine instance.
 */
class PdfEngineRegistry(
    private val nativeFactory: PdfEngineFactory? = null,
    private val pdfiumFactory: PdfEngineFactory? = null,
    private val customFactory: PdfEngineFactory? = null,
) {
    fun create(type: PdfEngineType = PdfEngineType.AUTO): PdfRenderEngine {
        val factory = when (type) {
            PdfEngineType.AUTO -> nativeFactory ?: pdfiumFactory ?: customFactory
            PdfEngineType.NATIVE -> nativeFactory
            PdfEngineType.PDFIUM -> pdfiumFactory
            PdfEngineType.CUSTOM -> customFactory
        } ?: throw IllegalStateException(
            "No engine is registered for $type. Register a supported factory or choose AUTO.",
        )
        return factory.create()
    }

    fun availableEngines(): Set<PdfEngineType> = buildSet {
        if (nativeFactory != null) add(PdfEngineType.NATIVE)
        if (pdfiumFactory != null) add(PdfEngineType.PDFIUM)
        if (customFactory != null) add(PdfEngineType.CUSTOM)
    }
}
