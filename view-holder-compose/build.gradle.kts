import com.storyteller_f.version_manager.*

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.storyteller_f.version_manager")
    id("common-publish")
}

baseLibrary(minSdkInt = 21, namespaceString = "com.storyteller_f.view_holder_compose")

dependencies {
    unitTestDependency()
    api("androidx.compose.ui:ui:${Versions.COMPOSE_UI}")

    implModule(":ui-list")
}