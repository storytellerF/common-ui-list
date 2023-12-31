@file:Suppress("UnstableApiUsage")

pluginManagement {
    val smlFolder: String by settings
    includeBuild("version-manager")
    includeBuild("common-publish")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven {
            setUrl("https://jitpack.io")
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://artifactory.cronapp.io/public-release/") }
    }
}
rootProject.name = "common_ui_list"
if (System.getenv()["JITPACK"] == null) {
    include(":app")
}

include(":ui-list")
include(":view-holder-compose")
include(":ui-list-annotation-compiler")
include(":ui-list-annotation-definition")
include(":ui-list-annotation-compiler-ksp")
include(":ui-list-annotation-common")
include(":ext-func-compiler")
include(":ext-func-definition")
include(":composite-definition")
include(":composite-compiler")

include(":common-vm-ktx")
include(":common-ui")
include(":common-ktx")
include(":common-pr")
include(":slim-ktx")
include(":compat-ktx")

include(":file-system")
include(":file-system-ktx")
include(":file-system-remote")
include(":file-system-root")
