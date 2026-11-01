package io.github.tauseefahmad.composepdf.viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Common Compose controls for searching text across the currently open PDF. */
@Composable
fun PdfSearchControls(
    state: PdfViewerState,
    modifier: Modifier = Modifier,
) {
    var query by remember(state) { mutableStateOf("") }
    var message by remember(state) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val resultCount = state.searchResults.size
    val activeIndex = state.activeSearchResultIndex.value
    val enabled = state.loadingState.value == PdfLoadingState.Loaded && state.supportsTextSearch

    fun runSearch() {
        scope.launch {
            message = null
            try {
                state.search(query)
                if (query.isNotBlank() && state.searchResults.isEmpty()) {
                    message = "No matches found"
                }
            } catch (throwable: Throwable) {
                message = throwable.message ?: "Search failed"
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.widthIn(min = 160.dp, max = 280.dp),
                enabled = enabled,
                singleLine = true,
                label = { Text("Find in document") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { runSearch() }),
            )
            TextButton(onClick = { runSearch() }, enabled = enabled && query.isNotBlank()) {
                Text("Search")
            }
            TextButton(
                onClick = { scope.launch { state.previousSearchResult() } },
                enabled = resultCount > 0,
            ) {
                Text("Previous")
            }
            TextButton(
                onClick = { scope.launch { state.nextSearchResult() } },
                enabled = resultCount > 0,
            ) {
                Text("Next")
            }
            Text(
                text = if (resultCount > 0) "${activeIndex + 1} / $resultCount" else "0 results",
            )
            TextButton(
                onClick = {
                    state.clearSearch()
                    query = ""
                    message = null
                },
                enabled = query.isNotEmpty() || resultCount > 0,
            ) {
                Text("Clear")
            }
        }
        message?.let { Text(it) }
        if (state.loadingState.value == PdfLoadingState.Loaded && !state.supportsTextSearch) {
            Text("Text search is unavailable with the selected PDF engine.")
        }
    }
}
