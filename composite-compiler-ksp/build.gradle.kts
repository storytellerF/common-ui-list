plugins {
    id("org.jetbrains.kotlin.jvm")
    id("common-publish")
    id("kotlin-library")
}

dependencies {
    dependencies {
        implementation(project(":composite-definition"))
    }

    implementation(libs.symbol.processing.api)
}