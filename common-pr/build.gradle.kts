import com.storyteller_f.version_manager.*

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.storyteller_f.version_manager")
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
    implModule(":ext-func-definition")
    implModule(":common-ktx")
    implModule(":common-vm-ktx")
    implModule(":common-ui")
    commonAndroidDependency()
    unitTestDependency()
    // https://mvnrepository.com/artifact/androidx.navigation/navigation-common-ktx
    implementation("androidx.navigation:navigation-common-ktx:${Versions.NAV}")
    implementation("androidx.navigation:navigation-fragment-ktx:${Versions.NAV}")
}
