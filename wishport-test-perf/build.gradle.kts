plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("maven-publish")
}

kotlin {
    sourceSets.getByName("commonMain").apply {
        dependencies {
            api(project(":wishport-core"))
        }
    }

    sourceSets.all {
        languageSettings {

        }
    }

    linuxX64() {
        binaries {
            executable() {

            }
        }

        val main by compilations.getting {
            kotlinOptions {

            }
        }
    }
}