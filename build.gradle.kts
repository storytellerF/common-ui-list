@file:Suppress("UnstableApiUsage")

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        val smlFolder: String by project
        val navVersion = "2.7.4"
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$navVersion")
        classpath("app.cash.licensee:licensee-gradle-plugin:1.6.0")
    }
}
plugins {
    val androidVersion = "8.3.0-alpha11"
    val kotlinVersion = "1.9.10"
    val kspVersion = "1.9.10-1.0.13"
    id("com.android.application") version androidVersion apply false
    id("com.android.library") version androidVersion
    id("org.jetbrains.kotlin.android") version kotlinVersion apply false
    id("org.jetbrains.kotlin.jvm") version kotlinVersion apply false
    id("com.google.devtools.ksp") version kspVersion apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.1"
    id("org.jetbrains.kotlinx.kover") version "0.7.4"
}

// tasks.register("clean", Delete::class) {
//     delete(rootProject.layout.buildDirectory)
// }

android {
    compileSdk = 33
    namespace = "com.root"
}

val deprecationCheckModule = listOf("")
subprojects {
    if (deprecationCheckModule.contains(name)) {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions {
                freeCompilerArgs =
                    freeCompilerArgs + listOf("-Xlint:deprecation", "-Xlint:unchecked")
            }
        }
        tasks.withType<JavaCompile> {
            options.compilerArgs =
                options.compilerArgs + listOf("-Xlint:deprecation", "-Xlint:unchecked")
        }
    }
}

val detektReportMergeSarif by tasks.registering(ReportMergeTask::class) {
    output = layout.buildDirectory.file("reports/detekt/merge.sarif")
}
val androidModules = listOf(
    "common-ktx",
    "common-pr",
    "common-ui",
    "common-vm-ktx",
    "compat-ktx",
    "file-system",
    "file-system-ktx",
    "file-system-remote",
    "file-system-root",
    "ui-list",
    "view-holder-compose"
)
val jvmModules = listOf("composite-compiler",
    "composite-definition",
    "ext-func-compiler",
    "ext-func-definition",
    "multi-core",
    "slim-ktx",
    "ui-list-annotation-common",
    "ui-list-annotation-compiler",
    "ui-list-annotation-compiler-ksp",
    "ui-list-annotation-definition",)
subprojects {

    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jetbrains.kotlinx.kover")
    if (androidModules.contains(name)) {
        apply(plugin = "com.android.library")
    }
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
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.1")
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:1.23.1")
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-ruleauthors:1.23.1")
        if (name == "app") {
            val action = { it: String ->
                kover(project(":$it"))
                Unit
            }
            androidModules.forEach(action)
            jvmModules.forEach(action)
        }
        if (androidModules.contains(name)) {
            testImplementation("org.robolectric:robolectric:4.10.3")
        }
    }
    koverReport {
        if (androidModules.contains(name)) {
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
        input.from(tasks.withType<Detekt>().map { it.sarifReportFile })
    }
    tasks.withType<DetektCreateBaselineTask>().configureEach {
        jvmTarget = "1.8"
    }

    tasks.withType<Test> {
        maxHeapSize = "8g"
        systemProperties["junit.jupiter.execution.parallel.enabled"] = true
        systemProperties["junit.jupiter.execution.parallel.mode.default"] = "concurrent"
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
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
}