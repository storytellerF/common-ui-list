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

val WindowInsetsCompat.navigator get() = getInsets(WindowInsetsCompat.Type.navigationBars())

val WindowInsetsCompat.status get() = getInsets(WindowInsetsCompat.Type.statusBars())

val WindowInsetsCompat.ime get() = getInsets(WindowInsetsCompat.Type.ime())

inline fun View.updateMargins(block: ViewGroup.MarginLayoutParams.() -> Unit) {
    updateLayoutParams(block)
}

@ExtFuncFlat()
val Context.lf: LayoutInflater
    get() = LayoutInflater.from(this)

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
