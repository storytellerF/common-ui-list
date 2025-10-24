plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("custom-android-library")
    id("common-publish")
    id("com.google.devtools.ksp")
    alias(libs.plugins.compose)
}

android {
    defaultConfig {
        namespace = "com.storyteller_f.common_ui"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
}

dependencies {
    ksp(project(":ext-func-compiler"))
    api(project(":ext-func-definition"))
    implementation(project(":common-ktx"))
    implementation(project(":slim-ktx"))
    implementation(project(":compat-ktx"))
    implementation(project(":common-vm-ktx"))
    implementation(libs.navigation.runtime.ktx)
    implementation(libs.viewbinding)
    implementation(libs.compos.material)
    implementation(libs.compose.ui.tooling)

    implementation(libs.core)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.fragment.ktx)
    implementation(libs.activity.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.android.junit)
    androidTestImplementation(libs.android.espresso)
}