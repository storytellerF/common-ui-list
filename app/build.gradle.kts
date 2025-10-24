import java.util.Base64
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileOutputStream

plugins {
    alias(libs.plugins.compose)
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("com.storyteller_f.version_manager")
    id("kotlin-kapt")
    id("com.starter.easylauncher")
    id("com.google.devtools.ksp")
    id("androidx.navigation.safeargs")
}

android {
    defaultConfig {
        applicationId = "com.storyteller_f.common_ui_list_sample"
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        viewBinding = true
    }
}

kapt {
//    correctErrorTypes = true
    useBuildCache = true
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.retrofit.mock)
    implementation(libs.log.receptor)
    implementation(libs.converter.gson)
}
val signPath: String? = getenv("storyteller_f_sign_path")
val signKey: String? = getenv("storyteller_f_sign_key")
val signAlias: String? = getenv("storyteller_f_sign_alias")
val signStorePassword: String? = getenv("storyteller_f_sign_store_password")
val signKeyPassword: String? = getenv("storyteller_f_sign_key_password")
val generatedJksFile =
    layout.buildDirectory.file("signing/signing_key.jks").get().asFile
val javaVersion = JavaVersion.VERSION_21
android {
    compileSdk = 36
    defaultConfig {
        "com.storyteller_f.common_ui_list".let<kotlin.String, kotlin.Unit> {
            namespace = it
        }
        minSdk = null ?: 23
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        val signStorePath = when {
            signPath != null -> File(signPath)
            signKey != null -> generatedJksFile
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
}
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
        optIn.add("kotlin.RequiresOptIn")
    }
}

val decodeBase64ToStoreFileTask = tasks.register("decodeBase64ToStoreFile") {
    group = "signing"
    val signKey = getenv("storyteller_f_sign_key")
    val generatedJksFile = layout.buildDirectory.file("signing/signing_key.jks").get().asFile

    inputs.property("signKey", signKey ?: "")
    outputs.file(generatedJksFile)
    doLast {
        if (!signKey.isNullOrBlank()) {
            // 定义输出文件路径 (如密钥存储文件)
            val outputFile = generatedJksFile

            outputFile.parentFile!!.let {
                if (!it.exists() && !it.mkdirs()) {
                    throw Exception("mkdirs failed: $it")
                }
            }
            if (!outputFile.exists() && !outputFile.createNewFile()) {
                throw Exception("create failed: $outputFile")
            }
            // 将 Base64 解码为字节
            val decodedBytes = Base64.getDecoder().decode(signKey)

            // 将解码后的字节写入文件
            FileOutputStream(outputFile).use { it.write(decodedBytes) }

            println("Base64 decoded and written to: $outputFile")
        } else {
            println("skip decodeBase64ToStoreFile")
        }

    }

}

afterEvaluate {
    tasks["packageRelease"]?.dependsOn(decodeBase64ToStoreFileTask)
}

dependencies {
    implementation("com.github.storytellerF.Bao:startup:2.4.0")
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

    ksp(libs.room.compiler)

    "debugImplementation"(libs.leak.canary)
    implementation(libs.multi.dex)
}
android {
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
}
dependencies {
    implementation(libs.compos.material)
    implementation(libs.compose.ui.tooling)
    implementation(project(":view-holder-compose"))
    ksp(project(":ext-func-compiler"))
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.nav.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.android.junit)
    androidTestImplementation(libs.android.espresso)
}
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(listOf<kotlin.String>("-Xcontext-parameters"))
    }
}
dependencies {
    dependencies {
        implementation(project(":common-pr"))
    }
}

fun getenv(key: String): String? {
    return System.getenv(key) ?: System.getenv(key.uppercase())
}