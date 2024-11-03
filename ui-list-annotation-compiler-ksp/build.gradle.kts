import com.storyteller_f.version_manager.implModule
import com.storyteller_f.version_manager.pureKotlinLanguageLevel

plugins {
    kotlin("jvm")
    id("com.storyteller_f.version_manager")
    id("common-publish")
}
pureKotlinLanguageLevel()

dependencies {
    implModule(":slim-ktx")
    implModule(":ui-list-annotation-definition")
    implModule(":ui-list-annotation-common")
    implementation(libs.symbol.processing.api)
}