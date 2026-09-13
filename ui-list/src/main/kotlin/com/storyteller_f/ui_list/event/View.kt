package com.storyteller_f.ui_list.event

import android.app.Activity
import android.content.ContextWrapper
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager.findFragment

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
