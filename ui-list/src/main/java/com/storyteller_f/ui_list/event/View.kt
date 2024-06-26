package com.storyteller_f.ui_list.event

import android.app.Activity
import android.content.ContextWrapper
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager.findFragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class FragmentViewBindingDelegate<T : ViewBinding>(
    val fragment: Fragment,
    val viewBindingFactory: (View) -> T
) : ReadOnlyProperty<Fragment, T> {
    private var binding: T? = null

    init {
        fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                fragment.viewLifecycleOwnerLiveData.observe(fragment) { viewLifecycleOwner: LifecycleOwner? ->
                    viewLifecycleOwner?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
                        override fun onDestroy(owner: LifecycleOwner) {
                            binding = null
                        }
                    })
                }
            }
        })
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        val binding = binding
        if (binding != null) {
            return binding
        }
        val lifecycle = fragment.viewLifecycleOwner.lifecycle
        check(
            lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)
        ) { "Should not attempt to get bindings when Fragment views are destroyed." }
        return viewBindingFactory(thisRef.requireView()).also { this.binding = it }
    }
}

fun <T : ViewBinding> Fragment.viewBinding(viewBindingFactory: (View) -> T) =
    FragmentViewBindingDelegate(this, viewBindingFactory)

/**
 * 需要在onCreate 中使用，自动为activity 设置contentView
 * 所以即使用不到，也需要进行引用以确保正常
 */
inline fun <T : ViewBinding> AppCompatActivity.viewBinding(crossinline bindingInflater: (LayoutInflater) -> T) =
    lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val invoke = bindingInflater.invoke(layoutInflater)
        setContentView(invoke.root)
        invoke
    }

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
