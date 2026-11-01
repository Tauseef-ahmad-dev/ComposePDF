# Local Maven publishing and integration

ComposePDF currently publishes snapshots to the local Maven repository for consumer-app validation. This is not a public Maven Central release.

## 1. Publish Android artifacts

From the ComposePDF repository root, using a JDK 17-compatible environment:

```bash
./gradlew publishAndroidToMavenLocal
```

This publishes Android release artifacts for core, viewer, and optional PDFium. It intentionally skips the Kotlin Multiplatform metadata publication because generating that metadata compiles all configured platform source sets, including iOS. The Android publication uses the Android variant artifact IDs with an `-android` suffix.

The aggregate `publishToMavenLocal` task publishes every configured target and may fail if an unrelated platform backend does not compile. Use it only when validating all configured targets.

Artifacts are installed in the local Maven repository (normally `~/.m2/repository`). The initial version is `0.1.0-SNAPSHOT`. Republish after changing library code and before rebuilding the consuming app.

## 2. Add Maven Local to your Android app

In the consuming app's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}
```

Keep `mavenLocal()` for local integration only. Remove it when switching to a remotely published release.

## 3. Add the viewer dependency

In the app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.tauseefahmad:composepdf-viewer-android:0.1.0-SNAPSHOT")
}
```

The viewer exposes core types through its API dependency, so a separate core dependency is not normally needed.

For optional PDFium:

```kotlin
dependencies {
    implementation("io.github.tauseefahmad:composepdf-pdfium-android:0.1.0-SNAPSHOT")
}
```

## 4. Render a PDF from a content URI

```kotlin
@Composable
fun PdfDocumentScreen(pdfUri: Uri) {
    val context = LocalContext.current
    val engineFactory = remember(context) { androidPdfEngineFactory(context) }
    val state = rememberPdfViewerState(engineFactory)

    ComposePDF(
        source = PdfSource.Uri(pdfUri.toString()),
        engineFactory = engineFactory,
        state = state,
        modifier = Modifier.fillMaxSize(),
    )
}
```

Make sure the app has permission to read the URI. For documents selected with the Storage Access Framework, retain URI permission when the document must remain accessible after process restarts.

## 5. Validate the consumer

1. Sync Gradle in the consuming app.
2. Build and run on an Android device or emulator.
3. Open a real PDF using a content URI.
4. Verify page rendering, scrolling, zoom, page navigation, and thumbnail selection.
5. Try text search only with an engine whose `textExtraction` capability is true.
6. Report dependency resolution, runtime, or rendering issues with the Android version and PDF characteristics.

## Coordinates

| Module | Maven coordinate |
|---|---|
| Core (Android) | `io.github.tauseefahmad:composepdf-core-android:0.1.0-SNAPSHOT` |
| Viewer (Android) | `io.github.tauseefahmad:composepdf-viewer-android:0.1.0-SNAPSHOT` |
| Android PDFium | `io.github.tauseefahmad:composepdf-pdfium-android:0.1.0-SNAPSHOT` |

## Public release

Maven Central is a separate release step. It requires a verified namespace, Sonatype Central Portal credentials, signing configuration, and a non-snapshot release version. Keep credentials outside source control. Do not publish this preview publicly until consumer integration and supported target builds have been validated.
