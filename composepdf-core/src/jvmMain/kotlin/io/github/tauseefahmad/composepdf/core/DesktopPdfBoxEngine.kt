package io.github.tauseefahmad.composepdf.core

import androidx.compose.ui.graphics.ImageBitmap
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

/**
 * Desktop JVM backend using Apache PDFBox.
 *
 * PDFBox is Apache-2.0 licensed and runs without platform-specific native binaries.
 */
class DesktopPdfBoxEngine : PdfRenderEngine {
    private val mutex = Mutex()
    private var document: PDDocument? = null
    private var renderer: PDFRenderer? = null

    override val name: String = "Apache PDFBox"
    override val capabilities = PdfEngineCapabilities(
        pageRendering = true,
        textExtraction = true,
        textSelection = false,
        annotations = false,
        forms = false,
        passwordProtection = true,
        hyperlinks = false,
        documentOutlines = true,
    )

    override suspend fun open(source: PdfSource, password: String?): PdfDocumentInfo =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                closeDocument()
                val opened = when (source) {
                    is PdfSource.File -> if (password == null) Loader.loadPDF(File(source.path))
                        else Loader.loadPDF(File(source.path), password)
                    is PdfSource.Bytes -> if (password == null) Loader.loadPDF(source.data)
                        else Loader.loadPDF(source.data, password)
                    is PdfSource.Url -> {
                        val connection = URL(source.url).openConnection().apply {
                            source.headers.forEach { (key, value) -> setRequestProperty(key, value) }
                            connectTimeout = 15_000
                            readTimeout = 30_000
                        }
                        val bytes = connection.getInputStream().use { it.readBytes() }
                        if (password == null) Loader.loadPDF(bytes) else Loader.loadPDF(bytes, password)
                    }
                    is PdfSource.Uri -> {
                        val uriFile = File(source.value.removePrefix("file://"))
                        if (password == null) Loader.loadPDF(uriFile) else Loader.loadPDF(uriFile, password)
                    }
                }
                document = opened
                renderer = PDFRenderer(opened)
                PdfDocumentInfo(pageCount = opened.numberOfPages)
            }
        }

    override suspend fun getPageMetadata(pageIndex: Int): PdfPageMetadata =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val pdf = requireNotNull(document) { "No PDF document is open." }
                require(pageIndex in 0 until pdf.numberOfPages) { "Page index is out of range." }
                val page = pdf.getPage(pageIndex)
                PdfPageMetadata(
                    pageIndex = pageIndex,
                    width = page.mediaBox.width,
                    height = page.mediaBox.height,
                )
            }
        }

    override suspend fun renderPage(
        pageIndex: Int,
        options: PdfRenderOptions,
    ): ImageBitmap = mutex.withLock {
        withContext(Dispatchers.IO) {
            val pdf = requireNotNull(document) { "No PDF document is open." }
            require(pageIndex in 0 until pdf.numberOfPages) { "Page index is out of range." }
            val pageWidth = pdf.getPage(pageIndex).mediaBox.width
            val dpi = (options.width.toFloat() / pageWidth) * 72f
            val image = requireNotNull(renderer).renderImageWithDPI(
                pageIndex,
                dpi,
                ImageType.ARGB,
            )
            val bitmap = ImageBitmap(image.width, image.height)
            val pixels = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
            bitmap.writePixels(
                buffer = pixels,
                width = image.width,
                height = image.height,
                stride = image.width,
            )
            bitmap
        }
    }

    override suspend fun extractText(pageIndex: Int): PdfPageText = mutex.withLock {
        withContext(Dispatchers.IO) {
            val pdf = requireNotNull(document) { "No PDF document is open." }
            require(pageIndex in 0 until pdf.numberOfPages) { "Page index is out of range." }
            val stripper = org.apache.pdfbox.text.PDFTextStripper().apply {
                startPage = pageIndex + 1
                endPage = pageIndex + 1
            }
            PdfPageText(pageIndex = pageIndex, text = stripper.getText(pdf).trimEnd())
        }
    }

    override suspend fun getDocumentOutline(): List<PdfBookmark> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val pdf = requireNotNull(document) { "No PDF document is open." }
            val outline = pdf.documentCatalog.documentOutline ?: return@withContext emptyList()
            fun readChildren(parent: org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode): List<PdfBookmark> {
                val result = mutableListOf<PdfBookmark>()
                var item = parent.firstChild
                while (item != null) {
                    val destination = (item.destination as? PDPageDestination)
                        ?: ((item.action as? PDActionGoTo)?.destination as? PDPageDestination)
                    val pageIndex = destination?.retrievePageNumber()?.takeIf { it >= 0 }
                    result += PdfBookmark(
                        title = item.title.orEmpty(),
                        pageIndex = pageIndex,
                        children = readChildren(item),
                    )
                    item = item.nextSibling
                }
                return result
            }
            readChildren(outline)
        }
    }

    override suspend fun close() = mutex.withLock {
        withContext(Dispatchers.IO) { closeDocument() }
    }

    private fun closeDocument() {
        runCatching { document?.close() }
        document = null
        renderer = null
    }
}

/** Factory for a new PDFBox-backed engine per viewer session. */
fun desktopPdfBoxEngineFactory(): PdfEngineFactory = PdfEngineFactory { DesktopPdfBoxEngine() }
