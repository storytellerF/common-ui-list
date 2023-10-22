import com.storyteller_f.version_manager.Versions
import com.storyteller_f.version_manager.apiModule
import com.storyteller_f.version_manager.baseLibrary
import com.storyteller_f.version_manager.commonAndroidDependency
import com.storyteller_f.version_manager.implModule
import com.storyteller_f.version_manager.setupCompose
import com.storyteller_f.version_manager.setupExtFunc
import com.storyteller_f.version_manager.unitTestDependency

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("common-version-manager")
    id("common-publish")
}

android {
    defaultConfig {
        minSdk = 21
    }

    buildFeatures {
        dataBinding = true
    }

    namespace = "com.storyteller_f.common_ui"
}
baseLibrary()
setupCompose(true)
setupExtFunc()
dependencies {
    apiModule(":ext-func-definition")
    implModule(":common-ktx")
    implModule(":compat-ktx")
    implModule(":common-vm-ktx")
    implementation("androidx.navigation:navigation-runtime-ktx:2.7.4")
    implementation("androidx.databinding:viewbinding:${Versions.DATA_BINDING_COMPILER}")

    commonAndroidDependency()
    unitTestDependency()
}