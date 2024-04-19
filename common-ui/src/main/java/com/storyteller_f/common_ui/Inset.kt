package com.storyteller_f.common_ui

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.core.view.updatePadding

/**
 * 增加额外操作，暂停开发
 */
class InsetLayout @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attributeSet, defStyleAttr) {
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // 保存view 原始padding margin
        getInsetBlock()
    }
}

object InsetBlockDirection {
    const val TOP = 1 shl 2
    const val START = 1 shl 3
    const val END = 1 shl 4
    const val BOTTOM = 1 shl 5
}

class InsetBlock(val padding: Direction, val margin: Direction)

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
fun View.inset(
    @InsetBlockFlag statusPadding: Int = 0,
    @InsetBlockFlag statusMargin: Int = 0,
    @InsetBlockFlag navigatorPadding: Int = 0,
    @InsetBlockFlag navigatorMargin: Int = 0
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val block = getInsetBlock()
        val bottom = insets.navigator.bottom
        val top = insets.status.top
        v.updatePadding(
            block.padding + statusPadding.insetBlock(top) + navigatorPadding.insetBlock(bottom)
        )
        v.updateMargin(
            block.margin + statusMargin.insetBlock(top) + navigatorMargin.insetBlock(bottom)
        )
        insets
    }
}

fun Int.insetBlock(v: Int) =
    Direction(
        if (this and InsetBlockDirection.START == InsetBlockDirection.START) {
            v
        } else {
            0
        },
        if (this and InsetBlockDirection.TOP == InsetBlockDirection.TOP) {
            v
        } else {
            0
        },
        if (this and InsetBlockDirection.END == InsetBlockDirection.END) {
            v
        } else {
            0
        },
        if (this and InsetBlockDirection.BOTTOM == InsetBlockDirection.BOTTOM) {
            v
        } else {
            0
        }
    )

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
fun View.updatePadding(block: Direction) {
    val isRtl = layoutDirection == View.LAYOUT_DIRECTION_RTL
    updatePadding(
        if (isRtl) block.end else block.start,
        block.top,
        if (isRtl) block.start else block.end,
        block.bottom
    )
}

fun <T : Any> getOrCreate(retrieve: () -> T?, produce: () -> T): T {
    return retrieve() ?: produce()
}

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
fun View.getInsetBlock() =
    getOrCreate(
        { getTag(R.id.inset_block) as? InsetBlock },
        {
            InsetBlock(
                Direction(paddingStart, paddingTop, paddingEnd, paddingBottom),
                Direction(marginStart, marginTop, marginEnd, marginBottom)
            ).also {
                setTag(
                    R.id.inset_block,
                    it
                )
            }
        }
    )

class Direction(val start: Int, val top: Int, val end: Int, val bottom: Int) {
    operator fun plus(r: Direction): Direction {
        return Direction(start + r.start, top + r.top, end + r.end, bottom + r.bottom)
    }

    override fun toString(): String {
        return "Direction(start=$start, top=$top, end=$end, bottom=$bottom)"
    }
}

@IntDef(
    flag = true,
    value = [InsetBlockDirection.TOP, InsetBlockDirection.START, InsetBlockDirection.END, InsetBlockDirection.BOTTOM]
)
internal annotation class InsetBlockFlag
