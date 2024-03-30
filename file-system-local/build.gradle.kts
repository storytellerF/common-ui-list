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
    namespace = "com.storyteller_f.file_system_local"

    defaultConfig {
        minSdk = 16
    }
}

baseLibrary(true)

dependencies {
    implModule(":file-system")
    implModule(":slim-ktx")
    implModule(":compat-ktx")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    unitTestDependency()
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.android.material:material:${Versions.MATERIAL}")
    // https://mvnrepository.com/artifact/androidx.test.uiautomator/uiautomator
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.COROUTINES}")
}