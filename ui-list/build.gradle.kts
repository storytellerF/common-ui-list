import com.storyteller_f.version_manager.*

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
    id("kotlin-kapt")
    id("common-version-manager")
    id("common-publish")
}

android {
    defaultConfig {
        minSdk = 21
    }

    buildFeatures {
        viewBinding = true
    }
    namespace = "com.storyteller_f.ui_list"
}

baseLibrary()

dependencies {
    implModule(":ui-list-annotation-definition")
    unitTestDependency()

    //components
    commonAndroidDependency()
    implementation("androidx.recyclerview:recyclerview:${Versions.RECYCLERVIEW}")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("androidx.fragment:fragment-ktx:${Versions.FRAGMENT_KTX}")
    implementation("androidx.activity:activity-ktx:${Versions.ACTIVITY_KTX}")

    //coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.COROUTINES}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.COROUTINES}")

    // lifecycle & view model
    api("androidx.lifecycle:lifecycle-runtime-ktx:${Versions.LIFECYCLE}")
    api("androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.LIFECYCLE}")
    api("androidx.lifecycle:lifecycle-livedata-ktx:${Versions.LIFECYCLE}")
    api("androidx.lifecycle:lifecycle-viewmodel-savedstate:${Versions.LIFECYCLE}")
    apiModule(":common-vm-ktx")

    // room
    api("androidx.room:room-runtime:${Versions.ROOM}")
    api("androidx.room:room-ktx:${Versions.ROOM}")
    api("androidx.room:room-paging:${Versions.ROOM}")

    api("androidx.paging:paging-runtime-ktx:${Versions.PAGING}")

    kapt("androidx.databinding:databinding-compiler-common:${Versions.DATA_BINDING_COMPILER}")

    // retrofit & okhttp
    implementation("com.squareup.retrofit2:converter-gson:${Versions.RETROFIT}")
}