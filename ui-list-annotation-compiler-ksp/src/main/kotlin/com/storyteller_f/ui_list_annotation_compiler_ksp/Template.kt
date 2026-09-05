package com.storyteller_f.ui_list_annotation_compiler_ksp

internal fun String.replaceCode(vararg codeBlock: CodeBlock): String =
    codeBlock.foldIndexed(this) { index, content, block ->
        content.replace("$${index + 1}", block.indentRest())
    }

internal fun String.trimAndReplaceCode(vararg codeBlock: CodeBlock): String =
    trimIndent().replaceCode(*codeBlock)

internal fun String.no(): CodeBlock = CodeBlock(this, 0)

internal fun String.yes(indent: Int = 1): CodeBlock = CodeBlock(this, indent)

internal class CodeBlock(private val content: String, private val indent: Int) {
    fun indentRest(): String = content.lineSequence().mapIndexed { index, line ->
        when {
            index == 0 -> line
            line.isNotBlank() -> "    ".repeat(indent) + line
            line.length < 4 -> "    "
            else -> line
        }
    }.joinToString("\n")
}
