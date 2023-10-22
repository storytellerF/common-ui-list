import com.storyteller_f.version_manager.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("com.storyteller_f.version_manager")
    id("kotlin-kapt")
}

android {

    defaultConfig {
        applicationId = "com.storyteller_f.common_ui_list_structure"
    }

    namespace = "com.storyteller_f.common_ui_list_structure"
}

//kapt {
//    correctErrorTypes = true
//}

dependencies {
    networkDependency()
    implementation("com.squareup.retrofit2:converter-gson:${Versions.RETROFIT}")
}
baseApp()
setupGeneric()
setupDataBinding()
setupPreviewFeature()