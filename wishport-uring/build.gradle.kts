plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("maven-publish")
}

val LIB_NAME = "liburing"

kotlin {
    val amd64 = linuxX64()
    val aarch64 = linuxArm64()

    listOf(amd64, aarch64).forEach {
        val main by it.compilations.getting {
            val sourceSet = defaultSourceSetName
            val libPath = "src/$sourceSet/$LIB_NAME.a"
            val staticLib = project.file(libPath)
            val path = staticLib.absolutePath

            kotlinOptions {
                freeCompilerArgs = listOf("-include-binary", path)
            }
        }


        val interop by main.cinterops.creating {
            defFile("src/cinterop/$LIB_NAME.def")
            includeDirs("src/include")
            packageName = "external.liburing"
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                api(project(":wishport-helpers"))
                api(project(":wishport-errors"))
            }
        }
    }
}