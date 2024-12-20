@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("version-manager")
    includeBuild("common-publish")
//    includeBuild("../easylauncher-gradle-plugin")
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
include(":ui-list-annotation-definition")
//include(":ui-list-annotation-compiler")
include(":ui-list-annotation-compiler-ksp")
include(":ui-list-annotation-common")
include(":ext-func-compiler")
include(":ext-func-definition")
include(":composite-definition")
include(":composite-compiler-ksp")

include(":common-vm-ktx")
include(":common-ui")
include(":common-ktx")
include(":common-pr")
include(":slim-ktx")
include(":compat-ktx")

//val userHome: String? = System.getProperty("user.home")
//include("sardine-android")
//project(":sardine-android").projectDir = file("$userHome/AndroidStudioProjects/sardine-android")