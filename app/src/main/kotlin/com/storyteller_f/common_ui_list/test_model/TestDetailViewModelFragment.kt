package com.storyteller_f.common_ui_list.test_model

import android.widget.TextView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storyteller_f.common_ui.RegularFragment
import com.storyteller_f.common_ui_list.api.ReposService
import com.storyteller_f.common_ui_list.api.requireReposService
import com.storyteller_f.common_ui_list.databinding.FragmentTestDetailBinding
import com.storyteller_f.common_ui_list.db.RepoDatabase
import com.storyteller_f.common_ui_list.db.requireRepoDatabase
import com.storyteller_f.common_vm_ktx.vm
import com.storyteller_f.ui_list.source.DetailHandler

class TestDetailViewModelFragment : RegularFragment<FragmentTestDetailBinding>(FragmentTestDetailBinding::inflate) {

    private val detail by vm({
        TestDetailDependencies(requireReposService, requireContext().requireRepoDatabase)
    }) { dependencies: TestDetailDependencies ->
        TestDetailViewModel(dependencies.service, dependencies.database)
    }

    override fun onBindViewEvent(binding: FragmentTestDetailBinding) {
        val textView: TextView = binding.textNotifications
        detail.content.observe(viewLifecycleOwner) {
            textView.text = it.fullName
        }
    }
}

private data class TestDetailDependencies(
    val service: ReposService,
    val database: RepoDatabase,
)

private class TestDetailViewModel(
    service: ReposService,
    database: RepoDatabase,
) : ViewModel() {
    private val detailHandler = DetailHandler(
        producer = {
            service.searchRepos(1, 1).items.first()
        },
        local = {
            database.reposDao().select()
        }
    )

    val content = detailHandler.content
    val loadState = detailHandler.loadState

    init {
        detailHandler.load(viewModelScope)
    }

    fun refresh() {
        detailHandler.refresh(viewModelScope)
    }
}
