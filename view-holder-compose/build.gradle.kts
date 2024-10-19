import com.storyteller_f.version_manager.*

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.storyteller_f.version_manager")
    id("common-publish")
}

baseLibrary(namespaceString = "com.storyteller_f.view_holder_compose")

dependencies {
    unitTestDependency()
    api(libs.ui)

    implModule(":ui-list")
}