import com.storyteller_f.version_manager.Versions
import com.storyteller_f.version_manager.baseApp
import com.storyteller_f.version_manager.networkDependency
import com.storyteller_f.version_manager.setupDataBinding
import com.storyteller_f.version_manager.setupGeneric
import com.storyteller_f.version_manager.setupPreviewFeature
import java.util.Locale

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("com.storyteller_f.version_manager")
    id("kotlin-kapt")
    id("com.starter.easylauncher") version ("6.2.0")
    id("com.google.devtools.ksp")
}

android {

    defaultConfig {
        applicationId = "com.storyteller_f.common_ui_list_structure"
    }

    namespace = "com.storyteller_f.common_ui_list_structure"
}

kapt {
//    correctErrorTypes = true
    useBuildCache = true
}

dependencies {
    networkDependency()
    implementation("com.squareup.retrofit2:converter-gson:${Versions.RETROFIT}")
}
baseApp()
setupGeneric()
setupDataBinding()
setupPreviewFeature()
/*
 * AGP tasks do not get properly wired to the KSP task at the moment.
 * As a result, KSP sees `error.NonExistentClass` instead of generated types.
 *
 * https://github.com/google/dagger/issues/4049
 * https://github.com/google/dagger/issues/4051
 * https://github.com/google/dagger/issues/4061
 * https://github.com/google/dagger/issues/4158
 */
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