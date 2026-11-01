package io.github.tauseefahmad.composepdf.viewer

import io.github.tauseefahmad.composepdf.core.PdfError

/** Events emitted by ComposePDF for application-level integrations. */
sealed interface PdfViewerEvent {
    data object DocumentLoaded : PdfViewerEvent
    data class PageChanged(val pageNumber: Int) : PdfViewerEvent
    data class Error(val error: PdfError) : PdfViewerEvent
    data object DocumentClosed : PdfViewerEvent
}
