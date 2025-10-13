import com.storyteller_f.version_manager.baseApp
import com.storyteller_f.version_manager.networkDependency
import com.storyteller_f.version_manager.setupGeneric
import com.storyteller_f.version_manager.setupPreviewFeature

plugins {
    alias(libs.plugins.compose)
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("com.storyteller_f.version_manager")
    id("kotlin-kapt")
    id("com.starter.easylauncher")
    id("androidx.navigation.safeargs")
}

android {
    defaultConfig {
        applicationId = "com.storyteller_f.common_ui_list_sample"
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        viewBinding = true
    }
}

kapt {
//    correctErrorTypes = true
    useBuildCache = true
}

dependencies {
    networkDependency()
    implementation(libs.converter.gson)
}
baseApp(namespaceString = "com.storyteller_f.common_ui_list")
setupGeneric()
setupPreviewFeature()
