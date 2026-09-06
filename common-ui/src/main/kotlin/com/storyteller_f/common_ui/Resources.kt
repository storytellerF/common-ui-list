@file:Suppress("detekt.formatting")

package com.storyteller_f.common_ui

import android.content.Context
import android.util.TypedValue
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import com.storyteller_f.ext_func_definition.ExtFuncFlatType

context(ctx: Context)
@ExtFuncFlat(ExtFuncFlatType.V4, isContextReceiver = true)
val Float.dip: Float
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this@dip, ctx.resources.displayMetrics)

context(ctx: Context)
@ExtFuncFlat(ExtFuncFlatType.V4, isContextReceiver = true)
val Float.dipToInt: Int
    get() = dip.toInt()
