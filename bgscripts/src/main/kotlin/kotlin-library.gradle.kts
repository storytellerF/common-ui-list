import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val javaVersion = JavaVersion.VERSION_21

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
        optIn.add("kotlin.RequiresOptIn")
    }
}

fun Project.kotlin(configure: Action<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>) {
    (this as ExtensionAware).extensions.configure("kotlin", configure)
}