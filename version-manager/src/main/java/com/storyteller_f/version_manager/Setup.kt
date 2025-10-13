@file:Suppress("unused", "UnstableApiUsage", "DEPRECATION")

package com.storyteller_f.version_manager

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import java.io.File
import java.io.FileOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

fun Project.setupExtFunc() {
    loadPlugin("com.google.devtools.ksp")
    dependencies {
        kspModule(":ext-func-compiler")
    }
}

fun Project.setupCompose(isLibrary: Boolean = false, supportUiList: Boolean = true) {
    if (isLibrary) {
        androidLibrary {
            buildFeatures {
                compose = true
            }
            composeOptions {
                kotlinCompilerExtensionVersion = BuildConfig.`version compose_compiler`
            }
        }
    } else
        androidApp {
            buildFeatures {
                compose = true
            }
            composeOptions {
                kotlinCompilerExtensionVersion = BuildConfig.`version compose_compiler`
            }
        }
    dependencies {
        composeDependency()
        if (supportUiList) {
            implModule(":view-holder-compose")
        }
    }
}

/**
 * 设置compose，extFunc，navigation，unitTest
 */
fun Project.setupGeneric() {
    setupCompose()
    setupExtFunc()
    dependencies {
        navigationDependency()
        unitTestDependency()
    }
}

fun Project.setupPreviewFeature() {
    androidKotlin {
        compilerOptions {
            freeCompilerArgs.addAll(listOf("-Xcontext-parameters"))
        }
    }
    dependencies {
        implModule(":common-pr")
    }
}

fun Project.loadPlugin(id: String) {
    if (!plugins.hasPlugin(id)) plugins.apply(id)
}

fun getenv(key: String): String? {
    return System.getenv(key) ?: System.getenv(key.uppercase())
}

/**
 * 默认最小SDK 为[Versions.DEFAULT_MIN_SDK]，如果需要重新定制需要在baseApp 之后指定，否则会被覆盖
 * 注意副作用：
 *  1. 读取[com.android.build.gradle.internal.dsl.DefaultConfig.applicationId]
 *  2. 在debug 中复写[com.android.build.api.dsl.ApplicationVariantDimension.applicationIdSuffix]
 */
@OptIn(ExperimentalEncodingApi::class)
fun Project.baseApp(minSdkInt: Int? = null, namespaceString: String? = null) {
    val signPath: String? = getenv("storyteller_f_sign_path")
    val signKey: String? = getenv("storyteller_f_sign_key")
    val signAlias: String? = getenv("storyteller_f_sign_alias")
    val signStorePassword: String? = getenv("storyteller_f_sign_store_password")
    val signKeyPassword: String? = getenv("storyteller_f_sign_key_password")
    val generatedJksFile = layout.buildDirectory.file("signing/signing_key.jks").get().asFile
    val javaVersion = JavaVersion.VERSION_21
    androidApp {
        compileSdk = Versions.COMPILE_SDK
        defaultConfig {
            namespaceString?.let {
                namespace = it
            }
            minSdk = minSdkInt ?: Versions.DEFAULT_MIN_SDK
            targetSdk = Versions.TARGET_SDK
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

    androidKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
            optIn.add("kotlin.RequiresOptIn")
        }
    }


    val decodeBase64ToStoreFileTask = tasks.register("decodeBase64ToStoreFile") {
        group = "signing"
        doLast {
            if (signKey != null) {
                // 定义输出文件路径 (如密钥存储文件)
                val outputFile = generatedJksFile

                outputFile.parentFile?.let {
                    if (!it.exists()) {
                        if (!it.mkdirs()) {
                            throw Exception("mkdirs failed: $it")
                        }
                    }
                }
                if (!outputFile.exists()) {
                    if (!outputFile.createNewFile()) {
                        throw Exception("create failed: $outputFile")
                    }
                }
                // 将 Base64 解码为字节
                val decodedBytes = Base64.decode(signKey, 0, signKey.length)

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
    loadBao()
    baseAppDependency()
}

private fun Project.loadBao() {
    dependencies {
        val baoModule = findProject(":bao:startup")
        if (baoModule != null)
            "implementation"(baoModule)
        else
            "implementation"("com.github.storytellerF.Bao:startup:${Versions.BAO}")
    }

}

/**
 * 默认最小SDK 为[Versions.DEFAULT_MIN_SDK]，如果需要重新定制需要在baseLibrary 之后指定，否则会被覆盖
 */
fun Project.baseLibrary(
    enableMultiDex: Boolean = false,
    minSdkInt: Int? = null,
    namespaceString: String? = null
) {
    val javaVersion = JavaVersion.VERSION_21
    androidLibrary {
        compileSdk = Versions.COMPILE_SDK

        defaultConfig {
            namespaceString?.let {
                namespace = it
            }
            minSdk = minSdkInt ?: Versions.DEFAULT_MIN_SDK
            targetSdk = Versions.TARGET_SDK
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            consumerProguardFiles("consumer-rules.pro")
        }

        buildTypes {
            debug {
                if (enableMultiDex) {
                    multiDexEnabled = true
                }
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
    androidKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
            optIn.add("kotlin.RequiresOptIn")
        }
    }
}

internal fun Project.androidLibrary(configure: Action<LibraryExtension>): Unit =
    (this as ExtensionAware).extensions.configure("android", configure)

internal fun Project.androidApp(configure: Action<BaseAppModuleExtension>): Unit =
    (this as ExtensionAware).extensions.configure("android", configure)

internal fun Project.java(configure: Action<org.gradle.api.plugins.JavaPluginExtension>): Unit =
    (this as ExtensionAware).extensions.configure("java", configure)

fun Project.androidComponents(configure: Action<ApplicationAndroidComponentsExtension>): Unit =
    (this as ExtensionAware).extensions.configure("androidComponents", configure)

fun Project.androidKotlin(configure: Action<KotlinAndroidProjectExtension>): Unit =
    (this as ExtensionAware).extensions.configure("kotlin", configure)

fun KotlinJvmCompilerOptions.addArgs(arg: String) {
    freeCompilerArgs.addAll(listOf(arg))
}

fun Project.pureKotlin(configure: Action<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>) {
    (this as ExtensionAware).extensions.configure("kotlin", configure)
}


fun Project.pureKotlinLanguageLevel() {
    val javaVersion = JavaVersion.VERSION_21
    java {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    pureKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
            optIn.add("kotlin.RequiresOptIn")
        }
    }
}

/**
 * 必须手动加载ksp 插件
 */
fun Project.constraintCommonUIListVersion(
    version: String,
    group: String = "com.github.storytellerF.common-ui-list"
) {
    val p = this
    dependencies {
        constraints {
            listOf(
                "common-ktx",
                "common-pr",
                "common-ui",
                "common-vm-ktx",
                "compat-ktx",
                "composite-compiler-ksp",
                "composite-definition",
                "ext-func-compiler",
                "ext-func-definition",
                "slim-ktx",
                "ui-list",
                "ui-list-annotation-common",
                "ui-list-annotation-compiler-ksp",
                "ui-list-annotation-definition",
                "view-holder-compose"
            ).forEach {
                "implementation"("$group:$it:$version")
                if (p.extensions.findByName("ksp") != null)
                    "ksp"("$group:$it:$version")
            }
        }
    }

}
