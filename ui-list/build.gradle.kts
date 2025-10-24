plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
    id("common-publish")
    id("custom-android-library")
}

android {
    defaultConfig {
        namespace = "com.storyteller_f.ui_list"
    }

    buildTypes {
        debug {
            multiDexEnabled = true
        }
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(project(":common-ktx"))
    implementation(project(":slim-ktx"))
    implementation(project(":ui-list-annotation-definition"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.android.junit)
    androidTestImplementation(libs.android.espresso)

    //components
    implementation(libs.core)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.fragment.ktx)
    implementation(libs.activity.ktx)
    implementation(libs.recycleview)
    implementation(libs.swipe.refresh)

    //coroutines
    implementation(libs.coroutines)
    implementation(libs.coroutines.android)

    // lifecycle & view model
    api(libs.lifecycle.runtime.ktx)
    api(libs.lifecycle.viewmodel.ktx)
    api(libs.lifecycle.livedata.ktx)
    api(libs.lifecycle.viewmodel.savedstate)
    api(project(":common-vm-ktx"))

    // room
    api(libs.room.runtime)
    api(libs.room.ktx)
    api(libs.room.paging)

    api(libs.paging.runtime.ktx)

    // retrofit & okhttp
    implementation(libs.converter.gson)
}