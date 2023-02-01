plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("maven-publish")
}

val LIB_NAME = "openssl"

kotlin {
    val amd64 = linuxX64()
    val aarch64 = linuxArm64()

    listOf(amd64, aarch64).forEach {
        val main = it.compilations.getByName("main")
        val sourceSet = main.defaultSourceSet.name
        val libPath = "src/$sourceSet"
        val staticLib = project.file(libPath)
        val path = staticLib.absolutePath

        val interop by main.cinterops.creating {
            defFile("src/cinterop/$LIB_NAME.def")
            includeDirs("src/include")
            packageName = "external.openssl"

            extraOpts("-libraryPath", path)
        }
    }
}
