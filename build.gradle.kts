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
