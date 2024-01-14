package com.storyteller_f.ui_list_annotation_compiler_ksp

import com.example.ui_list_annotation_common.Entry
import com.example.ui_list_annotation_common.UiAdapterGenerator
import com.google.devtools.ksp.symbol.KSAnnotated
import com.storyteller_f.slim_ktx.trimInsertCode
import com.storyteller_f.slim_ktx.yes

class KotlinGenerator : UiAdapterGenerator<KSAnnotated>() {
    override fun buildAddFunction(entry: List<Entry<KSAnnotated>>): String {
        var index = 0
        val addFunctions = entry.joinToString("\n") {
            buildRegisterBlock(it, index++)
        }
        return """
            fun add(offset: Int) : Int {
                $1
                return $index;
            }
            """.trimInsertCode(addFunctions.yes())
    }

    private fun buildRegisterBlock(it: Entry<KSAnnotated>, index: Int) = """
                registerCenter.put(${it.itemHolderName}::class.java, $index + offset);
                list.add(::buildFor${it.itemHolderName});
    """.trimIndent()
}
