import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.multiplatform").version("1.8.0").apply(false)
    id("com.diffplug.spotless").version("6.14.0").apply(false)
    id("com.github.ben-manes.versions").version("0.39.0").apply(false)
    id("maven-publish")
}

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

subprojects {
    // ALL projects get the appropriately tracked version
    version = "0.7.0"
    // all projects get the group
    group = "tf.veriny.wishport"

    apply(plugin = "org.jetbrains.kotlin.multiplatform")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "com.github.ben-manes.versions")

    // core kotlin configuration
    configure<KotlinMultiplatformExtension> {
        explicitApi = ExplicitApiMode.Strict

        // == Linux Targets == //
        // = AMD64 = //
        linuxX64()
        // = AArch64 = //
        linuxArm64()

        // == Windows Targets == //
        // = Windows (AMD64) = //
        // mingwX64()

        sourceSets {
            val commonMain by getting {
                dependencies {
                    // required to stop intellij from flipping out
                    implementation(kotlin("stdlib"))
                    implementation(kotlin("reflect"))
                }
            }
            val commonTest by getting {
                dependencies {
                    implementation(kotlin("test-common"))
                    implementation(kotlin("test-annotations-common"))
                }
            }

            // native main sourceset, allows us access to cinterop
            // and common posix stuff.
            val posixMain by creating {
                dependsOn(commonMain)
            }

            // linux sourcesets all share a sourceset
            val linuxMain by creating { dependsOn(posixMain) }

            val linuxMainX64 = getByName("linuxX64Main")
            linuxMainX64.dependsOn(linuxMain)

            val linuxMainArm = getByName("linuxArm64Main")
            linuxMainArm.dependsOn(linuxMain)

            all {
                languageSettings.apply {
                    // enableLanguageFeature("ContextReceivers")
                    optIn("kotlin.RequiresOptIn")
                }
            }
        }
    }

    tasks {
        filter { it.name.startsWith("spotless") }
            .forEach { it.group = "lint" }
        filter { it.name.startsWith("depend") }.forEach { it.group = "dependencies" }

        withType<KotlinCompile>().configureEach {
            kotlinOptions {
                freeCompilerArgs = freeCompilerArgs + "-Xgc=cms"
            }
        }
    }

    configure<SpotlessExtension> {
        kotlin {
            target("src/**/kotlin/**")
            targetExclude("build/generated/**")
            licenseHeaderFile(project.file("LICENCE-HEADER"))
                .onlyIfContentMatches("package tf\\.veriny")
        }
    }
}
