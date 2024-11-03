@file:Suppress("UnstableApiUsage")

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath(libs.safeArgs.plugin.lib)
    }
}
plugins {
    alias(libs.plugins.android) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.easylauncher) apply false
}
tasks.withType<Test> {
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(
            TestLogEvent.STARTED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED,
            TestLogEvent.PASSED
        )
        showStandardStreams = true
    }
}
setupDeprecationCheck(listOf(""))
setupDetekt()
setupKover(
    "app",
    listOf(
        "common-ktx",
        "common-pr",
        "common-ui",
        "common-vm-ktx",
        "compat-ktx",
        "ui-list",
        "view-holder-compose"
    ), listOf(
        "composite-compiler-ksp",
        "composite-definition",
        "ext-func-compiler",
        "ext-func-definition",
        "slim-ktx",
        "ui-list-annotation-common",
        "ui-list-annotation-compiler-ksp",
        "ui-list-annotation-definition",
    ), listOf(
        "ui-list-annotation-common",
        "ui-list-annotation-compiler-ksp",
        "ext-func-definition",
        "ext-func-compiler",
        "composite-definition",
        "composite-compiler-ksp",
        "app"
    )
)

fun Project.setupDetekt() {
    val detektReportMergeSarif by tasks.registering(ReportMergeTask::class) {
        output = layout.buildDirectory.file("reports/detekt/merge.sarif")
    }
    subprojects {
        apply(plugin = "io.gitlab.arturbosch.detekt")
        detekt {
            source.setFrom(
                io.gitlab.arturbosch.detekt.extensions.DetektExtension.DEFAULT_SRC_DIR_JAVA,
                io.gitlab.arturbosch.detekt.extensions.DetektExtension.DEFAULT_TEST_SRC_DIR_JAVA,
                io.gitlab.arturbosch.detekt.extensions.DetektExtension.DEFAULT_SRC_DIR_KOTLIN,
                io.gitlab.arturbosch.detekt.extensions.DetektExtension.DEFAULT_TEST_SRC_DIR_KOTLIN,
            )
            buildUponDefaultConfig = true
            autoCorrect = true
            config.setFrom("$rootDir/config/detekt/detekt.yml")
            baseline = file("$rootDir/config/detekt/baseline.xml")
        }
        dependencies {
            detektPlugins(rootProject.libs.detekt.formatting)
            detektPlugins(rootProject.libs.detekt.rules.libraries)
            detektPlugins(rootProject.libs.detekt.rules.ruleauthors)
        }
        tasks.withType<Detekt>().configureEach {
            jvmTarget = "1.8"
            reports {
                xml.required = true
                html.required = true
                txt.required = true
                sarif.required = true
                md.required = true
            }
            basePath = rootDir.absolutePath
            finalizedBy(detektReportMergeSarif)
        }
        detektReportMergeSarif {
            input.from(
                tasks.withType<Detekt>().map { it.sarifReportFile })
        }
        tasks.withType<DetektCreateBaselineTask>().configureEach {
            jvmTarget = "1.8"
        }
    }
}

fun Project.setupKover(
    mainModule: String,
    androidLibModules: List<String>,
    jvmLibModules: List<String>,
    excludeModules: List<String>,
) {
    subprojects {
        if (!excludeModules.contains(name)) {
            apply(plugin = "org.jetbrains.kotlinx.kover")
            if (androidLibModules.contains(name)) {
                apply(plugin = "com.android.library")
            }

            dependencies {
                if (name == mainModule) {
                    val action = { it: String ->
                        kover(project(":$it"))
                        Unit
                    }
                    androidLibModules.forEach(action)
                    jvmLibModules.forEach(action)
                }
                if (androidLibModules.contains(name)) {
                    val robolectricVersion = "4.11.1"
                    "testImplementation"("org.robolectric:robolectric:$robolectricVersion")
                }
            }
            koverReport {
                if (androidLibModules.contains(name)) {
                    defaults {
                        mergeWith("release")
                    }
                }
                // filters for all report types of all build variants
                filters {
                    excludes {
                        classes(
                            "*Fragment",
                            "*Fragment\$*",
                            "*Activity",
                            "*Activity\$*",
                            "*.databinding.*",
                            "*.BuildConfig"
                        )
                    }
                }
            }
        }

    }
}

fun Project.setupDeprecationCheck(deprecationCheckModules: List<String>) {
    subprojects {
        if (deprecationCheckModules.contains(name)) {
            tasks.withType<KotlinCompile> {
                compilerOptions {
                    freeCompilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
                }
            }
            tasks.withType<JavaCompile> {
                options.compilerArgs =
                    options.compilerArgs + listOf("-Xlint:deprecation", "-Xlint:unchecked")
            }
        }
    }
}