plugins {
    kotlin("jvm")
    id("kotlin-library")
    id("common-publish")
}

dependencies {
    implementation(project(":slim-ktx"))
    implementation(project(":ui-list-annotation-definition"))
    implementation(project(":ui-list-annotation-common"))
    implementation(libs.symbol.processing.api)
}