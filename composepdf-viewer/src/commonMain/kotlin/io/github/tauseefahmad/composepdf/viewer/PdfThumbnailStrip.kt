package io.github.tauseefahmad.composepdf.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/**
 * Horizontally scrollable page previews. Images are rendered only as their lazy
 * items enter composition and are not stored in the full-page render cache.
 */
@Composable
fun PdfThumbnailStrip(
    state: PdfViewerState,
    modifier: Modifier = Modifier,
    thumbnailWidth: Int = 120,
    onPageSelected: (Int) -> Unit = {},
) {
    val pageCount = state.documentInfo.value?.pageCount ?: 0
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(count = pageCount, key = { it }) { pageIndex ->
            val thumbnail = remember(state, pageIndex, thumbnailWidth) {
                mutableStateOf<ImageBitmap?>(null)
            }
            val failed = remember(state, pageIndex, thumbnailWidth) { mutableStateOf(false) }
            LaunchedEffect(state, pageIndex, thumbnailWidth) {
                runCatching { state.renderThumbnail(pageIndex, thumbnailWidth) }
                    .onSuccess { thumbnail.value = it }
                    .onFailure { failed.value = true }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .width(thumbnailWidth.dp)
                    .clickable { onPageSelected(pageIndex) },
            ) {
                when {
                    thumbnail.value != null -> Image(
                        bitmap = thumbnail.value!!,
                        contentDescription = "Preview of page ${pageIndex + 1}",
                        modifier = Modifier.width(thumbnailWidth.dp).height((thumbnailWidth * 1.4f).dp),
                        contentScale = ContentScale.Fit,
                    )
                    failed.value -> Text("Preview unavailable", style = MaterialTheme.typography.labelSmall)
                    else -> CircularProgressIndicator()
                }
                Text(
                    text = "Page ${pageIndex + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.currentPage.value == pageIndex) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}
