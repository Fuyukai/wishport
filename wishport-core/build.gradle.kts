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

    val amd64 = linuxX64()
    val aarch64 = linuxArm64()

    // add extra symbols that aren't correctly exported
    listOf(amd64, aarch64).forEach {
        val main = it.compilations.getByName("main")

        val extras by main.cinterops.creating {
            defFile("src/cinterop/extras.def")
            packageName = "platform.extra"
        }
    }
}