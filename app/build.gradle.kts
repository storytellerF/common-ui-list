import com.storyteller_f.jksify.getenv
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.compose)
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
    id("kotlin-parcelize")

    id("kotlin-kapt")
    id("com.starter.easylauncher")
    id("com.google.devtools.ksp")
    id("androidx.navigation.safeargs")
    id("com.storyteller_f.jksify")
}

kapt {
//    correctErrorTypes = true
    useBuildCache = true
}

val javaVersion = JavaVersion.VERSION_21
android {
    compileSdk = libs.versions.targetSdk.get().toInt()
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
        val signPath: String? = getenv("storyteller_f_sign_path")
        val signKey: String? = getenv("storyteller_f_sign_key")
        val signAlias: String? = getenv("storyteller_f_sign_alias")
        val signStorePassword: String? = getenv("storyteller_f_sign_store_password")
        val signKeyPassword: String? = getenv("storyteller_f_sign_key_password")
        val signStorePath = when {
            signPath != null -> File(signPath)
            signKey != null -> layout.buildDirectory.file("signing/signing_key.jks").get().asFile
            else -> null
        }
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
            resValue(
                "string",
                "leak_canary_display_activity_label",
                defaultConfig.applicationId?.substringAfterLast(".") ?: "Leaks"
            )
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
    implementation(libs.startup)
    implementation(project(":slim-ktx"))
    implementation(project(":common-ktx"))
    implementation(project(":compat-ktx"))
    implementation(project(":common-ui"))
    implementation(project(":ui-list"))
    implementation(project(":ui-list-annotation-definition"))
    ksp(project(":ui-list-annotation-compiler-ksp"))
    implementation(project(":composite-definition"))
    ksp(project(":composite-compiler-ksp"))
    implementation(libs.core)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.fragment.ktx)
    implementation(libs.activity.ktx)
    implementation(libs.retrofit)
    implementation(libs.retrofit.mock)
    implementation(libs.log.receptor)
    implementation(libs.converter.gson)
    ksp(libs.room.compiler)

    debugImplementation(libs.leak.canary)
    implementation(libs.multi.dex)
    implementation(libs.compos.material)
    implementation(libs.compose.ui.tooling)
    implementation(project(":view-holder-compose"))
    ksp(project(":ext-func-compiler"))
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.nav.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.android.junit)
    androidTestImplementation(libs.android.espresso)
    implementation(project(":common-pr"))
}
