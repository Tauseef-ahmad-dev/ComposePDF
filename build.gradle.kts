plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
}

allprojects {
    group = "io.github.tauseef-ahmad-dev"
    version = providers.gradleProperty("VERSION_NAME").getOrElse("0.1.0-SNAPSHOT")
}

/**
 * Publishes only Android release variants. Do not publish the KMP metadata
 * publication here: generating it compiles all platform source sets,
 * including iOS, which is outside this Android consumer-validation step.
 */
tasks.register("publishAndroidToMavenLocal") {
    group = "publishing"
    description = "Publishes ComposePDF Android release artifacts to Maven Local."

    dependsOn(
        ":composepdf-core:publishAndroidReleasePublicationToMavenLocal",
        ":composepdf-viewer:publishAndroidReleasePublicationToMavenLocal",
        ":composepdf-pdfium-android:publishAndroidReleasePublicationToMavenLocal",
    )
}
