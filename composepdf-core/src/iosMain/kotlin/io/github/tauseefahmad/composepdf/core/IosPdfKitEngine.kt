package io.github.tauseefahmad.composepdf.core

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.create
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
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class IosPdfKitEngine : PdfRenderEngine {
    private val mutex = Mutex()
    private var document: PDFDocument? = null

    override val name: String = "Apple PDFKit"
    override val capabilities = PdfEngineCapabilities(
        pageRendering = true,
        textExtraction = true,
        textSelection = false,
        annotations = false,
        forms = false,
        passwordProtection = true,
        hyperlinks = false,
        documentOutlines = false,
    )

    override suspend fun open(source: PdfSource, password: String?): PdfDocumentInfo =
        mutex.withLock {
            withContext(Dispatchers.Default) {
                document = null
                val resolvedSource = source.downloadIfRemote()
                val opened = when (resolvedSource) {
                    is PdfSource.File -> PDFDocument(NSURL.fileURLWithPath(resolvedSource.path))
                    is PdfSource.Uri -> {
                        val url = NSURL.URLWithString(resolvedSource.value)
                            ?: throw IllegalArgumentException("Invalid PDF URL: ${resolvedSource.value}")
                        PDFDocument(url)
                    }
                    is PdfSource.Bytes -> PDFDocument(resolvedSource.data.toNSData())
                    is PdfSource.Url -> throw UnsupportedOperationException(
                        "Pass downloaded bytes or a local file URL to PDFKit. Network loading is application-managed.",
                    )
                } ?: throw IllegalArgumentException("PDFKit could not open the supplied document.")

                if (opened.isLocked) {
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
                val page = requireNotNull(pdf.pageAtIndex(pageIndex.toULong()))
                val bounds = page.boundsForBox(0L)
                PdfPageMetadata(
                    pageIndex = pageIndex,
                    width = bounds.useContents { size.width.toFloat() },
                    height = bounds.useContents { size.height.toFloat() },
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
            val page: PDFPage = requireNotNull(pdf.pageAtIndex(pageIndex.toULong()))
            val thumbnail = page.thumbnailOfSize(
                size = CGSizeMake(options.width.toDouble(), options.height.toDouble()),
                forBox = 0L,
            )
            val png = UIImagePNGRepresentation(thumbnail)
                ?: throw IllegalStateException("PDFKit failed to encode the rendered page.")
            png.toByteArray().decodeToImageBitmap()
        }
    }

    override suspend fun extractText(pageIndex: Int): PdfPageText = mutex.withLock {
        withContext(Dispatchers.Default) {
            val pdf = requireNotNull(document) { "No PDF document is open." }
            require(pageIndex in 0 until pdf.pageCount.toInt()) { "Page index is out of range." }
            val page = requireNotNull(pdf.pageAtIndex(pageIndex.toULong()))
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
        val source = bytes!!.reinterpret<ByteVar>()
        return source.readBytes(count)
    }
}

/** Creates a fresh PDFKit engine for each iOS document session. */
fun iosPdfKitEngineFactory(): PdfEngineFactory = PdfEngineFactory { IosPdfKitEngine() }
