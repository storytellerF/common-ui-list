package com.storyteller_f.common_ui_list.test_model

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModel
import com.storyteller_f.common_ui.CommonFragment
import com.storyteller_f.common_ui.repeatOnViewResumed
import com.storyteller_f.common_ui_list.R
import com.storyteller_f.common_ui_list.api.ReposService
import com.storyteller_f.common_ui_list.api.requireReposService
import com.storyteller_f.common_ui_list.databinding.FragmentTestDataBinding
import com.storyteller_f.common_ui_list.holders.RepoItemHolder
import com.storyteller_f.common_ui_list.holders.RepoViewHolder
import com.storyteller_f.common_ui_list.holders.ui_list.registerRepoItemHolder
import com.storyteller_f.common_vm_ktx.vm
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import com.storyteller_f.ext_func_definition.ExtFuncFlatType
import com.storyteller_f.ui_list.adapter.SimpleDataAdapter
import com.storyteller_f.ui_list.core.BuildBatch
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.event.viewBinding
import com.storyteller_f.ui_list.source.DataHandler
import com.storyteller_f.ui_list.source.SimpleDataRepository
import kotlin.reflect.KClass

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

    private val data by vm({
        TestDataDependencies(requireReposService)
    }) { dependencies: TestDataDependencies ->
        TestDataViewModel(dependencies.service)
    }
    private val adapter = SimpleDataAdapter<RepoItemHolder, RepoViewHolder>(
        mutableMapOf<KClass<out DataItemHolder>, BuildBatch>().apply {
            registerRepoItemHolder(this)
        }
    )
    private val binding: FragmentTestDataBinding by viewBinding(FragmentTestDataBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.listWithState.dataUp(adapter, viewLifecycleOwner, data.dataHandler)
        repeatOnViewResumed {
            data.content.observe(viewLifecycleOwner) {
                adapter.submitData(it)
            }
        }
    }
}

private data class TestDataDependencies(
    val service: ReposService,
)

private class TestDataViewModel(
    service: ReposService,
) : ViewModel() {
    val dataHandler = DataHandler(
        SimpleDataRepository { page, size ->
            service.searchRepos(page, size)
        },
        { repo -> RepoItemHolder(repo) },
    )

    val content = dataHandler.content
}
