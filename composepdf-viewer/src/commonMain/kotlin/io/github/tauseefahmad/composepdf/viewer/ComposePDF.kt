package io.github.tauseefahmad.composepdf.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Constraints
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
 * Gestures and zoom state use common Compose APIs, avoiding platform-specific UI.
 * Pass [zoomState] to expose controls elsewhere in the host application.
 */
@Composable
fun ComposePDF(
    source: PdfSource,
    engineFactory: PdfEngineFactory,
    modifier: Modifier = Modifier,
    state: PdfViewerState = rememberPdfViewerState(engineFactory),
    config: PdfViewerConfig = PdfViewerConfig(),
    zoomState: PdfZoomState = rememberPdfZoomState(),
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
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .pointerInput(zoomState) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    zoomState.transform(zoom, pan)
                                }
                            }
                            .pointerInput(zoomState) {
                                detectTapGestures(onDoubleTap = { zoomState.toggleDoubleTapZoom() })
                            },
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
                                zoomState = zoomState,
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
    zoomState: PdfZoomState,
) {
    val renderFailed = remember(pageIndex, state) { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val viewportWidth = constraints.maxWidth.coerceAtLeast(1)
        val viewportHeight = constraints.maxHeight.takeUnless { it == Constraints.Infinity }
        val pageImage = state.pageImage(pageIndex)
        val pageMetadata = remember(pageIndex, state) { mutableStateOf<io.github.tauseefahmad.composepdf.core.PdfPageMetadata?>(null) }

        LaunchedEffect(pageIndex, state) {
            pageMetadata.value = runCatching { state.pageMetadata(pageIndex) }.getOrNull()
        }

        val fitScale = when (zoomState.fitMode.value) {
            PdfFitMode.FitWidth -> 1f
            PdfFitMode.FitPage -> {
                val metadata = pageMetadata.value
                if (metadata == null || viewportHeight == null) 1f else {
                    val pageAspectRatio = metadata.width / metadata.height
                    minOf(1f, viewportWidth.toFloat() / (viewportHeight * pageAspectRatio))
                }
            }
            PdfFitMode.ActualSize -> {
                val metadata = pageMetadata.value
                if (metadata == null) 1f else 72f / density
            }
        }
        val userScale = zoomState.scale.value
        val scale = fitScale * userScale
        val targetWidth = (viewportWidth * scale).toInt().coerceAtLeast(1)

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
                    .background(config.pageBackground)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = zoomState.pan.value.x
                        translationY = zoomState.pan.value.y
                    },
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
