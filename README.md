# ComposePDF

ComposePDF is an open-source Compose Multiplatform PDF rendering and viewing library for Android, iOS, and Desktop JVM.

> **Current release line: 0.1.0-SNAPSHOT (preview).** The project is not yet a stable 1.0 release. Platform compilation has been validated, but runtime compatibility and performance must be verified in consuming applications.

## Modules

| Module | Purpose |
|---|---|
| `composepdf-core` | PDF sources, engine contracts, capabilities, metadata, and platform rendering backends |
| `composepdf-viewer` | Compose viewer, zoom, page navigation, search controls, thumbnails, and bookmark navigation |
| `composepdf-pdfium-android` | Optional Android PDFium backend; applications opt in to its native binaries |

## Platform support

| Platform | Default backend | Current capabilities and constraints |
|---|---|---|
| Android | Android PdfRenderer | File, content URI, bytes, and HTTP(S) URL sources; page rendering; no text extraction, outlines, or password support |
| iOS | Apple PDFKit | File, URL, and bytes; page rendering, text extraction, and password unlocking |
| Desktop JVM | Apache PDFBox | File, URL, and bytes; page rendering, text extraction, outlines, and password-protected PDFs |
| Android (optional) | PDFiumAndroid | Optional native backend with text extraction; outline extraction is not currently exposed |

Feature parity is not guaranteed. Inspect `PdfEngineCapabilities` before exposing optional controls. Web is not currently supported.

## Features

- Compose Multiplatform viewer for Android, iOS, and Desktop JVM
- Sources: local file, platform URI, in-memory bytes, and HTTP(S) URL
- Custom HTTP request headers for authenticated URL downloads
- Pinch zoom, pan, double-tap zoom, and fit modes
- Previous/next and direct page navigation
- Document-wide text search on engines that support text extraction
- Lazy page thumbnails and document outline navigation where supported
- Configurable bounded in-memory rendered-page cache (`PdfViewerConfig.maxCachedPages`)

### Current limitations

- Remote PDFs are currently downloaded fully into memory before being passed to the renderer. Disk-backed streaming and persistent download caching are planned; large documents can therefore require substantial memory.
- The viewer's rendered-page cache is bounded, but cache sizing is currently internal.
- Text selection, rendered search highlights, annotation editing/persistence, forms, and Web support are not implemented.
- PDFium uses its own Android URL-loading implementation; other built-in engines use the shared Ktor loader.
- A successful compile does not replace runtime testing with real PDFs, authenticated endpoints, redirects, and error responses.

## Installation

### Android local preview

For local development, publish Android artifacts from the repository root:

```bash
./gradlew publishAndroidToMavenLocal
```

Add `mavenLocal()` to the consuming app's dependency repositories, then use the Android variant coordinates:

```kotlin
dependencies {
    implementation("io.github.tauseefahmad:composepdf-viewer-android:0.1.0-SNAPSHOT")
}
```

The viewer exposes core types transitively. Add the optional PDFium artifact only when you intend to use its backend:

```kotlin
implementation("io.github.tauseefahmad:composepdf-pdfium-android-android:0.1.0-SNAPSHOT")
```

See [local publishing and consumer integration](docs/publishing.md) for the exact setup. These are Maven Local preview artifacts, not a public Maven Central release.

## Android quick start

Add internet permission when loading remote PDFs:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Create an engine factory and viewer state:

```kotlin
@Composable
fun PdfViewerScreen(url: String) {
    val context = LocalContext.current
    val engineFactory = remember(context) { androidPdfEngineFactory(context) }
    val state = rememberPdfViewerState(engineFactory)

    ComposePDF(
        source = PdfSource.Url(
            url = url,
            headers = mapOf("Authorization" to "Bearer <token>"),
        ),
        engineFactory = engineFactory,
        state = state,
        modifier = Modifier.fillMaxSize(),
    )
}
```

For local documents, use `PdfSource.Uri(uri.toString())`, `PdfSource.File(path)`, or `PdfSource.Bytes(bytes)`. The application is responsible for obtaining and retaining access to platform URIs.

## Architecture

The viewer depends on the `PdfRenderEngine` abstraction rather than a concrete backend. Applications can provide custom engines through `PdfEngineFactory`. Inspect engine capabilities before showing optional features. Each factory should create a fresh engine instance for each document session.

## Compatibility

The preview toolchain currently uses Kotlin 2.2.20, Compose Multiplatform 1.9.3, Android Gradle Plugin 8.13.2, Gradle 8.14.3, and JDK 17. Android modules compile with compileSdk 36 and minSdk 24.

This is a build configuration, not a promise that every consumer toolchain version is compatible. The library does not guarantee compatibility with all Kotlin, Gradle, AGP, Compose, or Android versions. Validate the published artifact with your application's toolchain and target devices.

## Project scope

This repository contains library modules and documentation only. It intentionally does not include a sample application, CI workflows, or a test suite. Milestone 2 advanced editing features (text selection, annotations, forms, and related editing tools) are outside the current planned release scope.

## Publishing and release status

The project currently supports Maven Local publishing for Android preview artifacts. Public Maven Central publishing is not complete yet: namespace verification, Central Portal deployment configuration, signing credentials, release validation, and a non-snapshot release remain required.

See [publishing documentation](docs/publishing.md).

## License

Apache License 2.0. See [LICENSE](LICENSE).
