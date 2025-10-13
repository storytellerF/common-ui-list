package com.storyteller_f.common_ui

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewbinding.ViewBinding

abstract class CommonActivity : AppCompatActivity(), Registry {

    override fun onStart() {
        super.onStart()
        // 主要用于旋转屏幕
        observeResponse()
    }
}

abstract class SimpleActivity<T : ViewBinding>(
    val viewBindingFactory: (LayoutInflater) -> T
) : AppCompatActivity(), Registry {
    private var _binding: T? = null
    val binding: T get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindingLocal = viewBindingFactory(layoutInflater)
        setContentView(bindingLocal.root)
        _binding = bindingLocal
        onBindViewEvent(binding)
    }

    abstract fun onBindViewEvent(binding: T)
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}

fun ComponentActivity.supportNavigatorBarImmersive() {
    enableEdgeToEdge()
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = false
}
