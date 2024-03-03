import com.storyteller_f.version_manager.*

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.storyteller_f.version_manager")
    id("common-publish")
}

android {

    defaultConfig {
        minSdk = 16
    }
    namespace = "com.storyteller_f.common_ktx"
}
baseLibrary(true)

dependencies {
    commonAndroidDependency()
    unitTestDependency()
}