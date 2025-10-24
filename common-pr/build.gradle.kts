plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("custom-android-library")
    id("common-publish")
    id("com.google.devtools.ksp")
}

android {
    defaultConfig {
        namespace = "com.storyteller_f.common_pr"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}
kotlin {
    compilerOptions {
        freeCompilerArgs = listOf("-Xcontext-parameters")
    }
}

dependencies {
    ksp(project(":ext-func-compiler"))
    implementation(project(":ext-func-definition"))
    implementation(project(":common-ktx"))
    implementation(project(":common-vm-ktx"))
    implementation(project(":common-ui"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.android.junit)
    androidTestImplementation(libs.android.espresso)
    implementation(libs.navigation.common.ktx)
    implementation(libs.navigation.fragment.ktx)
}
