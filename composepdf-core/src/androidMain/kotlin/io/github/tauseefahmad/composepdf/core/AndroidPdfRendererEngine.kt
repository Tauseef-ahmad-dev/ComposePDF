package io.github.tauseefahmad.composepdf.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Android implementation backed by the platform PdfRenderer API.
 *
 * Supports local files, content URIs, and in-memory byte arrays. Remote URLs and
 * password-protected documents require a different backend or a future adapter.
 */
class AndroidPdfRendererEngine(
    context: Context,
) : PdfRenderEngine {
    private val appContext = context.applicationContext
    private val mutex = Mutex()

    private var descriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private var temporaryFile: File? = null

    override val name: String = "Android PdfRenderer"

    override val capabilities = PdfEngineCapabilities(
        pageRendering = true,
        textExtraction = false,
        textSelection = false,
        annotations = false,
        forms = false,
        passwordProtection = false,
        hyperlinks = false,
        documentOutlines = false,
    )

    override suspend fun open(
        source: PdfSource,
        password: String?,
    ): PdfDocumentInfo = mutex.withLock {
        withContext(Dispatchers.IO) {
            closeResources()
            try {
                val resolvedSource = source.downloadIfRemote()
                require(password == null) {
                    "The Android PdfRenderer backend does not support password-protected PDFs."
                }

                val fileDescriptor = when (resolvedSource) {
                    is PdfSource.File -> ParcelFileDescriptor.open(
                        File(source.path),
                        ParcelFileDescriptor.MODE_READ_ONLY,
                    )

                    is PdfSource.Bytes -> {
                        val file = File.createTempFile("composepdf-", ".pdf", appContext.cacheDir)
                        temporaryFile = file
                        FileOutputStream(file).use { it.write(source.data) }
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    }

                    is PdfSource.Uri -> {
                        val file = File.createTempFile("composepdf-uri-", ".pdf", appContext.cacheDir)
                        temporaryFile = file
                        val input = appContext.contentResolver.openInputStream(Uri.parse(source.value))
                            ?: throw IllegalArgumentException("Unable to open PDF URI: ${source.value}")
                        input.use { sourceStream ->
                            FileOutputStream(file).use { targetStream ->
                                sourceStream.copyTo(targetStream)
                            }
                        }
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    }

                    is PdfSource.Url -> throw UnsupportedOperationException(
                        "URL loading is not included in the Android PdfRenderer backend yet.",
                    )
                }

                descriptor = fileDescriptor
                renderer = PdfRenderer(fileDescriptor)
                PdfDocumentInfo(pageCount = requireNotNull(renderer).pageCount)
            } catch (throwable: Throwable) {
                closeResources()
                throw throwable
            }
        }
    }

    override suspend fun getPageMetadata(pageIndex: Int): PdfPageMetadata = mutex.withLock {
        withContext(Dispatchers.IO) {
            val pdfRenderer = requireNotNull(renderer) { "No PDF document is open." }
            require(pageIndex in 0 until pdfRenderer.pageCount) { "Page index is out of range." }

            pdfRenderer.openPage(pageIndex).use { page ->
                PdfPageMetadata(
                    pageIndex = pageIndex,
                    width = page.width.toFloat(),
                    height = page.height.toFloat(),
                )
            }
        }
    }

    override suspend fun renderPage(
        pageIndex: Int,
        options: PdfRenderOptions,
    ): ImageBitmap = mutex.withLock {
        withContext(Dispatchers.IO) {
            val pdfRenderer = requireNotNull(renderer) { "No PDF document is open." }
            require(pageIndex in 0 until pdfRenderer.pageCount) { "Page index is out of range." }

            pdfRenderer.openPage(pageIndex).use { page ->
                val bitmap = Bitmap.createBitmap(
                    options.width,
                    options.height,
                    Bitmap.Config.ARGB_8888,
                )
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(
                    bitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                )
                bitmap.asImageBitmap()
            }
        }
    }

    override suspend fun close() = mutex.withLock {
        withContext(Dispatchers.IO) {
            closeResources()
        }
    }

    private fun closeResources() {
        runCatching { renderer?.close() }
        renderer = null
        runCatching { descriptor?.close() }
        descriptor = null
        temporaryFile?.delete()
        temporaryFile = null
    }
}

/** Creates the built-in Android PdfRenderer engine for the supplied application context. */
fun androidPdfEngineFactory(context: Context): PdfEngineFactory =
    PdfEngineFactory { AndroidPdfRendererEngine(context.applicationContext) }
