plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
}

allprojects {
    group = "io.github.tauseefahmad"
    version = providers.gradleProperty("VERSION_NAME").getOrElse("0.1.0-SNAPSHOT")
}

/**
 * Publishes only the metadata and Android release variants.
 * The aggregate publishToMavenLocal task also builds every configured target.
 */
tasks.register("publishAndroidToMavenLocal") {
    group = "publishing"
    description = "Publishes ComposePDF metadata and Android release artifacts to Maven Local."

    dependsOn(
        ":composepdf-core:publishKotlinMultiplatformPublicationToMavenLocal",
        ":composepdf-core:publishAndroidReleasePublicationToMavenLocal",
        ":composepdf-viewer:publishKotlinMultiplatformPublicationToMavenLocal",
        ":composepdf-viewer:publishAndroidReleasePublicationToMavenLocal",
        ":composepdf-pdfium-android:publishKotlinMultiplatformPublicationToMavenLocal",
        ":composepdf-pdfium-android:publishAndroidReleasePublicationToMavenLocal",
    )
}
