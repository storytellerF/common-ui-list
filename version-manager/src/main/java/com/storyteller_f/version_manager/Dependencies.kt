@file:Suppress("unused")

package com.storyteller_f.version_manager

import org.gradle.api.Project
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.dependencies

object Versions {
    const val KOTLIN = "1.8.21"
    const val AGP = "8.2.2"
    const val KSP = "1.9.21-1.0.15"
    const val COMPILE_SDK = 34
    const val TARGET_SDK = 34
    const val APPCOMPAT = "1.6.1"
    const val CORE = "1.12.0"
    const val RECYCLERVIEW = "1.3.2"
    const val CONSTRAINTLAYOUT = "2.1.4"
    const val MATERIAL = "1.11.0"
    const val ACTIVITY_KTX = "1.8.2"
    const val FRAGMENT_KTX = "1.6.2"
    const val LIFECYCLE = "2.7.0"
    const val ROOM = "2.6.1"
    const val PAGING = "3.2.1"
    const val RETROFIT = "2.9.0"
    const val OKHTTP_LOGGING_INTERCEPTOR = "5.0.0-alpha.6"
    const val COROUTINES = "1.8.0"
    const val COMPOSE_COMPILER = "1.5.7"
    const val COMPOSE = "1.4.0"
    const val COMPOSE_UI = "1.5.4"
    const val COMPOSE_MATERIAL = "1.4.0-beta02"
    const val NAV = "2.7.7"
    const val WORK = "2.7.1"
    const val LEAK_CANARY = "2.9.1"
    const val DATA_BINDING_COMPILER = "8.2.2"
    const val JUNIT = "4.13.2"
    const val TEST_JUNIT = "1.1.3"
    const val TEST_ESPRESSO = "3.4.0"
    const val MULTI_DEX = "2.0.1"
    const val BAO = "2.4.0"
    const val NAVIGATION = "2.7.7"
    const val SWIPE_REFRESH = "1.1.0"

    const val JITPACK_RELEASE_GROUP = "com.github.storytellerF.common-ui-list"
}

fun Project.implModule(moduleName: String) {
    dependencies {
        val module = findProject(moduleName)
        if (module != null) {
            "implementation"(module)
        } else {
            "implementation"("${Versions.JITPACK_RELEASE_GROUP}$moduleName")
        }
    }

}

fun Project.androidTestImplModule(moduleName: String) {
    dependencies {
        val module = findProject(moduleName)
        if (module != null) {
            "androidTestImplementation"(module)
        } else {
            "androidTestImplementation"("${Versions.JITPACK_RELEASE_GROUP}$moduleName")
        }
    }

}

fun Project.apiModule(moduleName: String) {
    dependencies {
        val module = findProject(moduleName)
        if (module != null) {
            "api"(module)
        } else {
            "api"("${Versions.JITPACK_RELEASE_GROUP}$moduleName")
        }
    }

}

fun Project.kaptModule(moduleName: String) {
    dependencies {
        val module = findProject(moduleName)
        if (module != null) {
            "kapt"(module)
        } else {
            "kapt"("${Versions.JITPACK_RELEASE_GROUP}$moduleName")
        }
    }

}

fun Project.kspModule(moduleName: String) {
    dependencies {
        val module = findProject(moduleName)
        if (module != null) {
            "ksp"(module)
        } else {
            "ksp"("${Versions.JITPACK_RELEASE_GROUP}$moduleName")
        }
    }

}

fun Project.baseAppDependency() {
    implModule(":slim-ktx")
    implModule(":common-ktx")
    implModule(":compat-ktx")
    implModule(":common-ui")
    implModule(":ui-list")
    implModule(":ui-list-annotation-definition")
    kspModule(":ui-list-annotation-compiler-ksp")
    implModule(":composite-definition")
    kspModule(":composite-compiler-ksp")

    dependencies {
        commonAndroidDependency()

        "ksp"("androidx.room:room-compiler:${Versions.ROOM}")

        "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.COROUTINES}")
        "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.COROUTINES}")

        "debugImplementation"("com.squareup.leakcanary:leakcanary-android:${Versions.LEAK_CANARY}")
        "implementation"("androidx.multidex:multidex:${Versions.MULTI_DEX}")
    }

}

fun DependencyHandlerScope.composeDependency() {
    "implementation"("androidx.compose.material:material:${Versions.COMPOSE_MATERIAL}")
    "implementation"("androidx.compose.ui:ui-tooling:${Versions.COMPOSE_UI}")
}

fun DependencyHandlerScope.networkDependency() {
    "implementation"("com.squareup.retrofit2:retrofit:${Versions.RETROFIT}")
    "implementation"("com.squareup.retrofit2:retrofit-mock:${Versions.RETROFIT}")
    "implementation"("com.squareup.okhttp3:logging-interceptor:${Versions.OKHTTP_LOGGING_INTERCEPTOR}")
}

fun DependencyHandlerScope.navigationDependency() {
    "implementation"("androidx.navigation:navigation-fragment-ktx:${Versions.NAV}")
    "implementation"("androidx.navigation:navigation-ui-ktx:${Versions.NAV}")
}

fun DependencyHandlerScope.unitTestDependency() {
    "testImplementation"("junit:junit:${Versions.JUNIT}")
    "androidTestImplementation"("androidx.test.ext:junit:${Versions.TEST_JUNIT}")
    "androidTestImplementation"("androidx.test.espresso:espresso-core:${Versions.TEST_ESPRESSO}")
}

/**
 * 需要kapt 插件
 */
fun DependencyHandlerScope.dataBindingDependency() {
    "kapt"("androidx.databinding:databinding-compiler-common:${Versions.DATA_BINDING_COMPILER}")
}

fun Project.workerDependency() {
    dependencies {
        "implementation"("androidx.work:work-runtime-ktx:${Versions.WORK}")
        "androidTestImplementation"("androidx.work:work-testing:${Versions.WORK}")
        "implementation"("androidx.work:work-multiprocess:${Versions.WORK}")
    }

}

fun Project.fileSystemDependency() {
    implModule(":file-system-ktx")
}

fun DependencyHandlerScope.commonAndroidDependency() {
    "implementation"("androidx.core:core-ktx:${Versions.CORE}")
    "implementation"("androidx.appcompat:appcompat:${Versions.APPCOMPAT}")
    "implementation"("com.google.android.material:material:${Versions.MATERIAL}")

    "implementation"("androidx.fragment:fragment-ktx:${Versions.FRAGMENT_KTX}")
    "implementation"("androidx.activity:activity-ktx:${Versions.ACTIVITY_KTX}")
}