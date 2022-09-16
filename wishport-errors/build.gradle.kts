
kotlin {
    sourceSets.getByName("commonMain").dependencies {
        api(project(":wishport-helpers"))
    }
}