package com.storyteller_f.common_ui_list_structure.test_model

import android.os.Bundle
import android.view.View
import com.storyteller_f.common_ui.CommonFragment
import com.storyteller_f.common_ui.repeatOnViewResumed
import com.storyteller_f.common_ui_list_structure.R
import com.storyteller_f.common_ui_list_structure.api.requireReposService
import com.storyteller_f.common_ui_list_structure.databinding.FragmentTestDataBinding
import com.storyteller_f.common_ui_list_structure.holders.RepoItemHolder
import com.storyteller_f.common_ui_list_structure.holders.RepoViewHolder
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import com.storyteller_f.ext_func_definition.ExtFuncFlatType
import com.storyteller_f.ui_list.adapter.SimpleDataAdapter
import com.storyteller_f.ui_list.event.viewBinding
import com.storyteller_f.ui_list.source.DataProducer
import com.storyteller_f.ui_list.source.data

class Test {
    fun sayTest() {
        println("test")
    }
}

open class TestDataViewModelFragment : CommonFragment(R.layout.fragment_test_data) {
    /**
     * auto generate
     * fun test() = test.test()
     */
    @Suppress("unused")
    @ExtFuncFlat(ExtFuncFlatType.V8)
    val test = Test()

    private val data by data(
        DataProducer(
            { p, size ->
                requireReposService.searchRepos(p, size)
            },
            { repo, _ -> RepoItemHolder(repo) },
        )
    )
    private val adapter = SimpleDataAdapter<RepoItemHolder, RepoViewHolder>()
    private val binding: FragmentTestDataBinding by viewBinding(FragmentTestDataBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.listWithState.dataUp(adapter, viewLifecycleOwner, data)
        repeatOnViewResumed {
            data.content.observe(viewLifecycleOwner) {
                adapter.submitData(it)
            }
        }
    }
}
