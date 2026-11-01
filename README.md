# ComposePDF

A Compose Multiplatform PDF rendering and viewer library for Android, iOS, and Desktop JVM.

> **Status: early preview (0.1.0-SNAPSHOT).** The API and platform adapters are still evolving. Validate the targets and features you use in your consuming application before relying on them in production.

## Modules

- **composepdf-core** — document sources, metadata, engine contracts, capabilities, and platform rendering adapters.
- **composepdf-viewer** — Compose PDF viewer, zoom and fit controls, page navigation, text search controls, thumbnails, and bookmark navigation.
- **composepdf-pdfium-android** — optional Android PDFium backend. Applications opt in to its native binaries.

## Targets and rendering backends

| Target | Backend | Notes |
|---|---|---|
| Android | Android PdfRenderer | Local files, bytes, and content URIs; no text extraction or document outlines |
| iOS | Apple PDFKit | Local files, bytes, page rendering, and text extraction; validate with an iOS build |
| Desktop JVM | Apache PDFBox | Files, bytes, URLs, password-protected PDFs, text extraction, and document outlines |
| Android (optional) | PDFiumAndroid | Text extraction and additional PDF features; document outline extraction is not currently exposed |

A configured target does not guarantee feature parity. Check `PdfEngineCapabilities` before exposing optional functionality. Web is not currently supported.

## Publish locally

For Android consumer-app integration, run from the repository root:

```bash
./gradlew publishAndroidToMavenLocal
```

This publishes Android release artifacts only. It does not publish KMP metadata or compile iOS/Desktop targets. Use `./gradlew publishToMavenLocal` only when validating all configured targets.

The initial coordinates are:

- Group: `io.github.tauseefahmad`
- Version: `0.1.0-SNAPSHOT`
- Viewer (Android): `io.github.tauseefahmad:composepdf-viewer-android:0.1.0-SNAPSHOT`
- Core (Android): `io.github.tauseefahmad:composepdf-core-android:0.1.0-SNAPSHOT`
- Optional Android PDFium: `io.github.tauseefahmad:composepdf-pdfium-android:0.1.0-SNAPSHOT`

See [local publishing and consumer setup](docs/publishing.md).

## Android quick start

Add `mavenLocal()` to the consuming application's dependency repositories while testing a locally published snapshot, then add:

```kotlin
dependencies {
    implementation("io.github.tauseefahmad:composepdf-viewer-android:0.1.0-SNAPSHOT")
}
```

Create the engine factory and viewer state:

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

The caller is responsible for obtaining and retaining access to the supplied URI. For in-memory documents use `PdfSource.Bytes(bytes)`; for a local path use `PdfSource.File(path)`.

For optional PDFium support, add:

```kotlin
implementation("io.github.tauseefahmad:composepdf-pdfium-android:0.1.0-SNAPSHOT")
```

Then create `androidPdfiumEngineFactory(context)` and select it through your engine registry or pass it directly as the viewer's engine factory.

## Features

- Multiplatform rendering engine abstraction
- Android PdfRenderer, iOS PDFKit, Desktop PDFBox
- Pinch zoom, pan, double-tap zoom, fit modes
- Previous/next and direct page navigation
- Text search on engines with text extraction
- Lazy page thumbnails and page navigation
- Document outline extraction on Desktop PDFBox and hierarchical bookmark navigation

Text selection, rendered search highlights, annotation persistence, forms, and Web support are not currently implemented.

## Architecture

The viewer depends on `PdfRenderEngine`, not on a concrete PDF backend. Applications can provide custom engines and should inspect engine capabilities before presenting optional controls. Each factory should return a fresh engine instance per document session.

## Project constraints

This repository contains the library only. It intentionally has no CI workflows, automated test infrastructure, or sample application.

## License

Apache License 2.0. See [LICENSE](LICENSE).
