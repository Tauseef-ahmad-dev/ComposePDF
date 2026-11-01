# Platform support

## Build targets

- Android
- iOS Arm64
- iOS Simulator Arm64
- Desktop JVM (macOS, Windows, Linux)

A configured target is not a guarantee that every backend or feature is available on every architecture.

## Rendering backends

| Platform | Backend | Status |
|---|---|---|
| Android | Android PdfRenderer | Initial implementation |
| iOS | Apple PDFKit | Initial adapter added; native build validation pending |
| Desktop JVM | Apache PDFBox | Initial adapter added; JVM build validation pending |
| Android | PDFiumAndroidKt | Optional adapter in `composepdf-pdfium-android`; native build validation pending |\n| iOS / Desktop | PDFium | Not bundled; compatible Kotlin/Native/JVM distributions remain future work |

## Capability notes

The viewer uses `PdfEngineCapabilities` to expose backend differences. Android PdfRenderer currently renders local files, bytes, and content URIs. PDFBox supports local files, bytes, URL downloads, password-protected documents, and text extraction. PDFKit supports local files, file URLs, bytes, password unlocking, page rendering, and basic text extraction. Do not assume feature parity when changing engines.

## Engine selection

Use `PdfEngineRegistry` to select a native, PDFium, or custom engine. Register only factories whose platform artifacts are included in the consuming application. AUTO prefers the native engine, then PDFium, then custom.

## PDFium status

The Android PDFium adapter is isolated in the optional `composepdf-pdfium-android` module and uses the Apache-2.0 io.legere:pdfiumandroid binding. iOS and Desktop PDFium are not bundled yet because their native binary packaging and Kotlin/Native/JVM integration require separate adapters.
