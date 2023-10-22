import com.storyteller_f.version_manager.baseLibrary
import com.storyteller_f.version_manager.unitTestDependency
import com.storyteller_f.version_manager.commonAndroidDependency
import com.storyteller_f.version_manager.implModule

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("common-publish")
    id("common-version-manager")
}

android {
    namespace = "com.storyteller_f.file_system_root"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
    }
}
baseLibrary()

dependencies {

    commonAndroidDependency()

    val libsuVersion = "5.0.3"
    implementation("com.github.topjohnwu.libsu:nio:${libsuVersion}")

    unitTestDependency()
    implModule(":file-system")
}