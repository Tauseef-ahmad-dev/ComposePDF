package io.github.tauseefahmad.composepdf.core

/** Stable, platform-neutral error categories exposed by ComposePDF. */
sealed interface PdfError {
    data object InvalidDocument : PdfError
    data object PasswordRequired : PdfError
    data object IncorrectPassword : PdfError
    data object UnsupportedFeature : PdfError
    data object NetworkFailure : PdfError
    data object PermissionDenied : PdfError
    data class RenderingFailed(val cause: String? = null) : PdfError
    data class Unknown(val message: String? = null) : PdfError
}
