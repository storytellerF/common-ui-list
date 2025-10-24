plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("common-publish")
    id("custom-android-library")
}

android {
    namespace = "com.storyteller_f.compat_ktx"
    buildTypes {
        debug {
            multiDexEnabled = true
        }
    }
    
}

dependencies {
    implementation(libs.core)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.fragment.ktx)
    implementation(libs.activity.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.android.junit)
    androidTestImplementation(libs.android.espresso)
}
