@file:Suppress("UnstableApiUsage")

import com.storyteller_f.version_manager.Versions
import com.storyteller_f.version_manager.baseLibrary
import com.storyteller_f.version_manager.implModule
import com.storyteller_f.version_manager.unitTestDependency

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

    namespace = "com.storyteller_f.file_system"
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}
baseLibrary()
dependencies {
    implModule(":common-ktx")
    implModule(":compat-ktx")
    implModule(":multi-core")

    implementation("com.google.android.material:material:${Versions.MATERIAL}")

    unitTestDependency()
    // https://mvnrepository.com/artifact/androidx.test.uiautomator/uiautomator
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0-alpha04")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.COROUTINES}")


    implementation("com.google.code.gson:gson:2.10.1")
}
