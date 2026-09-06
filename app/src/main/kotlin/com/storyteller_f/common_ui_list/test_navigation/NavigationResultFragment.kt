package com.storyteller_f.common_ui_list.test_navigation

import android.os.Parcelable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.storyteller_f.common_ui.Registry
import com.storyteller_f.common_ui.ResponseFragment
import com.storyteller_f.common_ui.observeResponse
import com.storyteller_f.common_ui.responseModel
import com.storyteller_f.common_ui.setFragmentResult
import com.storyteller_f.common_ui_list.databinding.FragmentNavigationResultBinding
import kotlinx.parcelize.Parcelize

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class NavigationResultFragment : Fragment(), ResponseFragment, Registry {
    override val vm by responseModel

    override fun onStart() {
        super.onStart()
        observeResponse()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FragmentNavigationResultBinding.inflate(inflater, container, false).also(::bind).root

    private fun bind(binding: FragmentNavigationResultBinding) {
        binding.buttonSecond.setOnClickListener {
            setFragmentResult(Result("second fragment"))
            findNavController().navigateUp()
        }
    }

    @Parcelize
    class Result(val hh: String) : Parcelable
}
