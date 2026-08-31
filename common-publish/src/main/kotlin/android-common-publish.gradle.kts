plugins {
    id("com.vanniktech.maven.publish")
}

println("group: $group, version: $version")

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
    coordinates(artifactId = project.name)

    pom {
        name.set(project.name)
        description.set("A module from common-ui-list, a collection of reusable Android UI and Kotlin utilities")
        url.set("https://github.com/storytellerF/common-ui-list")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("storytellerF")
                name.set("storytellerF")
                url.set("https://github.com/storytellerF")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/storytellerF/common-ui-list.git")
            developerConnection.set("scm:git:ssh://github.com/storytellerF/common-ui-list.git")
            url.set("https://github.com/storytellerF/common-ui-list")
        }
    }
}
