import com.storyteller_f.version_manager.Versions
import com.storyteller_f.version_manager.baseLibrary
import com.storyteller_f.version_manager.commonAndroidDependency
import com.storyteller_f.version_manager.implModule
import com.storyteller_f.version_manager.unitTestDependency

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.storyteller_f.version_manager")
    id("common-publish")
}

android {
    namespace = "com.storyteller_f.file_system_remote"

    defaultConfig {
        minSdk = 21
    }
}
baseLibrary()

dependencies {

    implModule(":common-ktx")
    implModule(":slim-ktx")
    implModule(":file-system")
    unitTestDependency()
    commonAndroidDependency()
    implementation("androidx.room:room-common:${Versions.ROOM}")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    // https://mvnrepository.com/artifact/commons-net/commons-net
    implementation("commons-net:commons-net:3.10.0")
    // https://mvnrepository.com/artifact/org.mockftpserver/MockFtpServer
    testImplementation("org.mockftpserver:MockFtpServer:3.1.0")
    // https://mvnrepository.com/artifact/com.hierynomus/smbj
    implementation("com.hierynomus:smbj:0.13.0")
    // https://mvnrepository.com/artifact/com.hierynomus/sshj
    implementation("com.hierynomus:sshj:0.38.0")
    implementation("com.madgag.spongycastle:prov:1.58.0.0")

    loadSardine()


    val mockkVersion = "1.13.8"
    testImplementation("io.mockk:mockk-android:${mockkVersion}")
    testImplementation("io.mockk:mockk-agent:${mockkVersion}")
    testImplementation("com.google.jimfs:jimfs:1.3.0")
    testImplementation("com.github.tony19:logback-android:3.0.0")
}

fun DependencyHandlerScope.loadSardine() {
    val project = findProject(":sardine-android")
    if (project != null) {
        implementation(project)
    } else {
//        implementation("com.github.storytellerF:sardine-android:7da4aa36e1")
        implementation("com.github.thegrizzlylabs:sardine-android:0.9")
    }
}