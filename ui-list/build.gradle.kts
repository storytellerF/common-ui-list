import com.storyteller_f.version_manager.Libraries
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
    implementation(Libraries.RECYCLERVIEW)
    implementation(Libraries.SWIPE_REFRESH)

    //coroutines
    coroutineDependency()

    // lifecycle & view model
    api(libs.lifecycle.runtime.ktx)
    api(libs.lifecycle.viewmodel.ktx)
    api(libs.lifecycle.livedata.ktx)
    api(libs.lifecycle.viewmodel.savedstate)
    apiModule(":common-vm-ktx")

    // room
    api(libs.room.runtime)
    api(libs.room.ktx)
    api(libs.room.paging)

    api(libs.paging.runtime.ktx)

    // retrofit & okhttp
    implementation(libs.converter.gson)
}