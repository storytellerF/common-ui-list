import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.storyteller_f.version_manager")
    id("common-publish")
    id("com.google.devtools.ksp")
    alias(libs.plugins.compose)
}

val javaVersion = JavaVersion.VERSION_21
android {
    compileSdk = 36

    defaultConfig {
        "com.storyteller_f.common_ui"?.let<kotlin.String, kotlin.Unit> {
            namespace = it
        }
        minSdk = null ?: 23
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
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
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
        optIn.add("kotlin.RequiresOptIn")
    }
}

dependencies {
    ksp(project(":ext-func-compiler"))
    "api"(project(":ext-func-definition"))
    implementation(project(":common-ktx"))
    implementation(project(":slim-ktx"))
    implementation(project(":compat-ktx"))
    implementation(project(":common-vm-ktx"))
    implementation(libs.navigation.runtime.ktx)
    implementation(libs.viewbinding)
    "implementation"(libs.compos.material)
    "implementation"(libs.compose.ui.tooling)

    implementation(libs.core)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.fragment.ktx)
    implementation(libs.activity.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.android.junit)
    androidTestImplementation(libs.android.espresso)
}