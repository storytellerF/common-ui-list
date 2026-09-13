import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.compose)
    id("com.android.application")


    id("kotlin-parcelize")

    id("com.starter.easylauncher")
    id("com.google.devtools.ksp")
    id("androidx.navigation.safeargs")
}

//kapt {
//    correctErrorTypes = true
//    useBuildCache = true
//}

val javaVersion = JavaVersion.VERSION_21
android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    namespace = "com.storyteller_f.common_ui_list"
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        applicationId = "com.storyteller_f.common_ui_list_sample"
        versionCode = 1
        versionName = "1.0"
    }
    signingConfigs {
        val signPath = System.getenv("storyteller_f_sign_path")
        val signAlias = System.getenv("storyteller_f_sign_alias")
        val signStorePassword = System.getenv("storyteller_f_sign_store_password")
        val signKeyPassword = System.getenv("storyteller_f_sign_key_password")
        val signStorePath = signPath?.let(::File)
        if (signStorePath != null && signAlias != null && signStorePassword != null && signKeyPassword != null) {
            create("release") {
                keyAlias = signAlias
                keyPassword = signKeyPassword
                storeFile = signStorePath
                storePassword = signStorePassword
            }
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSignConfig = signingConfigs.findByName("release")
            if (releaseSignConfig != null)
                signingConfig = releaseSignConfig
        }
    }
    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    dependenciesInfo {
        includeInBundle = false
        includeInApk = false
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
}
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
        optIn.add("kotlin.RequiresOptIn")
        freeCompilerArgs.addAll(listOf("-Xcontext-parameters"))
    }
}

dependencies {
    implementation(project(":compat-ktx"))
    implementation(project(":common-ui"))
    implementation(project(":common-vm-ktx"))
    implementation(project(":ui-list"))
    implementation(project(":ui-list-annotation-definition"))
    ksp(project(":ui-list-annotation-compiler-ksp"))
    implementation(libs.core)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.fragment.ktx)
    implementation(libs.activity.ktx)
    implementation(libs.swipe.refresh)
    implementation(libs.retrofit)
    implementation(libs.retrofit.mock)
    implementation(libs.log.receptor)
    implementation(libs.converter.gson)
    implementation(libs.coroutines)
    implementation(libs.coroutines.android)
    implementation(libs.paging.runtime.ktx)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)
    kspTest(libs.room.compiler)

    debugImplementation(libs.leak.canary)
    implementation(libs.compos.material)
    implementation(libs.compose.ui.tooling)
    implementation(project(":view-holder-compose"))
    ksp(project(":ext-func-compiler"))
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.nav.ui.ktx)
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.16.1")
    androidTestImplementation(libs.android.junit)
    androidTestImplementation(libs.android.espresso)
}
