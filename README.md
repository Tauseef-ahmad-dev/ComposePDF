# ComposePDF

ComposePDF is an open-source Compose Multiplatform PDF rendering and viewing library for **Android, iOS, and Desktop JVM**. It provides a shared Compose viewer API with platform-specific rendering engines.

> **Current release: `0.1.0-alpha.1`.** This is an alpha release, not a stable `1.0.0` release. Maven Central consumers should use it only after the deployment is marked **Published** in the Central Portal. Runtime compatibility and performance must still be validated in consuming applications.

## Platform support

| Platform | Default rendering backend | Supported inputs and capabilities |
|---|---|---|
| Android | Android `PdfRenderer` | File, content URI, bytes, HTTP(S) URL; page rendering |
| iOS | Apple PDFKit | File, URL, bytes; page rendering, text extraction, password unlocking |
| Desktop JVM | Apache PDFBox | File, URL, bytes; page rendering, text extraction, outlines, password-protected PDFs |
| Android (optional) | PDFiumAndroid | Optional native backend with text extraction |

Feature parity is not guaranteed. Inspect `PdfEngineCapabilities` before exposing optional controls. Web is not currently supported.

## Modules

| Module | Purpose |
|---|---|
| `composepdf-core` | PDF sources, engine contracts, capabilities, metadata, and platform rendering backends |
| `composepdf-viewer` | Compose viewer, zoom, page navigation, search controls, thumbnails, and bookmark navigation |
| `composepdf-pdfium-android` | Optional Android PDFium backend; applications opt in to its native binaries |

## Features

- Compose Multiplatform viewer for Android, iOS, and Desktop JVM
- Local file, platform URI, in-memory bytes, and HTTP(S) URL sources
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

The examples below use `0.1.0-alpha.1`. Replace it with a later version when one is published. Make sure the alpha deployment is marked **Published** in Maven Central before resolving it.

### 1. Android-only project (Jetpack Compose)

Add Maven Central to your dependency repositories. For example, in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

In the Android application module's `build.gradle.kts`, add the Android publication:

```kotlin
dependencies {
    implementation("io.github.tauseef-ahmad-dev:composepdf-viewer-android:0.1.0-alpha.1")
}
```

The viewer exposes core types transitively. Add the optional PDFium artifact only if you intend to use that backend:

```kotlin
dependencies {
    implementation("io.github.tauseef-ahmad-dev:composepdf-pdfium-android-android:0.1.0-alpha.1")
}
```

For remote PDF URLs, add this permission to the consuming app's `AndroidManifest.xml`, outside the `<application>` element:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Example viewer:

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

### 2. Kotlin Multiplatform project (Android + iOS + Desktop)

For a Kotlin Multiplatform consumer, depend on the **KMP root module** rather than the Android-only publication. Add this dependency to `commonMain` in the consuming project's `build.gradle.kts`:

```kotlin
kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation("io.github.tauseef-ahmad-dev:composepdf-viewer:0.1.0-alpha.1")
        }
    }
}
```

Keep `google()` and `mavenCentral()` in the consumer's dependency repositories. The root KMP coordinate supplies Gradle module metadata so Gradle can select the matching platform variant for the consumer target. Do not add the `-android` artifact to `commonMain`; that artifact is for Android-only consumers.

Use the shared Compose viewer from common Kotlin code and supply the appropriate platform engine factory from the platform integration. Platform-specific file and URI access should remain in the relevant source set. The built-in engines have different capabilities, so check `PdfEngineCapabilities` before displaying optional functionality.

The Android-only PDFium engine is an opt-in Android dependency. Add it to `androidMain` (not `commonMain`) if the KMP application uses PDFium on Android:

```kotlin
sourceSets {
    androidMain.dependencies {
        implementation("io.github.tauseef-ahmad-dev:composepdf-pdfium-android-android:0.1.0-alpha.1")
    }
}
```

### 3. iOS-only application

There are two different iOS integration scenarios:

**Kotlin Multiplatform iOS application:** If the iOS app is part of a Kotlin Multiplatform project, use the KMP dependency shown above in `commonMain`, configure the iOS targets, and use the shared Compose UI in the iOS application. The iOS rendering backend is PDFKit. This is the supported Kotlin-side route for using the ComposePDF viewer on iOS.

**Native Swift / Xcode-only application:** Add this repository in Xcode using **File → Add Package Dependencies** and the repository URL:

```text
https://github.com/Tauseef-ahmad-dev/ComposePDF
```

Select the `ComposePDFViewer` product. The Swift package currently distributes the iOS alpha XCFramework from the GitHub release `ios-0.1.0-alpha.1`.

A SwiftUI wrapper can host the UIKit entry point:

```swift
import SwiftUI
import ComposePDFViewer

struct ComposePDFScreen: UIViewControllerRepresentable {
    let pdfURL: String

    func makeUIViewController(context: Context) -> UIViewController {
        ComposePDFViewControllerKt.composePdfViewController(url: pdfURL)
    }

    func updateUIViewController(
        _ uiViewController: UIViewController,
        context: Context
    ) {
        // Recreate this representable when pdfURL changes.
    }
}
```

Use it in a SwiftUI view:

```swift
struct ContentView: View {
    var body: some View {
        ComposePDFScreen(pdfURL: "https://example.com/sample.pdf")
            .ignoresSafeArea()
    }
}
```

This alpha entry point accepts publicly accessible HTTP(S) PDF URLs only; custom request headers and a native document picker are not exposed by this convenience API. The binary package has not yet been validated in a clean external Xcode consumer project.

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

## Architecture

The viewer depends on the `PdfRenderEngine` abstraction rather than a concrete backend. Applications can provide custom engines through `PdfEngineFactory`. Inspect engine capabilities before showing optional features. Each factory should create a fresh engine instance for each document session.

## Compatibility

The project build currently uses Kotlin 2.2.20, Compose Multiplatform 1.9.3, Android Gradle Plugin 8.13.2, Gradle 8.14.3, and JDK 17. Android modules compile with compileSdk 36 and minSdk 24.

These are the project's build versions, not a promise of compatibility with every consumer toolchain. Validate the published artifact with your application's Kotlin, Gradle, AGP, Compose versions, and target devices.

## Project scope

This repository contains library modules and documentation only. It intentionally does not include a sample application, CI workflows, or a test suite. Text selection, annotations, forms, and related editing tools are not currently implemented.

## Publishing and release status

- Maven Central namespace: `io.github.tauseef-ahmad-dev`
- Current alpha version: `0.1.0-alpha.1`
- Android-only consumers: use `composepdf-viewer-android`
- Kotlin Multiplatform consumers: use `composepdf-viewer`
- Native Swift / Xcode SPM distribution: not yet available; planned separately

See [publishing and consumer integration](docs/publishing.md) for Maven Local development, publication configuration, and consumer validation guidance.

## License

Apache License 2.0. See [LICENSE](LICENSE).
