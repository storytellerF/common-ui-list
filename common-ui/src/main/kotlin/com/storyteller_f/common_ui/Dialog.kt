package com.storyteller_f.common_ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding

abstract class CommonDialogFragment : DialogFragment(), ResponseFragment {
    override val vm by responseModel
}

abstract class SimpleDialogFragment<T : ViewBinding>(
    val viewBindingFactory: (LayoutInflater) -> T
) : CommonDialogFragment() {
    private var _binding: T? = null
    val binding: T get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val bindingLocal = viewBindingFactory(layoutInflater)
        _binding = bindingLocal
        onBindViewEvent(binding)
        return bindingLocal.root
    }

    abstract fun onBindViewEvent(binding: T)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
