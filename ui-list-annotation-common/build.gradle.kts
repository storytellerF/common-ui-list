import com.storyteller_f.version_manager.pureKotlinLanguageLevel

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("common-version-manager")
    id("common-publish")
}
pureKotlinLanguageLevel()
dependencies {
    implementation(project(":slim-ktx"))
}