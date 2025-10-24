import com.storyteller_f.version_manager.*
import com.storyteller_f.version_manager.pureKotlin
import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("common-publish")
    id("com.storyteller_f.version_manager")
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
 