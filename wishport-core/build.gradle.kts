plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("maven-publish")
}

repositories {

}

kotlin {
    sourceSets.getByName("commonMain").apply {
        dependencies {
            api(project(":wishport-helpers"))
        }
    }

    sourceSets.getByName("linuxMain").apply {
        dependencies {
            implementation(project(":wishport-static-uring"))
        }
    }
}