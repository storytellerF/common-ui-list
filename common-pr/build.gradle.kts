import com.storyteller_f.version_manager.*

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("common-version-manager")
    id("common-publish")
}

android {
    defaultConfig {
        minSdk = 21
    }
    kotlinOptions {
        freeCompilerArgs = listOf("-Xcontext-receivers")
    }
    namespace = "com.storyteller_f.common_pr"
}
baseLibrary()
setupExtFunc()
dependencies {
    implementation(project(":ext-func-definition"))
    implementation(project(":common-ktx"))
    implementation(project(":common-vm-ktx"))
    implementation(project(":common-ui"))
    commonAndroidDependency()
    unitTestDependency()
}
