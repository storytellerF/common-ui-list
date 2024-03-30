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
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Locale

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
                kotlinCompilerExtensionVersion = Versions.COMPOSE_COMPILER
            }
        }
    } else
        androidApp {
            buildFeatures {
                compose = true
            }
            composeOptions {
                kotlinCompilerExtensionVersion = Versions.COMPOSE_COMPILER
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

fun Project.setupDataBinding() {
    loadPlugin("kotlin-kapt")
    androidApp {
        buildFeatures {
            viewBinding = true
            dataBinding = true
        }
    }
    dependencies {
        dataBindingDependency()
    }
}

fun Project.setupPreviewFeature() {
    androidApp {
        kotlinOptionsApp {
            addArgs("-Xcontext-receivers")
        }
    }
    dependencies {
        implModule(":common-pr")
    }
}

fun Project.loadPlugin(id: String) {
    if (!plugins.hasPlugin(id)) plugins.apply(id)
}

/**
 * 增加
 */
fun Project.baseApp() {
    androidApp {
        compileSdk = Versions.COMPILE_SDK
        defaultConfig {
            minSdk = 21
            versionCode = 1
            versionName = "1.0"
            targetSdk = Versions.TARGET_SDK
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        signingConfigs {
            val path = System.getenv("storyteller_f_sign_path")
            val alias = System.getenv("storyteller_f_sign_alias")
            val storePassword = System.getenv("storyteller_f_sign_store_password")
            val keyPassword = System.getenv("storyteller_f_sign_key_password")
            if (path != null && alias != null && storePassword != null && keyPassword != null) {
                create("release") {
                    keyAlias = alias
                    this.keyPassword = keyPassword
                    storeFile = file(path)
                    this.storePassword = storePassword
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
        val javaVersion = JavaVersion.VERSION_17
        compileOptions {
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }

        kotlinOptionsApp {
            jvmTarget = javaVersion.toString()
            addArgs("-opt-in=kotlin.RequiresOptIn")
        }
        dependenciesInfo {
            includeInBundle = false
            includeInApk = false
        }
    }

    loadBao()
    baseAppDependency()
    redirectKaptOutputToKsp()
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

fun Project.baseLibrary(enableMultiDex: Boolean = false) {
    androidLibrary {
        compileSdk = Versions.COMPILE_SDK

        defaultConfig {
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
        val javaVersion = JavaVersion.VERSION_17
        compileOptions {
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }
        kotlinOptionsLibrary {
            jvmTarget = javaVersion.toString()
            addArgs("-opt-in=kotlin.RequiresOptIn")
        }
    }
}

internal fun Project.androidLibrary(configure: Action<LibraryExtension>): Unit =
    (this as ExtensionAware).extensions.configure("android", configure)

internal fun Project.androidApp(configure: Action<BaseAppModuleExtension>): Unit =
    (this as ExtensionAware).extensions.configure("android", configure)

internal fun BaseAppModuleExtension.kotlinOptionsApp(configure: Action<KotlinJvmOptions>): Unit =
    (this as ExtensionAware).extensions.configure("kotlinOptions", configure)

internal fun LibraryExtension.kotlinOptionsLibrary(configure: Action<KotlinJvmOptions>): Unit =
    (this as ExtensionAware).extensions.configure("kotlinOptions", configure)

internal fun Project.java(configure: Action<org.gradle.api.plugins.JavaPluginExtension>): Unit =
    (this as ExtensionAware).extensions.configure("java", configure)

fun Project.androidComponents(configure: Action<ApplicationAndroidComponentsExtension>): Unit =
    (this as ExtensionAware).extensions.configure("androidComponents", configure)

fun KotlinJvmOptions.addArgs(arg: String) {
    freeCompilerArgs = freeCompilerArgs.plusIfNotExists(arg)
}

fun <T> List<T>.plusIfNotExists(element: T): List<T> {
    if (contains(element)) return this
    val result = ArrayList<T>(size + 1)
    result.addAll(this)
    result.add(element)
    return result
}

fun Project.pureKotlinLanguageLevel() {
    val javaVersion = JavaVersion.VERSION_17
    java {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = javaVersion.toString()
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }
}

/**
 * 必须手动加载ksp 插件
 */
fun Project.constraintCommonUIListVersion(
    version: String,
    group: String = "com.github.storytellerF.common-ui-list"
) {
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
                "file-system",
                "file-system-archive",
                "file-system-ktx",
                "file-system-local",
                "file-system-memory",
                "file-system-remote",
                "file-system-root",
                "slim-ktx",
                "ui-list",
                "ui-list-annotation-common",
                "ui-list-annotation-compiler-ksp",
                "ui-list-annotation-definition",
                "view-holder-compose"
            ).forEach {
                "implementation"("$group:$it:$version")
                "ksp"("$group:$it:$version")
            }
        }
    }

}

/*
* AGP tasks do not get properly wired to the KSP task at the moment.
* As a result, KSP sees `error.NonExistentClass` instead of generated types.
*
* https://github.com/google/dagger/issues/4049
* https://github.com/google/dagger/issues/4051
* https://github.com/google/dagger/issues/4061
* https://github.com/google/dagger/issues/4158
*/
fun Project.redirectKaptOutputToKsp() {
    androidComponents {
        onVariants(selector().all()) { variant ->
            afterEvaluate {
                val variantName = variant.name.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                }
                val ksp = "ksp${variantName}Kotlin"
                val viewBinding = "dataBindingGenBaseClasses$variantName"
                val buildConfig = "generate${variantName}BuildConfig"
                val safeArgs = "generateSafeArgs$variantName"
                val aidl = "compile${variantName}Aidl"

                val kspTask = project.tasks.findByName(ksp)
                        as? org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool<*>
                val viewBindingTask = project.tasks.findByName(viewBinding)
                        as? com.android.build.gradle.internal.tasks.databinding.DataBindingGenBaseClassesTask
                val buildConfigTask = project.tasks.findByName(buildConfig)
                        as? com.android.build.gradle.tasks.GenerateBuildConfig
                val aidlTask = project.tasks.findByName(aidl)
                        as? com.android.build.gradle.tasks.AidlCompile
                val safeArgsTask = project.tasks.findByName(safeArgs)
                        as? androidx.navigation.safeargs.gradle.ArgumentsGenerationTask

                kspTask?.run {
                    viewBindingTask?.let { setSource(it.sourceOutFolder) }
                    buildConfigTask?.let { setSource(it.sourceOutputDir) }
                    aidlTask?.let { setSource(it.sourceOutputDir) }
                    safeArgsTask?.let { setSource(it.outputDir) }
                }
            }
        }
    }
}