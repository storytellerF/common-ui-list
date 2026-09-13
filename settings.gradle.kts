@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { setUrl("https://maven.aliyun.com/repository/central") }
        google()
        mavenCentral()
        maven { setUrl("https://artifactory.cronapp.io/public-release/") }
    }
}
rootProject.name = "common_ui_list"
include(":app")

include(":ui-list")
include(":view-holder-compose")
include(":ui-list-annotation-definition")
//include(":ui-list-annotation-compiler")
include(":ui-list-annotation-compiler-ksp")
include(":ui-list-annotation-common")
include(":ext-func-compiler")
include(":ext-func-definition")
include(":common-vm-ktx")
include(":common-ui")
include(":compat-ktx")


includeBuild("bgscripts")
includeBuild("common-publish")
