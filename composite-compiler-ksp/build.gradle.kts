import com.storyteller_f.version_manager.implModule
import com.storyteller_f.version_manager.pureKotlinLanguageLevel

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("common-publish")
    id("com.storyteller_f.version_manager")
}

pureKotlinLanguageLevel()

dependencies {
    implModule(":composite-definition")

    implementation(libs.symbol.processing.api)
}