import com.storyteller_f.version_manager.Versions
import com.storyteller_f.version_manager.implModule

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("common-publish")
    id("com.storyteller_f.version_manager")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implModule(":composite-definition")

    implementation("com.google.devtools.ksp:symbol-processing-api:${Versions.KSP}")
}