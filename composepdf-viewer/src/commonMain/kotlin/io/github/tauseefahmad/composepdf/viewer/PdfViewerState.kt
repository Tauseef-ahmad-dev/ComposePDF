package io.github.tauseefahmad.composepdf.viewer

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import io.github.tauseefahmad.composepdf.core.PdfDocumentInfo
import io.github.tauseefahmad.composepdf.core.PdfEngineFactory
import io.github.tauseefahmad.composepdf.core.PdfError
import io.github.tauseefahmad.composepdf.core.PdfRenderOptions
import io.github.tauseefahmad.composepdf.core.PdfSource
import io.github.tauseefahmad.composepdf.core.PdfRenderEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex

/** Owns a single viewer document session and its bounded rendered-page cache. */
class PdfViewerState internal constructor(
    private val engineFactory: PdfEngineFactory,
) {
    private val _documentInfo = mutableStateOf<PdfDocumentInfo?>(null)
    val documentInfo: State<PdfDocumentInfo?> = _documentInfo

    private val _currentPage = mutableStateOf(0)
    /** Zero-based index of the page currently considered active. */
    val currentPage: State<Int> = _currentPage

    private val _loadingState = mutableStateOf<PdfLoadingState>(PdfLoadingState.Idle)
    val loadingState: State<PdfLoadingState> = _loadingState

    private val _error = mutableStateOf<PdfError?>(null)
    val error: State<PdfError?> = _error

    private val renderedPages = mutableStateMapOf<Int, ImageBitmap>()
    private val cacheOrder = mutableStateListOf<Int>()
    private val renderMutex = Mutex()
    private val maxCachedPages = 12
    private var engine: PdfRenderEngine? = null

    internal fun pageImage(pageIndex: Int): ImageBitmap? = renderedPages[pageIndex]

    private val pageMetadataCache = mutableStateMapOf<Int, io.github.tauseefahmad.composepdf.core.PdfPageMetadata>()

    internal suspend fun pageMetadata(pageIndex: Int): io.github.tauseefahmad.composepdf.core.PdfPageMetadata {
        pageMetadataCache[pageIndex]?.let { return it }
        val metadata = requireNotNull(engine) { "No PDF document is open." }.getPageMetadata(pageIndex)
        pageMetadataCache[pageIndex] = metadata
        return metadata
    }

    suspend fun open(source: PdfSource) {
        renderMutex.lock()
        try {
            closeEngine()
            clearPageCache()
            _documentInfo.value = null
            _currentPage.value = 0
            _error.value = null
            _loadingState.value = PdfLoadingState.Loading

            val newEngine = engineFactory.create()
            engine = newEngine
            try {
                _documentInfo.value = newEngine.open(source)
                _loadingState.value = PdfLoadingState.Loaded
            } catch (cancellation: CancellationException) {
                closeEngine()
                _loadingState.value = PdfLoadingState.Idle
                throw cancellation
            } catch (throwable: Throwable) {
                _error.value = PdfError.Unknown(throwable.message)
                _loadingState.value = PdfLoadingState.Failed
                closeEngine()
            }
        } finally {
            renderMutex.unlock()
        }
    }

    suspend fun goToPage(pageIndex: Int) {
        val count = _documentInfo.value?.pageCount ?: return
        if (count > 0) _currentPage.value = pageIndex.coerceIn(0, count - 1)
    }

    suspend fun nextPage() = goToPage(_currentPage.value + 1)

    suspend fun previousPage() = goToPage(_currentPage.value - 1)

    internal fun updateCurrentPage(pageIndex: Int) {
        val count = _documentInfo.value?.pageCount ?: return
        if (count > 0) _currentPage.value = pageIndex.coerceIn(0, count - 1)
    }

    internal suspend fun renderPage(pageIndex: Int, targetWidth: Int): ImageBitmap {
        renderMutex.lock()
        try {
            renderedPages[pageIndex]?.let {
                cacheOrder.remove(pageIndex)
                cacheOrder.add(pageIndex)
                return it
            }

            val activeEngine = requireNotNull(engine) { "No PDF document is open." }
            val metadata = pageMetadata(pageIndex)
            val targetHeight = (targetWidth * metadata.height / metadata.width).toInt().coerceAtLeast(1)
            val bitmap = activeEngine.renderPage(
                pageIndex = pageIndex,
                options = PdfRenderOptions(width = targetWidth, height = targetHeight),
            )
            renderedPages[pageIndex] = bitmap
            cacheOrder.remove(pageIndex)
            cacheOrder.add(pageIndex)

            while (cacheOrder.size > maxCachedPages) {
                val evictedPage = cacheOrder.removeAt(0)
                renderedPages.remove(evictedPage)
            }
            return bitmap
        } finally {
            renderMutex.unlock()
        }
    }

    suspend fun close() {
        renderMutex.lock()
        try {
            closeEngine()
            clearPageCache()
            _documentInfo.value = null
            _currentPage.value = 0
            _loadingState.value = PdfLoadingState.Idle
        } finally {
            renderMutex.unlock()
        }
    }

    private fun clearPageCache() {
        renderedPages.clear()
        cacheOrder.clear()
        pageMetadataCache.clear()
    }

    private suspend fun closeEngine() {
        val activeEngine = engine
        engine = null
        if (activeEngine != null) runCatching { activeEngine.close() }
    }
}
