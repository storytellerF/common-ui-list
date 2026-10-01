package sample.ui_list

//scope: <sources>/sample/Sample.kt
//file 0: 


import sample.RepoViewItemBinding
import sample.RepoViewHolder
import sample.RepoItemHolder
import sample.ClickReceiver
import com.storyteller_f.ui_list.core.AbstractViewHolder
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.storyteller_f.ui_list.core.BuildBatch
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.event.findFragmentOrNull
import kotlin.reflect.KClass

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
fun buildRepoItemHolder(parent: ViewGroup, type: String, key: String): AbstractViewHolder<*> {
    if (type.equals("")) {
        val context = parent.context
        val binding = RepoViewItemBinding.inflate(LayoutInflater.from(context), parent, false)
    
        val viewHolder = RepoViewHolder(binding, key)
        binding.root.setOnClickListener { v ->
            viewHolder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position -> v.findFragmentOrNull<ClickReceiver>()?.clickRepo(position) }
        }
        binding.root.setOnLongClickListener { v ->
            if (viewHolder.bindingAdapterPosition == RecyclerView.NO_POSITION) {
                return@setOnLongClickListener false
            }
            viewHolder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position -> v.findFragmentOrNull<ClickReceiver>()?.longClickRepo(position) }
            true
        }
        return viewHolder
    }//type if end
    throw Exception("unrecognized type:[$type]")
}

fun registerRepoItemHolder(map: MutableMap<KClass<out DataItemHolder>, BuildBatch>) {
    map.put(RepoItemHolder::class, BuildBatch(b3 = ::buildRepoItemHolder));
}


// --- file ---

package sample.ui_list

//scope: <sources>/sample/Sample.kt
//file 0: 


import com.storyteller_f.view_holder_compose.EDComposeView
import sample.SeparatorViewHolder
import sample.SeparatorItemHolder
import sample.ClickReceiver
import com.storyteller_f.ui_list.core.AbstractViewHolder
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.storyteller_f.ui_list.core.BuildBatch
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.event.findFragmentOrNull
import kotlin.reflect.KClass

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
fun buildSeparatorItemHolder(parent: ViewGroup, type: String): AbstractViewHolder<*> {
    if (type.equals("")) {
        val context = parent.context
        val view = EDComposeView(context)
        val viewHolder = SeparatorViewHolder(view)
        @Suppress("UNUSED_VARIABLE") val v = viewHolder.itemView
        view.clickListener = { s ->
            if (s == "card") {
                viewHolder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position -> v.findFragmentOrNull<ClickReceiver>()?.clickSeparator(v, position) }
            }//if end
        }
        view.longClickListener = { s ->
            if (s == "card") {
                viewHolder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position -> v.findFragmentOrNull<ClickReceiver>()?.longClickSeparator(position) }
            }//if end
        }
        return viewHolder
    }//type if end
    throw Exception("unrecognized type:[$type]")
}

fun registerSeparatorItemHolder(map: MutableMap<KClass<out DataItemHolder>, BuildBatch>) {
    map.put(SeparatorItemHolder::class, BuildBatch(b2 = ::buildSeparatorItemHolder));
}
