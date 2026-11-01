# Quick start

ComposePDF is currently distributed as a local Maven snapshot for integration testing. See [publishing](publishing.md) to publish it and consume it from another app.

## Android dependency

```kotlin
dependencies {
    implementation("io.github.tauseefahmad:composepdf-viewer:0.1.0-SNAPSHOT")
}
```

Add `mavenLocal()` to the consuming app's dependency repositories while using a locally published snapshot.

## Android viewer

Create an engine factory from an Android context:

```kotlin
val engineFactory = androidPdfEngineFactory(context)
val state = rememberPdfViewerState(engineFactory)

ComposePDF(
    source = PdfSource.Uri(pdfUri.toString()),
    engineFactory = engineFactory,
    state = state,
    modifier = Modifier.fillMaxSize(),
)
```

The application must have permission to read the supplied URI. In-memory documents can use `PdfSource.Bytes(bytes)`, and local paths can use `PdfSource.File(path)`.

## Other platforms

The common viewer API targets Android, iOS, and Desktop JVM. Android PdfRenderer, Apple PDFKit, and Apache PDFBox are the built-in rendering backends. Validate iOS and Desktop native builds in the consuming environment before production use. Backend capabilities differ; consult [platform support](platform-support.md).
