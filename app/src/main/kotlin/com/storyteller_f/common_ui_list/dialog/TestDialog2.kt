package com.storyteller_f.common_ui_list.dialog

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.storyteller_f.common_ui.ResponseFragment
import com.storyteller_f.common_ui.responseModel
import com.storyteller_f.common_ui.setFragmentResult
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.common_ui_list.databinding.DialogTestBinding
import kotlinx.parcelize.Parcelize

class TestDialog2 : DialogFragment(), ResponseFragment {
    override val vm by responseModel

    private var _binding: DialogTestBinding? = null
    val binding: DialogTestBinding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = DialogTestBinding.inflate(layoutInflater)
        _binding = binding
        binding.button.text = "dialog 2"
        binding.button.setOnClick {
            setFragmentResult(Result("TestDialog 2"))
            dismiss()
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @Parcelize
    class Result(val test: String) : Parcelable
}
