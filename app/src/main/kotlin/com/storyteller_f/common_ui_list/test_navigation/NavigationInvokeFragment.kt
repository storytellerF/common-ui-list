package com.storyteller_f.common_ui_list.test_navigation

import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.storyteller_f.common_pr.request
import com.storyteller_f.common_pr.response
import com.storyteller_f.common_ui.SimpleFragment
import com.storyteller_f.common_ui.request
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.common_ui_list.R
import com.storyteller_f.common_ui_list.databinding.FragmentNavigationInvokeBinding
import com.storyteller_f.common_ui_list.dialog.NavigationDialog
import com.storyteller_f.common_ui_list.dialog.TestDialog2

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class NavigationInvokeFragment : SimpleFragment<FragmentNavigationInvokeBinding>(
    FragmentNavigationInvokeBinding::inflate
) {
    override fun onBindViewEvent(binding: FragmentNavigationInvokeBinding) {
        binding.buttonFirst.setOnClickListener {
            NavigationInvokeFragmentDirections.actionFirstFragmentToSecondFragment().request()
                .response(NavigationResultFragment.Result::class.java) { r ->
                    Toast.makeText(requireContext(), "fragment： ${r.hh}", Toast.LENGTH_SHORT).show()
                }
        }
        binding.textviewFirst.setOnClick {
            findNavController().request(R.id.action_FirstFragment_to_testDialog)
                .response(NavigationDialog.Result::class.java) { r ->
                    Toast.makeText(
                        requireContext(),
                        "fragment-->dialog ${r.test}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
        binding.button2.setOnClick {
            request(TestDialog2::class.java).response(TestDialog2.Result::class.java) { r ->
                Toast.makeText(requireContext(), "dialog ${r.test}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
