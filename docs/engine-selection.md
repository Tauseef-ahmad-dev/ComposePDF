# Engine selection

ComposePDF resolves engines through `PdfEngineRegistry`. Register a factory for each backend that the application ships:

```kotlin
// Android: include the optional composepdf-pdfium-android module to use PDFium.
val engines = PdfEngineRegistry(
    nativeFactory = androidPdfEngineFactory(context),
    pdfiumFactory = androidPdfiumEngineFactory(context),
    customFactory = applicationSpecificFactory,
)
val engine = engines.create(PdfEngineType.AUTO)
```

- `AUTO`: native, then PDFium, then custom.
- `NATIVE`: use only the platform-native backend.
- `PDFIUM`: use only the PDFium backend.
- `CUSTOM`: use only the application-provided backend.

Factories must return a new engine for every document session. The registry does not silently switch engines after an open failure: callers should choose a fallback explicitly so password handling, feature support, and document security remain predictable.

## Backend matrix

| Platform | Native backend | PDFium |
|---|---|---|
| Android | Android PdfRenderer | Optional adapter; dependency/API compatibility must be pinned |
| iOS | PDFKit adapter in progress | Requires a compatible Kotlin/Native PDFium distribution |
| Desktop JVM | Apache PDFBox | Optional JNI distribution |
