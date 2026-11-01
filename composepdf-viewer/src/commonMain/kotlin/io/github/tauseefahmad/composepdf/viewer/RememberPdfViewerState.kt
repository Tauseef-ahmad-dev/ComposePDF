package io.github.tauseefahmad.composepdf.viewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.tauseefahmad.composepdf.core.PdfEngineFactory

/** Remembers a viewer state and its rendering-engine factory for the current composition. */
@Composable
fun rememberPdfViewerState(engineFactory: PdfEngineFactory): PdfViewerState =
    remember(engineFactory) { PdfViewerState(engineFactory) }
