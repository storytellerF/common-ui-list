import com.storyteller_f.version_manager.*

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.storyteller_f.version_manager")
    id("common-publish")
}

android {
    namespace = "com.storyteller_f.common_vm_ktx"
}
baseLibrary()
setupExtFunc()
dependencies {
    implModule(":ext-func-definition")
    implementation("androidx.appcompat:appcompat:${Versions.APPCOMPAT}")
    unitTestDependency()

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.LIFECYCLE}")
}