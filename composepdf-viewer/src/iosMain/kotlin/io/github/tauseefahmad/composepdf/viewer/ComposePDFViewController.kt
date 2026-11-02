package io.github.tauseefahmad.composepdf.viewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import io.github.tauseefahmad.composepdf.core.PdfSource
import io.github.tauseefahmad.composepdf.core.iosPdfKitEngineFactory
import platform.UIKit.UIViewController

/**
 * Creates a UIKit view controller that presents the ComposePDF viewer for a remote PDF.
 *
 * The URL is loaded by the shared Ktor downloader before the iOS PDFKit engine opens it.
 * Use an HTTPS URL whenever possible. Authentication headers can be added through a future
 * overload; this convenience entry point is intended for publicly accessible PDF URLs.
 */
fun composePdfViewController(url: String): UIViewController {
    require(url.startsWith("https://", ignoreCase = true) ||
        url.startsWith("http://", ignoreCase = true)) {
        "PDF URL must use HTTP or HTTPS."
    }

    return ComposeUIViewController {
        val engineFactory = remember { iosPdfKitEngineFactory() }
        val state = rememberPdfViewerState(engineFactory)

        ComposePDF(
            source = PdfSource.Url(url),
            engineFactory = engineFactory,
            state = state,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
