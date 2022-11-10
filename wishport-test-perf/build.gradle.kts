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

    linuxX64() {
        binaries {
            executable() {
            }
        }
    }
}