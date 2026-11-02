# Publishing and consumer integration

ComposePDF currently publishes Android preview artifacts to Maven Local. This is not a public Maven Central release.

## 1. Publish Android preview artifacts

Use JDK 17 and run from the repository root:

```bash
./gradlew publishAndroidToMavenLocal
```

The task publishes the Android release variants for core, viewer, and optional PDFium. It intentionally skips KMP metadata publication, because that task can compile all configured platform source sets. The task does not validate iOS or Desktop.

The Android publication artifact IDs include the Android target suffix. Expected local coordinates:

| Module | Maven coordinate |
|---|---|
| Core | `io.github.tauseef-ahmad-dev:composepdf-core-android:0.1.0-SNAPSHOT` |
| Viewer | `io.github.tauseef-ahmad-dev:composepdf-viewer-android:0.1.0-SNAPSHOT` |
| Android PDFium | `io.github.tauseef-ahmad-dev:composepdf-pdfium-android-android:0.1.0-SNAPSHOT` |

Artifacts are installed in `~/.m2/repository`. Republish after changing library code. If Gradle keeps an older artifact cached, stop Gradle and refresh dependencies in the consumer app.

## 2. Configure Maven Local in the consumer

In the consumer's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
    }
}
```

Keep `mavenLocal()` for local integration only. Remove it when consuming a remotely published release.

## 3. Add the viewer

In the Android app module:

```kotlin
dependencies {
    implementation("io.github.tauseef-ahmad-dev:composepdf-viewer-android:0.1.0-SNAPSHOT")
}
```

The viewer exposes core types transitively. For the optional PDFium engine:

```kotlin
dependencies {
    implementation("io.github.tauseef-ahmad-dev:composepdf-pdfium-android-android:0.1.0-SNAPSHOT")
}
```

## 4. Network permission for URL sources

When using `PdfSource.Url`, declare this in the consuming Android app's manifest, outside the `<application>` element:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Example:

```kotlin
val source = PdfSource.Url(
    url = "https://example.com/document.pdf",
    headers = mapOf("Authorization" to "Bearer <token>"),
)
```

The current shared URL loader downloads the response into memory before the platform engine opens it. This can be expensive for large files. Disk-backed streaming, remote-document cache policy, download progress, and cancellation controls are still planned follow-up work.

## 5. Consumer validation checklist

Before relying on a version in an application, verify:

1. Gradle resolves the expected artifact and version.
2. The app compiles with its Kotlin, AGP, Gradle, and Compose versions.
3. A local file and a platform URI open successfully.
4. A public URL opens with network permission enabled.
5. An authenticated URL forwards its request headers.
6. Redirects and non-success HTTP responses produce understandable outcomes.
7. Cancellation and repeated open/close do not leak resources.
8. Rendering, scrolling, zoom, navigation, search, and thumbnails behave as expected for the selected engine.
9. Test representative small, large, scanned, and password-protected PDFs against the engine capability matrix.

## 6. Toolchain and compatibility

The viewer's rendered bitmap cache can be tuned per viewer:

\`\`\`kotlin
val config = PdfViewerConfig(maxCachedPages = 6)
ComposePDF(
    source = source,
    engineFactory = engineFactory,
    state = state,
    config = config,
)
\`\`\`

Lower values reduce retained bitmap memory at the cost of more page re-rendering when users scroll back. The default is 12 pages.

The current project build uses:

- Kotlin 2.2.20
- Compose Multiplatform 1.9.3
- Android Gradle Plugin 8.13.2
- Gradle wrapper 8.14.3
- JDK 17
- Android compileSdk 36 / minSdk 24

These are the project's current build versions, not a guarantee of compatibility with every consumer toolchain. Keep a tested compatibility matrix in release notes as additional consumer configurations are validated.

## 7. Public Maven Central release checklist

Maven Central publication is configured for the verified namespace `io.github.tauseef-ahmad-dev`. The first alpha release is being prepared. Before publishing a stable version:

- Confirm the `io.github.tauseef-ahmad-dev` namespace remains verified in Sonatype Central Portal.
- Configure the Central Portal publishing/deployment plugin and repository credentials using environment variables or Gradle user properties; never commit credentials.
- Configure in-memory or file-backed signing with a valid GPG key and publish signatures for every publication.
- Set a non-snapshot version through `VERSION_NAME`.
- Validate POM metadata, source/Javadoc artifacts as required by the Central Portal, licenses, and SCM links.
- Validate Android, iOS, and Desktop artifacts from clean consumer projects.
- Publish a release candidate to a staging workflow, verify the resolved artifacts, then publish the stable release.
- Create a Git tag and GitHub release with supported toolchain versions, platform limitations, and migration notes.

### Configure credentials locally

Create a Central Portal user token and configure it in `~/.gradle/gradle.properties` (never in this repository):

```properties
mavenCentralUsername=YOUR_CENTRAL_PORTAL_TOKEN_USERNAME
mavenCentralPassword=YOUR_CENTRAL_PORTAL_TOKEN_PASSWORD
signing.keyId=YOUR_GPG_KEY_ID
signing.password=YOUR_GPG_KEY_PASSWORD
signing.secretKeyRingFile=/absolute/path/to/secring.gpg
```

Alternatively, use the plugin's supported environment variables and in-memory signing key properties. See the [plugin's credential documentation](https://vanniktech.github.io/gradle-maven-publish-plugin/central/#secrets).

After namespace verification, credentials, and a distributed public GPG key are ready, set a release version (for example with `VERSION_NAME=0.1.0`) and run `./gradlew publishToMavenCentral`. The plugin uploads a deployment for validation; manually publish it in the Central Portal unless automatic release is explicitly configured. Do not run this task until the Sonatype namespace and credentials are confirmed.

Do not treat `publishAndroidToMavenLocal` as a Maven Central publishing task. Do not publish a public stable release until all platform artifacts and clean consumer projects have been validated.
