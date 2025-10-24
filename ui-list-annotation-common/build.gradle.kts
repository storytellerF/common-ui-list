plugins {
    id("org.jetbrains.kotlin.jvm")
    id("kotlin-library")
    id("common-publish")
}

dependencies {
    implementation(project(":slim-ktx"))
}