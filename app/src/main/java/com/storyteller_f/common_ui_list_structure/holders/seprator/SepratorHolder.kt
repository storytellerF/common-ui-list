package com.storyteller_f.common_ui_list_structure.holders.seprator

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.annotation_defination.ItemHolder
import com.storyteller_f.common_ui_list_structure.R
import com.storyteller_f.slim_ktx.propertiesSame
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.view_holder_compose.ComposeViewHolder
import com.storyteller_f.view_holder_compose.EDComposeView
import com.storyteller_f.view_holder_compose.EdComposeViewEventEmitter

@ItemHolder("separator")
abstract class SeparatorHolder : DataItemHolder()

class SeparatorItemHolder(val info: String) : SeparatorHolder() {
    override fun areItemsTheSame(other: DataItemHolder): Boolean {
        return propertiesSame(other, {
            info
        })
    }

    override fun areContentsTheSame(other: DataItemHolder): Boolean {
        return propertiesSame(other, {
            info
        })
    }
}

@BindItemHolder(SeparatorItemHolder::class)
class SeparatorViewHolder(edComposeView: EDComposeView) :
    ComposeViewHolder<SeparatorItemHolder>(edComposeView) {
    override fun bindData(itemHolder: SeparatorItemHolder) {
        edComposeView.composeView.setContent {
            Separator(itemHolder, edComposeView)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
private fun Separator(
    @PreviewParameter(RepoSeparatorProvider::class) itemHolder: SeparatorItemHolder,
    edComposeView: EdComposeViewEventEmitter = EdComposeViewEventEmitter.default
) {
    Box(
        modifier = Modifier
            .background(colorResource(id = R.color.separatorBackground))
            .combinedClickable(
                onClick = { edComposeView.notifyClickEvent("card") },
                onLongClick = { edComposeView.notifyLongClickEvent("card") }
            )
            .fillMaxWidth()
    ) {
        Text(
            text = itemHolder.info,
            modifier = Modifier.padding(12.dp),
            color = colorResource(
                id = R.color.separatorText
            ),
            fontSize = 25.sp,
        )
    }
}

class RepoSeparatorProvider : PreviewParameterProvider<SeparatorItemHolder> {
    override val values: Sequence<SeparatorItemHolder>
        get() = sequence {
            yield(SeparatorItemHolder("90.000+ starts"))
        }
}
