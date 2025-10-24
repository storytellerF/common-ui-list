plugins {
    id("org.jetbrains.kotlin.jvm")
    id("kotlin-library")
    id("common-publish")
}

dependencies {
    dependencies {
        implementation(project(":slim-ktx"))
    }
}