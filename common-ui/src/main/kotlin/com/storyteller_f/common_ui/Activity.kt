package com.storyteller_f.common_ui

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsControllerCompat

fun ComponentActivity.supportNavigatorBarImmersive() {
    enableEdgeToEdge()
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = false
}
