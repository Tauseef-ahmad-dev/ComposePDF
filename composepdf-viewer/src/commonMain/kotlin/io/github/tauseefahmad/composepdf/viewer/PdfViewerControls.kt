package io.github.tauseefahmad.composepdf.viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Compact, platform-independent controls for changing PDF zoom and fit mode. */
@Composable
fun PdfZoomControls(
    zoomState: PdfZoomState,
    modifier: Modifier = Modifier,
    showFitModes: Boolean = true,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = zoomState::zoomOut, enabled = zoomState.scale.value > zoomState.minScale) {
            Text("−")
        }
        Text(
            text = "${(zoomState.scale.value * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
        )
        TextButton(onClick = zoomState::zoomIn, enabled = zoomState.scale.value < zoomState.maxScale) {
            Text("+")
        }
        if (showFitModes) {
            TextButton(onClick = { zoomState.setFitMode(PdfFitMode.FitWidth) }) {
                Text("Fit width")
            }
            TextButton(onClick = { zoomState.setFitMode(PdfFitMode.FitPage) }) {
                Text("Fit page")
            }
            TextButton(onClick = { zoomState.setFitMode(PdfFitMode.ActualSize) }) {
                Text("Actual")
            }
        }
    }
}
