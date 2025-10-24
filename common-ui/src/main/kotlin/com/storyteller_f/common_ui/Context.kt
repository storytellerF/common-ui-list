package com.storyteller_f.common_ui

import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner

fun <T> LifecycleOwner.context(function: Context.() -> T) = (
    when (this) {
        is ComponentActivity -> this
        is Fragment -> requireContext()
        else -> throw UnsupportedOperationException("context is null")
    }
    ).run(function)

val LifecycleOwner.ctx
    get() = when (this) {
        is ComponentActivity -> this
        is Fragment -> requireContext()
        else -> throw UnsupportedOperationException("unknown type $this")
    }

val Context.isNightMode
    get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
