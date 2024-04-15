plugins {
    id("java-gradle-plugin")
    `kotlin-dsl`
    `maven-publish`
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
    val androidVersion = "8.3.2"
    val kotlinVersion = "1.9.22"
    val navVersion = "2.7.7"
    implementation("com.android.tools.build:gradle:$androidVersion")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("androidx.navigation:navigation-safe-args-gradle-plugin:$navVersion")
}
