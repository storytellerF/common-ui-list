plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("custom-android-library")
    id("common-publish")
}

android {
    namespace = "com.storyteller_f.view_holder_compose"
}

dependencies {
    testImplementation(libs.junit)
    androidTestImplementation(libs.android.junit)
    androidTestImplementation(libs.android.espresso)
    api(libs.ui)
    implementation(project(":ui-list"))
}