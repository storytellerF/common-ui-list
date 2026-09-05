package com.storyteller_f.ext_func_compiler

import com.google.devtools.ksp.symbol.KSFunctionDeclaration

internal fun generatePropertyV5(task: ExtFuncProcessor.Task): Pair<Set<String>, String> {
    val arguments = if (task.ksAnnotated is KSFunctionDeclaration) {
        task.ksAnnotated.parameters.map {
            it.type.element
        }.joinToString(",")
    } else {
        null
    }
    val imports = getImports(task.ksAnnotated) + listOf(
        "androidx.fragment.app.Fragment", "androidx.activity.ComponentActivity"
    )
    return imports to extendVm(arguments, task)
}

private fun extendVm(extra: String?, task: ExtFuncProcessor.Task): String {
    val parameterList =
        getParameterListExcludeDefaultList(task.ksAnnotated as KSFunctionDeclaration)
    val parameterString = parameterList.joinToString(",\n").indent(3)
    val second = parameterList.toMutableList().apply {
        add(1, "vmScope: VMScope")
    }.joinToString(",\n").indent(3)
    return """
        //$extra
        @MainThread
        inline fun <reified VM : ViewModel, ARG> Fragment.a${task.name}(
            $parameterString
        ) = ${task.name}(arg, { requireActivity().viewModelStore }, { requireActivity() }, vmProducer)
        @MainThread
        inline fun <reified VM : ViewModel, ARG> Fragment.p${task.name}(
            $parameterString
        ) = ${task.name}(arg, { requireParentFragment().viewModelStore }, { requireParentFragment() }, vmProducer)
        @MainThread
        inline fun <reified VM : ViewModel, T, ARG> T.${task.name}(
            $second
        )  where T : HasDefaultViewModelProviderFactory, T : ViewModelStoreOwner = ${task.name}(arg, vmScope.storeProducer, vmScope.ownerProducer, vmProducer)
    """.trimIndent()
}

private fun String.indent(level: Int): String = lineSequence().mapIndexed { index, line ->
    when {
        index == 0 -> line
        line.isNotBlank() -> "    ".repeat(level) + line
        line.length < 4 -> "    "
        else -> line
    }
}.joinToString("\n")
