package com.example.ui_list_annotation_common

import com.storyteller_f.slim_ktx.trimInsertCode
import com.storyteller_f.slim_ktx.yes
import javax.lang.model.element.Element

abstract class UiAdapterGenerator<T> {
    /**
     * 用于添加到列表中
     */
    abstract fun buildAddFunction(entry: List<Entry<T>>): String

    companion object {

        val commonImports = listOf(
            "import android.content.Context",
            "import com.storyteller_f.ui_list.core.AbstractViewHolder",
            "import android.view.LayoutInflater",
            "import android.view.ViewGroup"
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
            """.trimInsertCode(addFunctions.yes())
    }

    private fun buildRegisterBlock(it: Entry<Element>, index: Int) = """
                getRegisterCenter().put(${it.itemHolderName}.class, $index + offset);
                getList().add($CLASS_NAME::buildFor${it.itemHolderName});
    """.trimIndent()

    companion object {
        const val CLASS_NAME = "HolderBuilder"
    }
}
