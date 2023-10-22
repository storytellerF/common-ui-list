import com.storyteller_f.version_manager.Versions
import com.storyteller_f.version_manager.baseLibrary
import com.storyteller_f.version_manager.unitTestDependency
import com.storyteller_f.version_manager.commonAndroidDependency
import com.storyteller_f.version_manager.implModule

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.storyteller_f.version_manager")
    id("common-publish")
}

android {
    namespace = "com.storyteller_f.file_system_remote"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
    }
}
baseLibrary()

dependencies {

    commonAndroidDependency()
    implementation("androidx.room:room-common:${Versions.ROOM}")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implModule(":file-system")

    // https://mvnrepository.com/artifact/commons-net/commons-net
    implementation("commons-net:commons-net:3.9.0")
    // https://mvnrepository.com/artifact/org.mockftpserver/MockFtpServer
    testImplementation("org.mockftpserver:MockFtpServer:3.1.0")
    // https://mvnrepository.com/artifact/com.hierynomus/smbj
    implementation("com.hierynomus:smbj:0.11.5")
    // https://mvnrepository.com/artifact/com.hierynomus/sshj
    implementation("com.hierynomus:sshj:0.35.0")

    unitTestDependency()
}