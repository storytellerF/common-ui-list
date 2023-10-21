import com.storyteller_f.version_manager.baseLibrary
import com.storyteller_f.version_manager.commonAndroidDependency
import com.storyteller_f.version_manager.unitTestDependency

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("common-version-manager")
    id("common-publish")
}

android {
    namespace = "com.storyteller_f.compat_ktx"
    defaultConfig {
        minSdk = 16
    }
}

dependencies {
    commonAndroidDependency()
    unitTestDependency()
}

baseLibrary()