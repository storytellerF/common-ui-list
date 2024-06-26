package com.storyteller_f.common_ui

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
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

fun ComponentActivity.supportNavigatorBarImmersive(view: View) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    }
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = !isNightMode
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isNightMode
    /**
     * 如果提供一个透明色，在低版本中会自动添加一个颜色
     */
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.navigationBarColor = Color.parseColor("#01000000")
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
        view.setOnApplyWindowInsetsListener { v, insets ->
            val top = WindowInsetsCompat.toWindowInsetsCompat(insets, v).getInsets(WindowInsetsCompat.Type.statusBars())
            v.updatePadding(top = top.top)
            insets
        }
    }
}
