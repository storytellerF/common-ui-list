import com.storyteller_f.version_manager.pureKotlinLanguageLevel

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("kotlin-kapt")
    id("common-publish")
    id("common-version-manager")
}

pureKotlinLanguageLevel()

dependencies {
    api(project(":ui-list-annotation-definition"))
    implementation(project(":slim-ktx"))
    implementation(project(":ui-list-annotation-common"))
}