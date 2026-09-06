package com.storyteller_f.ui_list.event

import android.app.Activity
import android.content.ContextWrapper
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.findFragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * 查找继承指定接口或抽象类的Fragment
 */
inline fun <reified T> View.findFragmentOrNull(): T? {
    var fragment: Fragment? = try {
        findFragment(this)
    } catch (_: Exception) {
        null
    }
    while (true) {
        when (fragment) {
            null -> return null
            is T -> return fragment
            else -> fragment = fragment.parentFragment
        }
    }
}

inline fun <reified T> Any.doWhen(block: (T) -> Unit) {
    if (this is T) {
        block(this as T)
    }
}

fun View.findActivityOrNull(): Activity? {
    var context = context
    while (true) {
        when (context) {
            !is Activity -> {
                if (context is ContextWrapper) {
                    context = context.baseContext
                } else {
                    return null
                }
            }

            else -> {
                return context
            }
        }
    }
}
