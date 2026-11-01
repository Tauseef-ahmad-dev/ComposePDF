plugins {
    alias(libs.plugins.maven.publish)
    signing
    `maven-publish`
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
            api(project(":composepdf-core"))
        }
        androidMain.dependencies {
            implementation("io.legere:pdfiumandroid:1.0.35")
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

publishing {
    publications.withType<MavenPublication>().matching { it.name == "androidRelease" }.configureEach {
        artifactId = project.name
    }

    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("ComposePDF")
            description.set("Compose Multiplatform PDF rendering and viewer library")
            url.set("https://github.com/Tauseef-ahmad-dev/ComposePDF")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("tauseef-ahmad-dev")
                    name.set("Tauseef Ahmad")
                    url.set("https://github.com/Tauseef-ahmad-dev")
                }
            }
            scm {
                url.set("https://github.com/Tauseef-ahmad-dev/ComposePDF")
                connection.set("scm:git:git://github.com/Tauseef-ahmad-dev/ComposePDF.git")
                developerConnection.set("scm:git:ssh://git@github.com/Tauseef-ahmad-dev/ComposePDF.git")
            }
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


mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(project.group.toString(), project.name, project.version.toString())
    pom {
        name.set("ComposePDF")
        description.set("Compose Multiplatform PDF rendering and viewer library")
        inceptionYear.set("2026")
        url.set("https://github.com/Tauseef-ahmad-dev/ComposePDF")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("tauseef-ahmad-dev")
                name.set("Tauseef Ahmad")
                url.set("https://github.com/Tauseef-ahmad-dev")
            }
        }
        scm {
            url.set("https://github.com/Tauseef-ahmad-dev/ComposePDF")
            connection.set("scm:git:git://github.com/Tauseef-ahmad-dev/ComposePDF.git")
            developerConnection.set("scm:git:ssh://git@github.com/Tauseef-ahmad-dev/ComposePDF.git")
        }
    }
}
