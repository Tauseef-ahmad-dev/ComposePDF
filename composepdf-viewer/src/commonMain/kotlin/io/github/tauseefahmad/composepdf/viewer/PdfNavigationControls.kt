package io.github.tauseefahmad.composepdf.viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Page navigation controls for a [PdfViewerState].
 *
 * The page field uses one-based numbering for readers, while [PdfViewerState]
 * continues to expose zero-based page indexes to Kotlin callers.
 */
@Composable
fun PdfNavigationControls(
    state: PdfViewerState,
    modifier: Modifier = Modifier,
) {
    val documentInfo = state.documentInfo.value
    val pageCount = documentInfo?.pageCount ?: 0
    val currentPage = state.currentPage.value
    val scope = rememberCoroutineScope()
    var pageInput by remember(state) { mutableStateOf((currentPage + 1).toString()) }

    LaunchedEffect(currentPage, pageCount) {
        pageInput = if (pageCount > 0) (currentPage + 1).toString() else ""
    }

    val requestedPage = pageInput.toIntOrNull()
    val canNavigate = pageCount > 0
    val canGoToPage = requestedPage != null && requestedPage in 1..pageCount

    fun navigateToInputPage() {
        val page = requestedPage ?: return
        if (page in 1..pageCount) {
            scope.launch { state.goToPage(page - 1) }
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = { scope.launch { state.previousPage() } },
            enabled = canNavigate && currentPage > 0,
        ) {
            Text("Previous")
        }

        OutlinedTextField(
            value = pageInput,
            onValueChange = { value ->
                pageInput = value.filter(Char::isDigit).take(7)
            },
            modifier = Modifier.width(76.dp),
            enabled = canNavigate,
            singleLine = true,
            placeholder = { Text("Page") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { navigateToInputPage() }),
        )

        Text(
            text = "/ $pageCount",
            maxLines = 1,
        )

        TextButton(
            onClick = { navigateToInputPage() },
            enabled = canNavigate && canGoToPage,
        ) {
            Text("Go")
        }

        TextButton(
            onClick = { scope.launch { state.nextPage() } },
            enabled = canNavigate && currentPage < pageCount - 1,
        ) {
            Text("Next")
        }
    }
}
