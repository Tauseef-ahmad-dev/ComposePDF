package io.github.tauseefahmad.composepdf.core

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.PDFKit.PDFDisplayBox
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFPage
import platform.UIKit.UIImagePNGRepresentation
import platform.CoreGraphics.CGSizeMake

/**
 * iOS backend backed by Apple's PDFKit.
 *
 * File paths, file URLs, and in-memory bytes are supported. Network downloads are
 * intentionally left to the application so authentication and caching stay explicit.
 */
class IosPdfKitEngine : PdfRenderEngine {
    private val mutex = Mutex()
    private var document: PDFDocument? = null

    override val name: String = "Apple PDFKit"
    override val capabilities = PdfEngineCapabilities(
        pageRendering = true,
        textExtraction = true,
        textSelection = false,
        annotations = true,
        forms = false,
        passwordProtection = true,
        hyperlinks = true,
        documentOutlines = true,
    )

    override suspend fun open(source: PdfSource, password: String?): PdfDocumentInfo =
        mutex.withLock {
            withContext(Dispatchers.Default) {
                document = null
                val opened = when (source) {
                    is PdfSource.File -> PDFDocument(url = NSURL.fileURLWithPath(source.path))
                    is PdfSource.Uri -> {
                        val url = NSURL.URLWithString(source.value)
                            ?: throw IllegalArgumentException("Invalid PDF URL: ${source.value}")
                        PDFDocument(url = url)
                    }
                    is PdfSource.Bytes -> PDFDocument(data = source.data.toNSData())
                    is PdfSource.Url -> throw UnsupportedOperationException(
                        "Pass downloaded bytes or a local file URL to PDFKit. Network loading is application-managed.",
                    )
                } ?: throw IllegalArgumentException("PDFKit could not open the supplied document.")

                if (opened.isLocked()) {
                    require(!password.isNullOrEmpty()) { "This PDF requires a password." }
                    require(opened.unlockWithPassword(password)) { "The PDF password is incorrect." }
                }
                document = opened
                PdfDocumentInfo(pageCount = opened.pageCount.toInt())
            }
        }

    override suspend fun getPageMetadata(pageIndex: Int): PdfPageMetadata =
        mutex.withLock {
            withContext(Dispatchers.Default) {
                val pdf = requireNotNull(document) { "No PDF document is open." }
                require(pageIndex in 0 until pdf.pageCount.toInt()) { "Page index is out of range." }
                val page = requireNotNull(pdf.pageAtIndex(pageIndex.toLong()))
                val bounds = page.boundsForBox(PDFDisplayBox.kPDFDisplayBoxMediaBox)
                PdfPageMetadata(
                    pageIndex = pageIndex,
                    width = bounds.size.width.toFloat(),
                    height = bounds.size.height.toFloat(),
                )
            }
        }

    override suspend fun renderPage(
        pageIndex: Int,
        options: PdfRenderOptions,
    ): ImageBitmap = mutex.withLock {
        withContext(Dispatchers.Default) {
            val pdf = requireNotNull(document) { "No PDF document is open." }
            require(pageIndex in 0 until pdf.pageCount.toInt()) { "Page index is out of range." }
            val page: PDFPage = requireNotNull(pdf.pageAtIndex(pageIndex.toLong()))
            val thumbnail = page.thumbnailOfSize(
                size = CGSizeMake(options.width.toDouble(), options.height.toDouble()),
                forBox = PDFDisplayBox.kPDFDisplayBoxMediaBox,
            )
            val png = UIImagePNGRepresentation(thumbnail)
                ?: throw IllegalStateException("PDFKit failed to encode the rendered page.")
            Image.makeFromEncoded(png.toByteArray()).toComposeImageBitmap()
        }
    }

    override suspend fun extractText(pageIndex: Int): PdfPageText = mutex.withLock {
        withContext(Dispatchers.Default) {
            val pdf = requireNotNull(document) { "No PDF document is open." }
            require(pageIndex in 0 until pdf.pageCount.toInt()) { "Page index is out of range." }
            val page = requireNotNull(pdf.pageAtIndex(pageIndex.toLong()))
            PdfPageText(pageIndex = pageIndex, text = page.string.orEmpty())
        }
    }

    override suspend fun close() = mutex.withLock {
        withContext(Dispatchers.Default) { document = null }
    }

    private fun ByteArray.toNSData(): NSData {
        require(isNotEmpty()) { "PDF data cannot be empty." }
        return usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
        }
    }

    private fun NSData.toByteArray(): ByteArray {
        val count = length.toInt()
        if (count == 0) return byteArrayOf()
        return bytes!!.readBytes(count)
    }
}

/** Creates a fresh PDFKit engine for each iOS document session. */
fun iosPdfKitEngineFactory(): PdfEngineFactory = PdfEngineFactory { IosPdfKitEngine() }
