plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("maven-publish")
}

kotlin {
    val amd64 = linuxX64()
    val aarch64 = linuxArm64()
    // val win64 = mingwX64()

    // manual system call
    listOf(amd64, aarch64).forEach {
        val main = it.compilations.getByName("main")

        val getrandom by main.cinterops.creating {
            defFile("src/cinterop/getrandom.def")
            packageName = "external.getrandom"
        }

        val extras by main.cinterops.creating {
            defFile("src/cinterop/extras.def")
            packageName = "platform.extra"
        }
    }
}