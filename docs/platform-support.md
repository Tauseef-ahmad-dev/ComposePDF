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
| Android / iOS / Desktop | PDFium | Not bundled yet; optional integration requires a compatible distribution per target |

## Capability notes

The viewer uses `PdfEngineCapabilities` to expose backend differences. Android PdfRenderer currently renders local files, bytes, and content URIs. PDFBox supports local files, bytes, URL downloads, password-protected documents, and text extraction. PDFKit supports local files, file URLs, bytes, password unlocking, page rendering, and basic text extraction. Do not assume feature parity when changing engines.

## Engine selection

Use `PdfEngineRegistry` to select a native, PDFium, or custom engine. Register only factories whose platform artifacts are included in the consuming application. AUTO prefers the native engine, then PDFium, then custom.

## PDFium status

PDFium is intentionally not added as a mandatory common dependency. The available packages currently have different Android/JVM/Kotlin-Native packaging and Kotlin toolchain requirements. A shared integration needs a pinned compatible version, native binaries for each supported architecture, license review, and an adapter implementing the ComposePDF engine contract.
