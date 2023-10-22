import com.storyteller_f.version_manager.implModule
import com.storyteller_f.version_manager.pureKotlinLanguageLevel

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.storyteller_f.version_manager")
    id("common-publish")
}
pureKotlinLanguageLevel()
dependencies {
    implModule(":slim-ktx")
}