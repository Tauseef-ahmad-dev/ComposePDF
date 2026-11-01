package io.github.tauseefahmad.composepdf.viewer

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import io.github.tauseefahmad.composepdf.core.PdfBookmark
import io.github.tauseefahmad.composepdf.core.PdfDocumentInfo
import io.github.tauseefahmad.composepdf.core.PdfEngineFactory
import io.github.tauseefahmad.composepdf.core.PdfError
import io.github.tauseefahmad.composepdf.core.PdfRenderOptions
import io.github.tauseefahmad.composepdf.core.PdfSource
import io.github.tauseefahmad.composepdf.core.PdfRenderEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex

/** A text occurrence found in a PDF page. Offsets refer to the extracted page text. */
data class PdfSearchResult(
    val pageIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val matchedText: String,
)

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

    private val _bookmarks = mutableStateListOf<PdfBookmark>()
    /** Hierarchical document outline; empty when unsupported or when no outline exists. */
    val bookmarks: List<PdfBookmark> get() = _bookmarks

    private val _searchResults = mutableStateListOf<PdfSearchResult>()
    /** Results for the most recent document-wide text search. */
    val searchResults: List<PdfSearchResult> get() = _searchResults

    private val _activeSearchResultIndex = mutableStateOf(-1)
    val activeSearchResultIndex: State<Int> = _activeSearchResultIndex

    /** True when the active engine advertises text extraction support. */
    val supportsTextSearch: Boolean get() = engine?.capabilities?.textExtraction == true
    val supportsDocumentOutlines: Boolean get() = engine?.capabilities?.documentOutlines == true

    private val renderedPages = mutableStateMapOf<Int, ImageBitmap>()
    private val cacheOrder = mutableStateListOf<Int>()
    private val renderMutex = Mutex()
    private var maxCachedPages = 12
    private var engine: PdfRenderEngine? = null

    internal fun pageImage(pageIndex: Int): ImageBitmap? = renderedPages[pageIndex]

    /** Applies the viewer's configured bitmap-cache limit and evicts oldest pages immediately. */
    internal fun configurePageCache(maxPages: Int) {
        require(maxPages >= 1) { "maxCachedPages must be at least 1." }
        maxCachedPages = maxPages
        while (cacheOrder.size > maxCachedPages) {
            val evictedPage = cacheOrder.removeAt(0)
            renderedPages.remove(evictedPage)
        }
    }

    private val pageMetadataCache = mutableStateMapOf<Int, io.github.tauseefahmad.composepdf.core.PdfPageMetadata>()

    internal suspend fun pageMetadata(pageIndex: Int): io.github.tauseefahmad.composepdf.core.PdfPageMetadata {
        pageMetadataCache[pageIndex]?.let { return it }
        val metadata = requireNotNull(engine) { "No PDF document is open." }.getPageMetadata(pageIndex)
        pageMetadataCache[pageIndex] = metadata
        return metadata
    }

    /**
     * Searches extracted text on every page. Results are ordered by page and text offset.
     * This operation is available only when the configured engine supports text extraction.
     */
    suspend fun search(query: String): List<PdfSearchResult> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) {
            clearSearch()
            return emptyList()
        }

        renderMutex.lock()
        try {
            val activeEngine = requireNotNull(engine) { "No PDF document is open." }
            check(activeEngine.capabilities.textExtraction) {
                "Text search is not supported by ${activeEngine.name}. Use an engine with text extraction support."
            }
            val pageCount = _documentInfo.value?.pageCount ?: 0
            val matches = mutableListOf<PdfSearchResult>()
            for (pageIndex in 0 until pageCount) {
                val text = activeEngine.extractText(pageIndex).text
                var offset = 0
                while (offset <= text.length - normalizedQuery.length) {
                    val foundAt = text.indexOf(normalizedQuery, startIndex = offset, ignoreCase = true)
                    if (foundAt < 0) break
                    matches += PdfSearchResult(
                        pageIndex = pageIndex,
                        startOffset = foundAt,
                        endOffset = foundAt + normalizedQuery.length,
                        matchedText = text.substring(foundAt, foundAt + normalizedQuery.length),
                    )
                    offset = foundAt + normalizedQuery.length
                }
            }
            _searchResults.clear()
            _searchResults.addAll(matches)
            _activeSearchResultIndex.value = if (matches.isEmpty()) -1 else 0
            matches.firstOrNull()?.let { _currentPage.value = it.pageIndex }
            return matches
        } finally {
            renderMutex.unlock()
        }
    }

    /** Loads the document outline when the active engine supports it. */
    suspend fun loadBookmarks(): List<PdfBookmark> {
        renderMutex.lock()
        try {
            val activeEngine = requireNotNull(engine) { "No PDF document is open." }
            if (!activeEngine.capabilities.documentOutlines) {
                _bookmarks.clear()
                return emptyList()
            }
            val outline = activeEngine.getDocumentOutline()
            _bookmarks.clear()
            _bookmarks.addAll(outline)
            return outline
        } finally {
            renderMutex.unlock()
        }
    }

    fun clearSearch() {
        _searchResults.clear()
        _activeSearchResultIndex.value = -1
    }

    suspend fun nextSearchResult() = moveSearchResult(1)

    suspend fun previousSearchResult() = moveSearchResult(-1)

    private suspend fun moveSearchResult(direction: Int) {
        if (_searchResults.isEmpty()) return
        val nextIndex = (_activeSearchResultIndex.value + direction)
            .mod(_searchResults.size)
        _activeSearchResultIndex.value = nextIndex
        goToPage(_searchResults[nextIndex].pageIndex)
    }

    suspend fun open(source: PdfSource) {
        renderMutex.lock()
        try {
            closeEngine()
            clearPageCache()
            clearSearch()
            _bookmarks.clear()
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

    /** Renders a lightweight preview without polluting the full-page LRU cache. */
    internal suspend fun renderThumbnail(pageIndex: Int, targetWidth: Int = 120): ImageBitmap {
        renderMutex.lock()
        try {
            val activeEngine = requireNotNull(engine) { "No PDF document is open." }
            val metadata = pageMetadata(pageIndex)
            val targetHeight = (targetWidth * metadata.height / metadata.width).toInt().coerceAtLeast(1)
            return activeEngine.renderPage(
                pageIndex = pageIndex,
                options = PdfRenderOptions(width = targetWidth, height = targetHeight),
            )
        } finally {
            renderMutex.unlock()
        }
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
            clearSearch()
            _bookmarks.clear()
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
