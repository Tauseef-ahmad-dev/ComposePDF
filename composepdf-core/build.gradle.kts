plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }

    iosArm64()
    iosSimulatorArm64()
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "io.github.tauseefahmad.composepdf.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }
}
