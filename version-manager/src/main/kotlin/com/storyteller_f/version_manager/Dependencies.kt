@file:Suppress("unused")

package com.storyteller_f.version_manager

import org.gradle.api.Project
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.dependencies

/**
 * 手动调用[constraintCommonUIListVersion] 是必须的
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
 * 手动调用[constraintCommonUIListVersion] 是必须的
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
 * 手动调用[constraintCommonUIListVersion] 是必须的
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
 * 手动调用[constraintCommonUIListVersion] 是必须的
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

/**
 * 默认加载ksp 插件
 */
fun Project.baseAppDependency() {
    loadPlugin("com.google.devtools.ksp")
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

        "ksp"(BuildConfig.`library room-compiler`)

        "debugImplementation"(BuildConfig.`library leak-canary`)
        "implementation"(BuildConfig.`library multi-dex`)
    }

}

fun DependencyHandlerScope.composeDependency() {
    "implementation"(BuildConfig.`library compos-material`)
    "implementation"(BuildConfig.`library compose-ui-tooling`)
}

fun DependencyHandlerScope.networkDependency() {
    "implementation"(BuildConfig.`library retrofit`)
    "implementation"(BuildConfig.`library retrofit-mock`)
    "implementation"(BuildConfig.`library log-receptor`)
}

fun DependencyHandlerScope.navigationDependency() {
    "implementation"(BuildConfig.`library navigation-fragment-ktx`)
    "implementation"(BuildConfig.`library nav-ui-ktx`)
}

fun DependencyHandlerScope.coroutineDependency() {
    "implementation"(BuildConfig.`library coroutines`)
    "implementation"(BuildConfig.`library coroutines-android`)
}

fun DependencyHandlerScope.unitTestDependency() {
    "testImplementation"(BuildConfig.`library junit`)
    "androidTestImplementation"(BuildConfig.`library android-junit`)
    "androidTestImplementation"(BuildConfig.`library android-espresso`)
}

fun Project.workerDependency() {
    dependencies {
        "implementation"(BuildConfig.`library work-runtime`)
        "androidTestImplementation"(BuildConfig.`library work-test`)
        "implementation"(BuildConfig.`library work-multiprocess`)
    }

}

fun DependencyHandlerScope.commonAppDependency() {
    "implementation"(BuildConfig.`library core`)
    "implementation"(BuildConfig.`library appcompat`)
    "implementation"(BuildConfig.`library material`)

    "implementation"(BuildConfig.`library fragment-ktx`)
    "implementation"(BuildConfig.`library activity-ktx`)
}

fun DependencyHandlerScope.commonLibraryDependency() {
    "implementation"(BuildConfig.`library core`)
    "implementation"(BuildConfig.`library appcompat`)
    "implementation"(BuildConfig.`library material`)
}