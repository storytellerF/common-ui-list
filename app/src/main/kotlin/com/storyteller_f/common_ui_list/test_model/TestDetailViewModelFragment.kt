package com.storyteller_f.common_ui_list.test_model

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.storyteller_f.common_ui_list.api.ReposService
import com.storyteller_f.common_ui_list.api.requireReposService
import com.storyteller_f.common_ui_list.databinding.FragmentTestDetailBinding
import com.storyteller_f.common_ui_list.db.RepoDatabase
import com.storyteller_f.common_ui_list.db.requireRepoDatabase
import com.storyteller_f.common_vm_ktx.vm
import com.storyteller_f.ui_list.source.DetailHandler
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class TestDetailViewModelFragment : Fragment() {

    private val detail by vm({
        TestDetailDependencies(requireReposService, requireContext().requireRepoDatabase)
    }) { dependencies: TestDetailDependencies ->
        TestDetailViewModel(dependencies.service, dependencies.database)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FragmentTestDetailBinding.inflate(inflater, container, false).also(::bind).root

    private fun bind(binding: FragmentTestDetailBinding) {
        val textView: TextView = binding.textNotifications
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                detail.content.collect { repo ->
                    textView.text = repo?.fullName.orEmpty()
                }
            }
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
