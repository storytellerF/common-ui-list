plugins {
    id("java-gradle-plugin")
    `kotlin-dsl`
    `maven-publish`
    id("com.github.gmazzo.buildconfig") version "5.3.5"
}

val env: MutableMap<String, String> = System.getenv()
group = group.takeIf { it.toString().contains(".") } ?: env["GROUP"] ?: "com.storyteller_f"
version = version.takeIf { it != "unspecified" } ?: env["VERSION"] ?: "0.0.1-local"

gradlePlugin {
    plugins {
        register("version-manager") {
            // 插件ID
            id = "com.storyteller_f.version_manager"
            // 插件的实现类
            implementationClass = "com.storyteller_f.version_manager.VersionManager"
        }
    }
}

dependencies {
    implementation(cul.android.plugin.lib)
    implementation(cul.kotlin.plugin.lib)
    implementation(cul.safeArgs.plugin.lib)
}

buildConfig {
    generateAtSync.set(false)
    buildConfigField("COMMON_UI_VERSION", cul.versions.compose.ui)
    buildConfigField("COMMON_MATERIAL_VERSION", cul.versions.compose.material)
    buildConfigField("COMMON_COMPILER_VERSION", cul.versions.compose.compiler)
}