package com.storyteller_f.ext_func_compiler

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.storyteller_f.ext_func_definition.ExtFuncFlat

internal fun generatePropertyV2(name: String) = """
    val Fragment.$name get() = requireContext().$name
    val View.$name get() = context.$name
""".trimIndent()

internal fun generatePropertyV3(name: String) = """
    val Fragment.$name get() = requireContext().$name
    val View.$name get() = context.$name
    val ViewBinding.$name get() = binding.root.context.$name
""".trimIndent()

internal fun generatePropertyV4(name: String, annotation: ExtFuncFlat): String {
    return """
    ${if (annotation.isContextReceiver) "context(ctx: Context)" else ""}
    val Int.$name
    get() = toFloat().$name
    
    ${if (annotation.isContextReceiver) "context(v: View)" else ""}
    val Int.${name}1
    get() = v.context.run {
        toFloat().$name
    }
    
    ${if (annotation.isContextReceiver) "context(f: Fragment)" else ""}
    val Int.${name}2
    get() = f.requireContext().run {
        toFloat().$name
    }
    """.trimIndent()
}

val builtinMethod = listOf("equals", "hashCode", "toString", "<init>")

internal fun generateForV8(task: ExtFuncProcessor.Task, logger: KSPLogger): String {
    val ksAnnotated = task.ksAnnotated as KSPropertyDeclaration
    val fieldName = ksAnnotated.simpleName.asString()
    logger.info("ext-func v8", ksAnnotated)
    val type = ksAnnotated.type.resolve()
    val declaration = type.declaration as KSClassDeclaration
    val methods = declaration.getAllFunctions().filter {
        !builtinMethod.contains(it.simpleName.asString())
    }.joinToString("\n") {
        val methodName = it.simpleName.asString()
        logger.info("method $methodName")
        """
            fun $methodName() = $fieldName.$methodName()
        """.trimIndent()
    }
    logger.info("type ${type.javaClass.canonicalName} declaration ${declaration.javaClass.canonicalName}")
    val parent = ksAnnotated.parent as? KSClassDeclaration ?: return ""
    logger.info("parent ${parent.javaClass}")
    val className = parent.simpleName.asString()
    return """class ${className}Impl : $className() {
        |   $methods
        |}
    """.trimMargin()
}
