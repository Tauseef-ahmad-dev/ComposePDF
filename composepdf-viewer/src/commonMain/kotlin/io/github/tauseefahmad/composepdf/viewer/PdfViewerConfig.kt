package io.github.tauseefahmad.composepdf.viewer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Visual configuration for the built-in PDF page viewer. */
data class PdfViewerConfig(
    val pageSpacing: Dp = 12.dp,
    val pageHorizontalPadding: Dp = 12.dp,
    val pageBackground: Color = Color.White,
    val viewerBackground: Color = Color(0xFF202124),
    val showPageShadow: Boolean = true,
)
