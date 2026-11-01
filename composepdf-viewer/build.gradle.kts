plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)

    androidTarget {
        publishLibraryVariants("release")
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "ComposePDFViewer"
            export(projects.composepdfCore)
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            api(projects.composepdfCore)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "io.github.tauseefahmad.composepdf.viewer"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }
}
