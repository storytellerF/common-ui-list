plugins {
    `maven-publish`
}

group = "com.storyteller_f"
version = System.getenv().let {
    if (it["JITPACK"] == null) "0.0.1-local" else it["VERSION"]!!
}

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