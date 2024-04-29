@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    versionCatalogs {
        create("cul") {
            from(files("../gradle/cul.versions.toml"))
        }
    }
    repositories {
        google()
        mavenCentral()
    }
}
