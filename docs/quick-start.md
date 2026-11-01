# Quick start

The viewer API is being developed. The initial Android rendering adapter is available in composepdf-core.

## Android

Create an engine factory from an application context:

    val engineFactory = androidPdfEngineFactory(context)

Then place the viewer in Compose:

    val state = rememberPdfViewerState(engineFactory)

    ComposePDF(
        source = PdfSource.File("/path/to/document.pdf"),
        engineFactory = engineFactory,
        state = state,
        modifier = Modifier.fillMaxSize(),
    )

For an in-memory document, use PdfSource.Bytes(bytes). For a document provider URI, use PdfSource.Uri(uri.toString()).

## Other platforms

The common viewer API and platform targets are configured, but built-in iOS and Desktop rendering engines are not yet available. Do not use the viewer on those targets until a compatible engine adapter is provided.
