package io.github.tauseefahmad.composepdf.viewer

sealed interface PdfLoadingState {
    data object Idle : PdfLoadingState
    data object Loading : PdfLoadingState
    data object Loaded : PdfLoadingState
    data object Failed : PdfLoadingState
}
