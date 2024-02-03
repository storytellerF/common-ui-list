package com.example.ui_list_annotation_common

import com.storyteller_f.slim_ktx.trimAndReplaceCode
import com.storyteller_f.slim_ktx.yes
import javax.lang.model.element.Element

abstract class UiAdapterGenerator<T> {
    /**
     * 用于添加到列表中
     */
    abstract fun buildAddFunction(entry: List<Entry<T>>): String

    companion object {

        val commonImports = listOf(
            "com.storyteller_f.ui_list.core.AbstractViewHolder",
            "android.view.LayoutInflater",
            "android.view.ViewGroup",
            "com.storyteller_f.ui_list.event.ViewJava",
            "com.storyteller_f.ui_list.core.list",
            "com.storyteller_f.ui_list.core.registerCenter"
        )
    }
}

class JavaGenerator : UiAdapterGenerator<Element>() {
    override fun buildAddFunction(entry: List<Entry<Element>>): String {
        var index = 0
        val addFunctions = entry.joinToString("\n") {
            buildRegisterBlock(it, index++)
        }
        return """
            public static int add(int offset) {
                $1
                return $index;
            }
            """.trimAndReplaceCode(addFunctions.yes())
    }

    private fun buildRegisterBlock(it: Entry<Element>, index: Int) = """
                getRegisterCenter().put(${it.itemHolderName}.class, $index + offset);
                getList().add($CLASS_NAME::buildFor${it.itemHolderName});
    """.trimIndent()

    companion object {
        const val CLASS_NAME = "HolderBuilder"
    }
}
