import com.storyteller_f.version_manager.apiModule
import com.storyteller_f.version_manager.implModule
import com.storyteller_f.version_manager.pureKotlinLanguageLevel

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("kotlin-kapt")
    id("common-publish")
    id("com.storyteller_f.version_manager")
}

pureKotlinLanguageLevel()

dependencies {
    apiModule(":ui-list-annotation-definition")
    implModule(":slim-ktx")
    implModule(":ui-list-annotation-common")
}