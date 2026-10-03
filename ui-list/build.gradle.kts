plugins {
    id("com.android.library")


    id("android-common-publish")
    id("custom-android-library")
}

android {
    namespace = "com.storyteller_f.ui_list"
    buildTypes {
        debug {
            multiDexEnabled = true
        }
    }
    
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    buildFeatures {
        viewBinding = true
    }

}

// Robolectric's API 36 shared-memory setup accesses SharedSecrets on JDK 21.
// Keep this export confined to test JVMs.
tasks.withType<Test>().configureEach {
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
}

dependencies {
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

    api(libs.paging.runtime.ktx)
}
