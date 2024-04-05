plugins {
    id("java-gradle-plugin")
    `kotlin-dsl`
    `maven-publish`
}

group = "com.storyteller_f"
version = System.getenv().let {
    if (it["JITPACK"] == null) "0.0.1-local" else it["VERSION"]!!
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
    val androidVersion = "8.3.1"
    val kotlinVersion = "1.9.22"
    val navVersion = "2.7.7"
    implementation("com.android.tools.build:gradle:$androidVersion")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("androidx.navigation:navigation-safe-args-gradle-plugin:$navVersion")
}
