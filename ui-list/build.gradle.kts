import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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

val javaVersion = JavaVersion.VERSION_21
android {
    compileSdk = 36

    defaultConfig {
        "com.storyteller_f.ui_list"?.let<kotlin.String, kotlin.Unit> {
            namespace = it
        }
        minSdk = null ?: 23
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            multiDexEnabled = true
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
}
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
        optIn.add("kotlin.RequiresOptIn")
    }
}

dependencies {
    dependencies {
        implementation(project(":common-ktx"))
    }
    dependencies {
        implementation(project(":slim-ktx"))
    }
    dependencies {
        implementation(project(":ui-list-annotation-definition"))
    }
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
    dependencies {
        "api"(project(":common-vm-ktx"))
    }

    // room
    api(libs.room.runtime)
    api(libs.room.ktx)
    api(libs.room.paging)

    api(libs.paging.runtime.ktx)

    // retrofit & okhttp
    implementation(libs.converter.gson)
}