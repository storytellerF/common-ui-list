import com.storyteller_f.version_manager.Versions
import com.storyteller_f.version_manager.baseLibrary
import com.storyteller_f.version_manager.unitTestDependency
import com.storyteller_f.version_manager.commonAndroidDependency
import com.storyteller_f.version_manager.implModule

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

    implModule(":multi-core")
    commonAndroidDependency()
    implementation("androidx.constraintlayout:constraintlayout:${Versions.CONSTRAINTLAYOUT}")
    unitTestDependency()
    implModule(":compat-ktx")
    // https://mvnrepository.com/artifact/androidx.test.uiautomator/uiautomator
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0-alpha04")

    debugImplementation("androidx.lifecycle:lifecycle-runtime-ktx:${Versions.LIFECYCLE}")
    debugImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.COROUTINES}")
    debugImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.COROUTINES}")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.COROUTINES}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.COROUTINES}")

    implementation("com.google.code.gson:gson:2.10.1")
}