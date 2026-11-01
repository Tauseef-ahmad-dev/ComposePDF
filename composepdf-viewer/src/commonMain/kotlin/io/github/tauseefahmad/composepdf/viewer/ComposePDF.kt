package io.github.tauseefahmad.composepdf.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.github.tauseefahmad.composepdf.core.PdfEngineFactory
import io.github.tauseefahmad.composepdf.core.PdfError
import io.github.tauseefahmad.composepdf.core.PdfSource
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Displays a PDF document using a caller-provided rendering engine.
 *
 * This initial viewer iteration supports vertically scrolling pages. Zoom, search,
 * thumbnails, annotations, and bundled platform engines are separate roadmap items.
 */
@Composable
fun ComposePDF(
    source: PdfSource,
    engineFactory: PdfEngineFactory,
    modifier: Modifier = Modifier,
    state: PdfViewerState = rememberPdfViewerState(engineFactory),
    config: PdfViewerConfig = PdfViewerConfig(),
    onEvent: (PdfViewerEvent) -> Unit = {},
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state) {
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                state.close()
                onEvent(PdfViewerEvent.DocumentClosed)
            }
        }
    }

    LaunchedEffect(source, state) {
        state.open(source)
        if (state.loadingState.value == PdfLoadingState.Loaded) {
            onEvent(PdfViewerEvent.DocumentLoaded)
        } else {
            state.error.value?.let { onEvent(PdfViewerEvent.Error(it)) }
        }
    }

    LaunchedEffect(listState, state) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { it.coerceAtLeast(0) }
            .distinctUntilChanged()
            .collect { index ->
                if (state.loadingState.value == PdfLoadingState.Loaded) {
                    state.updateCurrentPage(index)
                    onEvent(PdfViewerEvent.PageChanged(index + 1))
                }
            }
    }

    LaunchedEffect(state.currentPage.value, state.loadingState.value) {
        if (state.loadingState.value == PdfLoadingState.Loaded &&
            listState.firstVisibleItemIndex != state.currentPage.value
        ) {
            listState.animateScrollToItem(state.currentPage.value)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(config.viewerBackground),
        contentAlignment = Alignment.Center,
    ) {
        when (state.loadingState.value) {
            PdfLoadingState.Idle,
            PdfLoadingState.Loading -> CircularProgressIndicator()

            PdfLoadingState.Failed -> Text(
                text = state.error.value?.toDisplayMessage() ?: "Unable to open PDF.",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
            )

            PdfLoadingState.Loaded -> {
                val pageCount = state.documentInfo.value?.pageCount ?: 0
                if (pageCount == 0) {
                    Text("This document contains no pages.", color = Color.White)
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = config.pageHorizontalPadding,
                            vertical = config.pageSpacing,
                        ),
                        verticalArrangement = Arrangement.spacedBy(config.pageSpacing),
                    ) {
                        items(count = pageCount, key = { it }) { pageIndex ->
                            PdfPage(
                                pageIndex = pageIndex,
                                state = state,
                                config = config,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPage(
    pageIndex: Int,
    state: PdfViewerState,
    config: PdfViewerConfig,
) {
    val renderFailed = remember(pageIndex, state) { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val targetWidth = constraints.maxWidth.coerceAtLeast(1)
        val pageImage = state.pageImage(pageIndex)

        LaunchedEffect(pageIndex, targetWidth, state) {
            if (pageImage == null) {
                renderFailed.value = false
                renderFailed.value = runCatching {
                    state.renderPage(pageIndex, targetWidth)
                }.isFailure
            }
        }

        when {
            pageImage != null -> Image(
                bitmap = pageImage,
                contentDescription = "PDF page ${pageIndex + 1}",
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (config.showPageShadow) Modifier.shadow(2.dp) else Modifier)
                    .background(config.pageBackground),
                contentScale = ContentScale.FillWidth,
            )

            renderFailed.value -> Text(
                text = "Unable to render page ${pageIndex + 1}.",
                color = Color.White,
                modifier = Modifier.padding(24.dp),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .background(config.pageBackground)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

private fun PdfError.toDisplayMessage(): String = when (this) {
    PdfError.InvalidDocument -> "The PDF document is invalid."
    PdfError.PasswordRequired -> "This PDF requires a password."
    PdfError.IncorrectPassword -> "The PDF password is incorrect."
    PdfError.UnsupportedFeature -> "This PDF feature is not supported."
    PdfError.NetworkFailure -> "Unable to download the PDF."
    PdfError.PermissionDenied -> "Permission to read the PDF was denied."
    is PdfError.RenderingFailed -> "Unable to render this PDF page."
    is PdfError.Unknown -> message ?: "An unexpected PDF error occurred."
}
