import com.storyteller_f.version_manager.Versions
import com.storyteller_f.version_manager.baseLibrary
import com.storyteller_f.version_manager.implModule
import com.storyteller_f.version_manager.setupExtFunc
import com.storyteller_f.version_manager.unitTestDependency

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.storyteller_f.version_manager")
    id("common-publish")
}

android {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xcontext-receivers")
    }
}
baseLibrary(namespaceString = "com.storyteller_f.common_pr")

setupExtFunc()
dependencies {
    implModule(":ext-func-definition")
    implModule(":common-ktx")
    implModule(":common-vm-ktx")
    implModule(":common-ui")
    unitTestDependency()
    // https://mvnrepository.com/artifact/androidx.navigation/navigation-common-ktx
    implementation(libs.navigation.common.ktx)
    implementation(libs.navigation.fragment.ktx)
}
