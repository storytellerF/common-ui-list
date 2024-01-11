@file:Suppress("unused", "UnstableApiUsage", "DEPRECATION")

package com.storyteller_f.version_manager

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
 * 需要手动添加kapt 插件
 */
fun Project.setupAppBase() {
    dependencies {
        baseAppDependency()
    }
}

fun Project.setupGeneric() {
    setupAppBase()
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
        dipToPxDependency()
    }
}

fun Project.loadPlugin(id: String) {
    if (!plugins.hasPlugin(id)) plugins.apply(id)
}

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
                resValue("string", "leak_canary_display_activity_label", defaultConfig.applicationId?.substringAfterLast(".") ?: "Leaks")
            }
            release {
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    dependencies {
        val baoModule = findProject(":bao:startup")
        if (baoModule != null)
            "implementation"(baoModule)
        else
            "implementation"("com.github.storytellerF.Bao:startup:${Versions.BAO}")
    }
}

fun Project.baseLibrary() {
    androidLibrary {
        compileSdk = Versions.COMPILE_SDK

        defaultConfig {
            targetSdk = Versions.TARGET_SDK
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            consumerProguardFiles("consumer-rules.pro")
        }

        buildTypes {
            release {
                isMinifyEnabled = false
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

fun Project.androidLibrary(configure: Action<LibraryExtension>): Unit =
    (this as ExtensionAware).extensions.configure("android", configure)

fun Project.androidApp(configure: Action<BaseAppModuleExtension>): Unit =
    (this as ExtensionAware).extensions.configure("android", configure)

fun Project.kotlin(configure: Action<KotlinAndroidProjectExtension>): Unit =
    (this as ExtensionAware).extensions.configure("kotlin", configure)

val NamedDomainObjectContainer<KotlinSourceSet>.main: NamedDomainObjectProvider<KotlinSourceSet>
    get() = named<KotlinSourceSet>("main")

val NamedDomainObjectContainer<KotlinSourceSet>.test: NamedDomainObjectProvider<KotlinSourceSet>
    get() = named<KotlinSourceSet>("test")

fun BaseAppModuleExtension.kotlinOptionsApp(configure: Action<KotlinJvmOptions>): Unit =
    (this as ExtensionAware).extensions.configure("kotlinOptions", configure)

fun LibraryExtension.kotlinOptionsLibrary(configure: Action<KotlinJvmOptions>): Unit =
    (this as ExtensionAware).extensions.configure("kotlinOptions", configure)

fun Project.java(configure: Action<org.gradle.api.plugins.JavaPluginExtension>): Unit =
    (this as ExtensionAware).extensions.configure("java", configure)

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
                "composite-compiler",
                "composite-definition",
                "ext-func-compiler",
                "ext-func-definition",
                "file-system",
                "file-system-ktx",
                "file-system-remote",
                "file-system-root",
                "slim-ktx",
                "ui-list",
                "ui-list-annotation-common",
                "ui-list-annotation-compiler",
                "ui-list-annotation-compiler-ksp",
                "ui-list-annotation-definition",
                "view-holder-compose"
            ).forEach {
                "implementation"("$group:$it:$version")
                "ksp"("$group:$it:$version")
                "kapt"("$group:$it:$version")
            }
        }
    }

}