plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    jvmToolchain(17)

    androidTarget {
        publishLibraryVariants("release")
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.composepdfCore)
        }
        androidMain.dependencies {
            implementation("io.legere:pdfiumandroid:2.0.3")
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "io.github.tauseefahmad.composepdf.pdfium.android"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }
}
