import com.storyteller_f.version_manager.Versions
import com.storyteller_f.version_manager.apiModule
import com.storyteller_f.version_manager.baseLibrary
import com.storyteller_f.version_manager.commonAppDependency
import com.storyteller_f.version_manager.coroutineDependency
import com.storyteller_f.version_manager.implModule
import com.storyteller_f.version_manager.unitTestDependency

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
    id("com.storyteller_f.version_manager")
    id("common-publish")
}

android {
    buildFeatures {
        viewBinding = true
    }
}

baseLibrary(enableMultiDex = true, minSdkInt = 19, namespaceString = "com.storyteller_f.ui_list")

dependencies {
    implModule(":common-ktx")
    implModule(":slim-ktx")
    implModule(":ui-list-annotation-definition")
    unitTestDependency()

    //components
    commonAppDependency()
    implementation("androidx.recyclerview:recyclerview:${Versions.RECYCLERVIEW}")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:${Versions.SWIPE_REFRESH}")

    //coroutines
    coroutineDependency()

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

    // retrofit & okhttp
    implementation("com.squareup.retrofit2:converter-gson:${Versions.RETROFIT}")
}