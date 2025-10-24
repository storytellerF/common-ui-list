plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("custom-android-library")
    id("common-publish")
}

android {
    namespace = "com.storyteller_f.common_vm_ktx"
    
}

dependencies {
    ksp(project(":ext-func-compiler"))
    implementation(project(":ext-func-definition"))
    implementation(libs.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.android.junit)
    androidTestImplementation(libs.android.espresso)

    implementation(libs.lifecycle.viewmodel.ktx)
}