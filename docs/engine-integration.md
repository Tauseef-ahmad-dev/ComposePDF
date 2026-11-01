# Rendering engine integration

ComposePDF separates the viewer from the PDF backend through the `PdfRenderEngine` interface in `composepdf-core`.

## Implement an engine

A backend implements `open(source, password)`, `getPageMetadata(pageIndex)`, `renderPage(pageIndex, options)`, optional `extractText(pageIndex)`, and `close()`. It must expose truthful `PdfEngineCapabilities`.

## Select an engine

Use `PdfEngineRegistry` to register native, PDFium, and custom factories. `AUTO` selects the first registered factory in native → PDFium → custom order. It does not silently retry another engine if opening a document fails.

Example:

```kotlin
val registry = PdfEngineRegistry(
    nativeFactory = desktopPdfBoxEngineFactory(),
    pdfiumFactory = optionalPdfiumFactory,
)
val engine = registry.create(PdfEngineType.AUTO)
```

Use `androidPdfEngineFactory(context)` on Android and `iosPdfKitEngineFactory()` on iOS.

## Threading and lifecycle

Engine implementations own their threading rules. They must serialize access when their native library is not thread-safe, avoid blocking the UI thread, and make cleanup safe after failed or cancelled opens. Each factory must return a fresh engine instance.

## Current backend status

| Backend | Status |
|---|---|
| Android PdfRenderer | Initial adapter |
| iOS PDFKit | Initial adapter; native build validation pending |
| Desktop Apache PDFBox | Initial adapter; JVM build validation pending |
| Android PDFium | Optional `composepdf-pdfium-android` module; validation pending |\n| iOS / Desktop PDFium | Not bundled |

The Android PDFium adapter is an opt-in module. Add `implementation(projects.composepdfPdfiumAndroid)` and register `androidPdfiumEngineFactory(context)` as the PDFium factory. iOS and Desktop PDFium bindings remain future work; the platform-specific native distributions cannot yet be shared safely through one dependency.
