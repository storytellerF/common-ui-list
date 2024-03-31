import com.storyteller_f.version_manager.baseLibrary
import com.storyteller_f.version_manager.commonAppDependency
import com.storyteller_f.version_manager.unitTestDependency

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.storyteller_f.version_manager")
    id("common-publish")
}

android {
    namespace = "com.storyteller_f.compat_ktx"
    defaultConfig {
        minSdk = 16
    }
}

dependencies {
    commonAppDependency()
    unitTestDependency()
}

baseLibrary(true)