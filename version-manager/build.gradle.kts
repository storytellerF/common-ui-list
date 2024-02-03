plugins {
    id("java-gradle-plugin")
    `kotlin-dsl`
    `maven-publish`
}

group = "com.storyteller_f"
version = "0.0.2"
publishing {

}
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
    val androidVersion = "8.2.2"
    val kotlinVersion = "1.9.20"
    val navVersion = "2.7.6"
    implementation("com.android.tools.build:gradle:$androidVersion")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("androidx.navigation:navigation-safe-args-gradle-plugin:$navVersion")
}
