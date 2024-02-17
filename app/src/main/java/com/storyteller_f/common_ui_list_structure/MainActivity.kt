package com.storyteller_f.common_ui_list_structure

import android.content.Intent
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.common_pr.dipToInt
import com.storyteller_f.common_ui.navigator
import com.storyteller_f.common_ui.owner
import com.storyteller_f.common_ui.repeatOnViewResumed
import com.storyteller_f.common_ui.status
import com.storyteller_f.common_ui.supportNavigatorBarImmersive
import com.storyteller_f.common_ui.updateMargins
import com.storyteller_f.common_ui_list_structure.api.requireReposService
import com.storyteller_f.common_ui_list_structure.databinding.ActivityMainBinding
import com.storyteller_f.common_ui_list_structure.db.composite.RepoComposite
import com.storyteller_f.common_ui_list_structure.db.requireRepoDatabase
import com.storyteller_f.common_ui_list_structure.holders.RepoItemHolder
import com.storyteller_f.common_ui_list_structure.holders.seprator.SeparatorItemHolder
import com.storyteller_f.common_ui_list_structure.test_model.TestViewModelActivity
import com.storyteller_f.common_ui_list_structure.test_navigation.TestNavigationResultActivity
import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.event.viewBinding
import com.storyteller_f.ui_list.source.SourceProducer
import com.storyteller_f.ui_list.source.source
import com.storyteller_f.ui_list.ui.ListWithState
import com.storyteller_f.view_holder_compose.ComposeSourceAdapter
import kotlinx.coroutines.flow.collectLatest

class MainActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityMainBinding::inflate)
    private val editing = MutableLiveData(false)
    private val viewModel by source({ }, {
        SourceProducer(
            { RepoComposite(requireRepoDatabase) },
            { p, c ->
                requireReposService.searchRepos(p, c)
            },
            {
                requireRepoDatabase.reposDao().selectAll()
            },
            { repo, _ -> RepoItemHolder(repo) },
            { before, after ->
                val dataItemHolder: SeparatorItemHolder? = when {
                    after == null -> null
                    before == null -> SeparatorItemHolder("${after.roundedStarCount}0.000+ stars")
                    before.roundedStarCount <= after.roundedStarCount -> null
                    after.roundedStarCount >= 1 -> SeparatorItemHolder("${after.roundedStarCount}0.000+ stars")
                    else -> SeparatorItemHolder("< 10.000+ stars")
                }
                println("add separator ${dataItemHolder?.info}")
                dataItemHolder
            }
        )
    })

    private val adapter =
        ComposeSourceAdapter<DataItemHolder, AbstractViewHolder<DataItemHolder>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.content.sourceUp(adapter, this)
        val dp24 = 24.dipToInt
        binding.content.setupClickSelectableSupport(
            editing,
            owner,
            object : ListWithState.SelectableDrawer {
                override fun width(
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State,
                    childAdapterPosition: Int,
                    absoluteAdapterPosition: DataItemHolder
                ): Int {
                    return dp24 * 3
                }

                override fun draw(
                    c: Canvas,
                    top: Int,
                    bottom: Int,
                    childWidth: Int,
                    childHeight: Int,
                    parentWidth: Int,
                    parentHeight: Int,
                    child: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val offset = (childHeight - dp24) / 2
                    val t = top + offset
                    ContextCompat.getDrawable(
                        this@MainActivity,
                        R.drawable.ic_baseline_radio_button_unchecked_24
                    )?.run {
                        setBounds(parentWidth - dp24 * 2, t, (parentWidth - dp24), t + dp24)
                        draw(c)
                    }
                }
            }
        )
        supportNavigatorBarImmersive(binding.root)
        repeatOnViewResumed {
            viewModel.content.collectLatest {
                adapter.submitData(it)
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            printInsets(insets)
            binding.buttonGroup.updateMargins {
                topMargin = insets.status.top
            }
            binding.content.recyclerView.updatePadding(bottom = insets.navigator.bottom)
            insets
        }
        binding.buttonGroup.run {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(lifecycle))
            setContent {
                ButtonGroup({
                    startActivity(Intent(this@MainActivity, TestViewModelActivity::class.java))
                }) {
                    startActivity(
                        Intent(
                            this@MainActivity,
                            TestNavigationResultActivity::class.java
                        )
                    )
                }
            }
        }
    }

    @BindClickEvent(RepoItemHolder::class)
    fun clickRepo(itemHolder: RepoItemHolder) {
        Toast.makeText(this, itemHolder.repo.fullName, Toast.LENGTH_SHORT).show()
    }

    @BindClickEvent(SeparatorItemHolder::class, "card")
    fun clickLine(view: View, itemHolder: SeparatorItemHolder) {
        Toast.makeText(this, "${itemHolder.info} ${view::class.qualifiedName}", Toast.LENGTH_SHORT)
            .show()
    }

    private fun printInsets(insets: WindowInsetsCompat) {
        val insets1 = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        Log.d(TAG, "printInsets: navigator ${insets1.bottom} status ${insets1.top}")
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    @Preview
    @Composable
    fun ButtonGroup(next: () -> Unit = {}, response: () -> Unit = {}) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        ) {
            Button(
                onClick = {
                    editing.value = true
                },
            ) {
                Text(text = "edit")
            }
            Button(
                onClick = {
                    editing.value = false
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(text = "redo")
            }
            Button(
                onClick = {
                    next()
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(text = "next")
            }
            Button(
                onClick = {
                    response()
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(text = "test")
            }
        }
    }
}
