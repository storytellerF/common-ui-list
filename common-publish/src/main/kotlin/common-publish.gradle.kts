plugins {
    `maven-publish`
}

group = "com.storyteller_f"
version = "0.0.1"

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                val component = components.find {
                    it.name == "java" || it.name == "release"
                }
                from(component)
            }
        }
    }
}