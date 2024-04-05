@file:Suppress("unused")

package com.storyteller_f.version_manager

import org.gradle.api.Project
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.dependencies

/**
 * 手动调用constraintCommonUIListVersion 是必须的
 */
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

/**
 * 手动调用constraintCommonUIListVersion 是必须的
 */
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

/**
 * 手动调用constraintCommonUIListVersion 是必须的
 */
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

/**
 * 手动调用constraintCommonUIListVersion 是必须的
 */
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
        commonAppDependency()

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

fun DependencyHandlerScope.commonAppDependency() {
    "implementation"("androidx.core:core-ktx:${Versions.CORE}")
    "implementation"("androidx.appcompat:appcompat:${Versions.APPCOMPAT}")
    "implementation"("com.google.android.material:material:${Versions.MATERIAL}")

    "implementation"("androidx.fragment:fragment-ktx:${Versions.FRAGMENT_KTX}")
    "implementation"("androidx.activity:activity-ktx:${Versions.ACTIVITY_KTX}")
}

fun DependencyHandlerScope.commonLibraryDependency() {
    "implementation"("androidx.core:core-ktx:${Versions.CORE}")
    "implementation"("androidx.appcompat:appcompat:${Versions.APPCOMPAT}")
    "implementation"("com.google.android.material:material:${Versions.MATERIAL}")
}