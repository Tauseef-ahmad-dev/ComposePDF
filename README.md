# ComposePDF

A Compose Multiplatform PDF viewer library for Android, iOS, and Desktop.

> **Status: early development.** The initial foundation and Android PdfRenderer adapter are in place. APIs are not yet stable. Built-in iOS and Desktop rendering engines are still planned.

## Modules

- **composepdf-core** — document sources and metadata, engine contracts, rendering options, text models, capability declarations, and the initial Android PdfRenderer adapter.
- **composepdf-viewer** — Compose viewer API, document state, loading/error states, and lazy vertical page presentation.

## Targets

- Android
- iOS (device and simulator)
- Desktop (JVM)

Web is a future target, subject to rendering-engine feasibility. A configured target does not imply a built-in rendering backend is available.

## Android quick start

Create an engine factory from an Android context:

    val engineFactory = androidPdfEngineFactory(context)

Then render a document:

    val state = rememberPdfViewerState(engineFactory)

    ComposePDF(
        source = PdfSource.File("/path/to/document.pdf"),
        engineFactory = engineFactory,
        state = state,
        modifier = Modifier.fillMaxSize(),
    )

The initial Android adapter supports filesystem paths, byte arrays, and content URIs. URL loading and password-protected documents are not supported yet.

## Architecture

The viewer depends on the PdfRenderEngine abstraction rather than a concrete PDF backend. Engines declare their supported capabilities, and platform-specific adapters own document access, rendering, threading, and native-resource cleanup.

## Current feature status

| Feature | Status |
|---|---|
| Common document and engine contracts | Implemented |
| Compose vertical page viewer | Initial implementation |
| Android PdfRenderer backend | Initial implementation |
| iOS rendering backend | Planned |
| Desktop rendering backend | Planned |
| URL loading | Planned |
| Zoom and fit modes | Planned |
| Search and text selection | Planned |
| Thumbnails and bookmarks | Planned |
| Annotation overlays and persistence | Planned |
| Forms and document editing | Planned |

See [platform support](docs/platform-support.md), [engine integration](docs/engine-integration.md), and [quick start](docs/quick-start.md).

## Build

Open the project in Android Studio or IntelliJ IDEA with a compatible JDK and Gradle installation. The project targets Android, iOS, and Desktop JVM.

## Project constraints

This repository contains the library only. It intentionally has no CI workflows, automated test infrastructure, or sample application.

## License

Apache License 2.0. See [LICENSE](LICENSE).
