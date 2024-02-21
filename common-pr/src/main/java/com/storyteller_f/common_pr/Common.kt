package com.storyteller_f.common_pr

import android.content.Context
import android.util.TypedValue
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.storyteller_f.common_vm_ktx.state
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import com.storyteller_f.ext_func_definition.ExtFuncFlatType
import kotlinx.coroutines.flow.Flow

context(LifecycleOwner)
fun <T> LiveData<T>.state(ob: Observer<T>) {
    val owner: LifecycleOwner = this@LifecycleOwner
    state(owner, ob)
}

context(LifecycleOwner)
fun <T> Flow<T>.state(function: (T) -> Unit) {
    val owner: LifecycleOwner = this@LifecycleOwner
    state(owner, function)
}

context(Context)
@ExtFuncFlat(ExtFuncFlatType.V4, isContextReceiver = true)
val Float.dip: Float
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this@dip, resources.displayMetrics)

context(Context)
@ExtFuncFlat(ExtFuncFlatType.V4, isContextReceiver = true)
val Float.dipToInt: Int
    get() = dip.toInt()
