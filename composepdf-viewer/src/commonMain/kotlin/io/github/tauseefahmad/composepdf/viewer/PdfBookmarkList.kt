package io.github.tauseefahmad.composepdf.viewer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.tauseefahmad.composepdf.core.PdfBookmark

/**
 * Displays a loaded PDF outline. Call [PdfViewerState.loadBookmarks] after opening
 * a document; engines without outline support return an empty list.
 */
@Composable
fun PdfBookmarkList(
    state: PdfViewerState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val rows = remember(state.bookmarks) {
        buildList {
            fun append(bookmarks: List<PdfBookmark>, depth: Int) {
                bookmarks.forEach { bookmark ->
                    add(BookmarkRow(bookmark, depth))
                    append(bookmark.children, depth + 1)
                }
            }
            append(state.bookmarks, 0)
        }
    }
    LazyColumn(modifier = modifier) {
        items(rows) { row ->
            Text(
                text = row.bookmark.title.ifBlank { "Untitled bookmark" },
                style = MaterialTheme.typography.bodyMedium,
                color = if (row.bookmark.pageIndex != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .padding(start = (12 + row.depth * 16).dp, top = 8.dp, bottom = 8.dp, end = 12.dp)
                    .then(
                        if (row.bookmark.pageIndex != null) {
                            Modifier.clickable { row.bookmark.pageIndex?.let { pageIndex -> scope.launch { state.goToPage(pageIndex) } } }
                        } else Modifier
                    ),
            )
        }
    }
}

private data class BookmarkRow(val bookmark: PdfBookmark, val depth: Int)
