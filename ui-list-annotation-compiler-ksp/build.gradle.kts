import com.storyteller_f.version_manager.pureKotlin
import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    id("com.storyteller_f.version_manager")
    id("common-publish")
}
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

dependencies {
    dependencies {
        implementation(project(":slim-ktx"))
    }
    dependencies {
        implementation(project(":ui-list-annotation-definition"))
    }
    dependencies {
        implementation(project(":ui-list-annotation-common"))
    }
    implementation(libs.symbol.processing.api)
}