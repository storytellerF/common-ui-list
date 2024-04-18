package com.storyteller_f.common_ui_list_structure.holders

import android.annotation.SuppressLint
import android.view.View
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.annotation_defination.ItemHolder
import com.storyteller_f.common_ui_list_structure.R
import com.storyteller_f.common_ui_list_structure.databinding.RepoViewItemBinding
import com.storyteller_f.common_ui_list_structure.model.Repo
import com.storyteller_f.slim_ktx.propertiesSame
import com.storyteller_f.ui_list.core.BindingViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder

@ItemHolder("repo")
data class RepoItemHolder(val repo: Repo) : DataItemHolder(key = " from common-ui-list") {
    override fun areItemsTheSame(other: DataItemHolder): Boolean {
        return propertiesSame(other, {
            repo.id
        })
    }

    val roundedStarCount = repo.stars / 10_000
}

@BindItemHolder(RepoItemHolder::class)
class RepoViewHolder(private val binding: RepoViewItemBinding, key: String) :
    BindingViewHolder<RepoItemHolder>(binding, key) {
    @SuppressLint("SetTextI18n")
    override fun bindData(itemHolder: RepoItemHolder) {
        binding.repoName.text = itemHolder.repo.name + key
        // if the description is missing, hide the TextView
        var descriptionVisibility = View.GONE
        if (itemHolder.repo.description != null) {
            binding.repoDescription.text = itemHolder.repo.description
            descriptionVisibility = View.VISIBLE
        }
        binding.repoDescription.visibility = descriptionVisibility

        binding.repoStars.text = itemHolder.repo.stars.toString()
        binding.repoForks.text = itemHolder.repo.forks.toString()

        // if the language is missing, hide the label and the value
        var languageVisibility = View.GONE
        if (!itemHolder.repo.language.isNullOrEmpty()) {
            val resources = this.itemView.context.resources
            binding.repoLanguage.text =
                resources.getString(
                    R.string.language,
                    itemHolder.repo.language
                )
            languageVisibility = View.VISIBLE
        }
        binding.repoLanguage.visibility = languageVisibility
    }
}
