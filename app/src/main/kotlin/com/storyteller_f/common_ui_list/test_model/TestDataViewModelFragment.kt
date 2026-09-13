package com.storyteller_f.common_ui_list.test_model

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.storyteller_f.common_ui.viewBinding
import com.storyteller_f.common_ui_list.R
import com.storyteller_f.common_ui_list.api.ReposService
import com.storyteller_f.common_ui_list.api.requireReposService
import com.storyteller_f.common_ui_list.data.SimpleResponse
import com.storyteller_f.common_ui_list.databinding.FragmentTestDataBinding
import com.storyteller_f.common_ui_list.holders.RepoItemHolder
import com.storyteller_f.common_ui_list.holders.RepoViewHolder
import com.storyteller_f.common_ui_list.holders.ui_list.registerRepoItemHolder
import com.storyteller_f.common_ui_list.source.SearchHandler
import com.storyteller_f.common_ui_list.source.SimpleSearchRepository
import com.storyteller_f.common_ui_list.ui.ListWithState
import com.storyteller_f.common_vm_ktx.vm
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import com.storyteller_f.ext_func_definition.ExtFuncFlatType
import com.storyteller_f.ui_list.adapter.SimpleSourceAdapter
import com.storyteller_f.ui_list.core.BuildBatch
import com.storyteller_f.ui_list.core.DataItemHolder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class Test {
    fun sayTest() {
        println("test")
    }
}

open class TestDataViewModelFragment : Fragment(R.layout.fragment_test_data) {
    @Suppress("unused")
    @ExtFuncFlat(ExtFuncFlatType.V8)
    val test = Test()

    private val data by vm({
        TestDataDependencies(requireReposService)
    }) { dependencies: TestDataDependencies ->
        TestDataViewModel(dependencies.service)
    }
    private val adapter = SimpleSourceAdapter<RepoItemHolder, RepoViewHolder>(
        mutableMapOf<KClass<out DataItemHolder>, BuildBatch>().apply {
            registerRepoItemHolder(this)
        }
    )
    private val binding: FragmentTestDataBinding by viewBinding(FragmentTestDataBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.listWithState.sourceUp(
            adapter,
            viewLifecycleOwner,
            flash = ListWithState.Companion::remote,
        )
        binding.searchInput.doAfterTextChanged { text ->
            data.search(text?.toString().orEmpty())
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                data.content.collectLatest { pagingData ->
                    adapter.submitData(pagingData)
                }
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
    private val query = MutableStateFlow(DEFAULT_QUERY)
    private val searchHandler = SearchHandler(
        SimpleSearchRepository { value: String, page: Int, size: Int ->
            service.searchRepos(value, page, size).let { response ->
                SimpleResponse(response.total, response.items, response.nextPage)
            }
        },
        { repo, _ -> RepoItemHolder(repo) },
    )

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val content = query
        .map(String::trim)
        .debounce(SEARCH_DEBOUNCE_MILLIS)
        .distinctUntilChanged()
        .flatMapLatest { value ->
            if (value.isEmpty()) {
                flowOf(PagingData.empty())
            } else {
                searchHandler.search(value, viewModelScope)
            }
        }

    fun search(value: String) {
        query.value = value
    }

    companion object {
        private const val DEFAULT_QUERY = "Android"
        private const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
