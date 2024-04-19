package com.storyteller_f.common_ui

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.LifecycleOwner
import com.storyteller_f.common_vm_ktx.state
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

fun <T : View> T.setOnClick(block: (T) -> Unit) {
    setOnClickListener {
        block(this)
    }
}

fun <T : View> T.pp(block: (T) -> Unit) {
    post {
        block(this)
    }
}

val WindowInsetsCompat.navigator get() = getInsets(WindowInsetsCompat.Type.navigationBars())

val WindowInsetsCompat.status get() = getInsets(WindowInsetsCompat.Type.statusBars())

val WindowInsetsCompat.ime get() = getInsets(WindowInsetsCompat.Type.ime())

inline fun View.updateMargins(block: ViewGroup.MarginLayoutParams.() -> Unit) {
    updateLayoutParams(block)
}

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
fun View.updateMargin(rect: Direction) {
    updateMargins {
        marginStart = rect.start
        marginEnd = rect.end
        topMargin = rect.top
        bottomMargin = rect.bottom
    }
}

fun <T : View> T.setVisible(visible: Boolean, block: (T) -> Unit) {
    isVisible = visible
    if (visible) {
        block(this)
    }
}

inline fun <T : View, reified V> T.setVisible(obj: Any, visible: (V) -> Boolean, block: (T, V) -> Unit) {
    val b = if (obj is V) {
        visible(obj)
    } else {
        false
    }
    isVisible = b
    if (b) block(this, obj as V)
}

@ExtFuncFlat()
val Context.lf: LayoutInflater
    get() = LayoutInflater.from(this)

fun List<View>.onVisible(view: View) {
    forEach {
        it.isVisible = it === view
    }
}

fun <T> MutableStateFlow<T?>.bind(
    owner: LifecycleOwner,
    editText: EditText,
    map: T.() -> String,
    rebuild: T.(String) -> T
) {
    map {
        it?.map()
    }.state(owner) {
        if (it != editText.text.toString()) {
            editText.setText(it)
        }
    }
    editText.doAfterTextChanged { s ->
        update {
            it!!.rebuild(s.toString())
        }
    }
}
