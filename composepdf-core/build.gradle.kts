plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
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
            baseName = "ComposePDFCore"
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            api(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
        }

        jvmMain.dependencies {
            implementation("org.apache.pdfbox:pdfbox:3.0.5")
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
