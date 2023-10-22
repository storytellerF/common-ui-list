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
    implModule(":file-system")
    implModule(":file-system-remote")
    implModule(":file-system-root")
    commonAndroidDependency()
    implementation("com.j256.simplemagic:simplemagic:1.17")
    unitTestDependency()
}