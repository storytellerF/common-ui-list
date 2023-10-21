import com.storyteller_f.version_manager.Versions
import com.storyteller_f.version_manager.pureKotlinLanguageLevel

plugins {
    kotlin("jvm")
    id("common-version-manager")
    id("common-publish")
}
pureKotlinLanguageLevel()

dependencies {
    implementation(project(":ui-list-annotation-definition"))
    implementation("com.google.devtools.ksp:symbol-processing-api:${Versions.kspVersion}")
    implementation(project(":ui-list-annotation-common"))
}