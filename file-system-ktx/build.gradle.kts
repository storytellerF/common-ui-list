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

    namespace = "com.storyteller_f.file_system_ktx"
}
baseLibrary()
dependencies {
    implementation(project(":file-system"))
    implementation(project(":file-system-remote"))
    implementation(project(":file-system-root"))
    implementation("androidx.core:core-ktx:${Versions.coreVersion}")
    implementation("androidx.appcompat:appcompat:${Versions.appcompatVersion}")
    implementation("com.google.android.material:material:${Versions.materialVersion}")
    implementation("com.j256.simplemagic:simplemagic:1.17")
    unitTestDependency()
}