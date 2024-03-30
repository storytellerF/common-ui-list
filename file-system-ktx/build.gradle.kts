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

    namespace = "com.storyteller_f.file_system_ktx"
}
baseLibrary()
dependencies {
    apiModule(":file-system")
    implementation("com.j256.simplemagic:simplemagic:1.17")
    unitTestDependency()
}