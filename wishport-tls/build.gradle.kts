
kotlin {
    sourceSets.getByName("commonMain").apply {
        dependencies {
            api(project(":wishport-helpers"))
            api(project(":wishport-core"))
        }
    }

    sourceSets.getByName("linuxMain").apply {
        dependencies {
            implementation(project(":wishport-static-uring"))
            implementation(project(":wishport-static-openssl"))
        }
    }
}
