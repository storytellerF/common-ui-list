plugins {
    id("org.jetbrains.kotlin.jvm")
    id("kotlin-library")
    id("common-publish")
}

dependencies {
    api(project(":ext-func-definition"))
    implementation(libs.symbol.processing.api)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.compile.testing.ksp)
}
