package io.github.tauseefahmad.composepdf.pdfium.android

import io.github.tauseefahmad.composepdf.core.*

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.legere.pdfiumandroid.suspend.PdfDocumentKt
import io.legere.pdfiumandroid.suspend.PdfiumCoreKt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android PDFium backend.
 *
 * PDFium is useful when text extraction and broader PDF compatibility are needed.
 * This adapter reads sources into memory before opening; applications should avoid
 * using it for very large documents without an application-level download/cache layer.
 */
class AndroidPdfiumEngine(
    context: Context,
) : PdfRenderEngine {
    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private val core = PdfiumCoreKt(Dispatchers.IO)
    private var document: PdfDocumentKt? = null

    override val name: String = "PDFium Android"
    override val capabilities = PdfEngineCapabilities(
        pageRendering = true,
        textExtraction = true,
        textSelection = false,
        annotations = true,
        forms = false,
        passwordProtection = true,
        hyperlinks = true,
        documentOutlines = false,
    )

    override suspend fun open(source: PdfSource, password: String?): PdfDocumentInfo =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                closeDocument()
                try {
                    val resolvedSource = source.downloadIfRemote()
                    val bytes = when (resolvedSource) {
                        is PdfSource.File -> File(source.path).readBytes()
                        is PdfSource.Bytes -> source.data
                        is PdfSource.Uri -> {
                            val input = appContext.contentResolver.openInputStream(Uri.parse(source.value))
                                ?: throw IllegalArgumentException("Unable to open PDF URI: ${source.value}")
                            input.use { it.readBytes() }
                        }
                        is PdfSource.Url -> {
                            val connection = URL(source.url).openConnection().apply {
                                source.headers.forEach { (key, value) -> setRequestProperty(key, value) }
                                connectTimeout = 15_000
                                readTimeout = 30_000
                            }
                            connection.getInputStream().use { it.readBytes() }
                        }
                    }
                    require(bytes.isNotEmpty()) { "PDF data cannot be empty." }
                    val opened = core.newDocument(bytes, password)
                    document = opened
                    PdfDocumentInfo(pageCount = opened.getPageCount())
                } catch (throwable: Throwable) {
                    closeDocument()
                    throw throwable
                }
            }
        }

    override suspend fun getPageMetadata(pageIndex: Int): PdfPageMetadata =
        mutex.withLock {
            val pdf = requireNotNull(document) { "No PDF document is open." }
            val count = pdf.getPageCount()
            require(pageIndex in 0 until count) { "Page index is out of range." }
            requireNotNull(pdf.openPage(pageIndex)) { "Unable to open PDF page $pageIndex." }.use { page ->
                PdfPageMetadata(
                    pageIndex = pageIndex,
                    width = page.getPageWidthPoint().toFloat(),
                    height = page.getPageHeightPoint().toFloat(),
                    rotationDegrees = page.getPageRotation() * 90,
                )
            }
        }

    override suspend fun renderPage(
        pageIndex: Int,
        options: PdfRenderOptions,
    ): ImageBitmap = mutex.withLock {
        withContext(Dispatchers.IO) {
        val pdf = requireNotNull(document) { "No PDF document is open." }
        require(pageIndex in 0 until pdf.getPageCount()) { "Page index is out of range." }
        val bitmap = Bitmap.createBitmap(options.width, options.height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        requireNotNull(pdf.openPage(pageIndex)) { "Unable to open PDF page $pageIndex." }.use { page ->
            page.renderPageBitmap(
                bitmap = bitmap,
                startX = 0,
                startY = 0,
                drawSizeX = options.width,
                drawSizeY = options.height,
                renderAnnot = options.renderAnnotations,
            )
        }
        bitmap.asImageBitmap()
        }
    }

    override suspend fun extractText(pageIndex: Int): PdfPageText = mutex.withLock {
        val pdf = requireNotNull(document) { "No PDF document is open." }
        require(pageIndex in 0 until pdf.getPageCount()) { "Page index is out of range." }
        requireNotNull(pdf.openPage(pageIndex)) { "Unable to open PDF page $pageIndex." }.use { page ->
            page.openTextPage().use { textPage ->
                val count = textPage.textPageCountChars()
                PdfPageText(
                    pageIndex = pageIndex,
                    text = if (count > 0) textPage.textPageGetText(0, count).orEmpty() else "",
                )
            }
        }
    }

    override suspend fun close() = mutex.withLock {
        withContext(Dispatchers.IO) { closeDocument() }
    }

    private fun closeDocument() {
        runCatching { document?.close() }
        document = null
    }
}

/** Creates a fresh PDFium-backed Android engine for each viewer session. */
fun androidPdfiumEngineFactory(context: Context): PdfEngineFactory =
    PdfEngineFactory { AndroidPdfiumEngine(context.applicationContext) }
