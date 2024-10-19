plugins {
    id("java-gradle-plugin")
    `kotlin-dsl`
    `maven-publish`
    id("com.github.gmazzo.buildconfig") version "5.4.0"
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
    implementation(libs.android.plugin.lib)
    implementation(libs.kotlin.plugin.lib)
    implementation(libs.safeArgs.plugin.lib)
}

buildConfig {
    generateAtSync.set(false)
    buildConfigField("COMMON_UI_VERSION", libs.versions.compose.ui)
    buildConfigField("COMMON_MATERIAL_VERSION", libs.versions.compose.material)
    buildConfigField("COMMON_COMPILER_VERSION", libs.versions.compose.compiler)
    buildConfigField("COMMON_KSP_VERSION", libs.versions.ksp)
    val catalog = parseVersionCatalog(rootProject.file("../gradle/libs.versions.toml").absolutePath)

    // Process each section and generate corresponding objects
    catalog["versions"]?.map { (key, version) ->
        buildConfigField("""`version $key`""", version["version"] as String)
    }

    catalog["plugins"]?.map { (key, plugin) ->
        buildConfigField("""`plugin $key`""", "${plugin["id"]}:${plugin["version"]}")
    }

    catalog["bundles"]?.map { (key, libraries) ->
        @Suppress("UNCHECKED_CAST") val libList = (libraries["libraries"] as List<String>).joinToString(", ") { "\"$it\"" }
        buildConfigField("""`bundle $key`""", "listOf($libList)")
    }

    catalog["libraries"]?.map { (key, library) ->
        val version = library["version.ref"]?.let { ref -> catalog["versions"]!![ref]!!["version"] }
            ?: library["version"]
        buildConfigField("""`library $key`""", "${library["module"]}:$version")
    }
}

fun parseVersionCatalog(filePath: String): Map<String, Map<String, Map<String, Any>>> {
    val catalog = mutableMapOf<String, MutableMap<String, Map<String, Any>>>()
    val file = File(filePath)

    var currentSection: String? = null
    file.forEachLine { line ->
        if (line.startsWith("[")) currentSection = line.trim().removeSurrounding("[", "]")
        else {
            val current = currentSection
            if (current != null && line.contains("=")) {
                val (key, value) = line.split("=", limit = 2).map { it.trim() }

                when (current) {
                    "versions" -> {
                        catalog.getOrPut(current) { mutableMapOf() }[key] =
                            mapOf("version" to value.removeSurrounding("\""))
                    }

                    "plugins" -> {
                        val pluginData = value.removeSurrounding("{", "}").split(",").associate {
                            val (k, v) = it.split("=").map { it.trim() }
                            k to v.removeSurrounding("\"")
                        }
                        catalog.getOrPut(current) { mutableMapOf() }[key] = pluginData
                    }

                    "bundles" -> {
                        val libraries = value.removeSurrounding("[", "]").split(",")
                            .map { it.trim().removeSurrounding("\"") }
                        catalog.getOrPut(current) { mutableMapOf() }[key] =
                            mapOf("libraries" to libraries)
                    }

                    "libraries" -> {
                        val moduleAndVersion =
                            value.removeSurrounding("{", "}").split(",").associate {
                                val (k, v) = it.split("=").map { it.trim() }
                                k to v.removeSurrounding("\"")
                            }
                        catalog.getOrPut(current) { mutableMapOf() }[key] = moduleAndVersion
                    }
                }
            }
        }
    }
    return catalog
}
