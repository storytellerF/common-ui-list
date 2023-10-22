import com.storyteller_f.version_manager.Versions
import com.storyteller_f.version_manager.*

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("common-version-manager")
    id("common-publish")
}

pureKotlinLanguageLevel()

dependencies {
    implModule(":slim-ktx")
    apiModule(":ext-func-definition")
    implementation("com.google.devtools.ksp:symbol-processing-api:${Versions.KSP}")
}