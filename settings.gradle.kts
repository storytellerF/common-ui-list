@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven {
            name = "github"
            url = uri("https://maven.pkg.github.com/storytellerF/jksify")
            credentials {
                // 需要配置在~/.gradle/gradle.properties
                username = providers.gradleProperty("gpr.user").get()
                password = providers.gradleProperty("gpr.key").get()
            }
            mavenContent {
                includeGroupAndSubgroups("com.storytellerF.jksify")
            }
        }
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


includeBuild("bgscripts")
includeBuild("common-publish")