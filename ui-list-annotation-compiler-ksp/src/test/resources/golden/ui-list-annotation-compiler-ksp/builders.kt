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
            v.findFragmentOrNull<ClickReceiver>()?.clickRepo(viewHolder.itemHolder)
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
                v.findFragmentOrNull<ClickReceiver>()?.clickSeparator(v, viewHolder.itemHolder)                
            }//if end
        }
        view.longClickListener = { s ->
            if (s == "card") {
                v.findFragmentOrNull<ClickReceiver>()?.longClickSeparator(inflate, viewHolder.itemHolder)                
            }//if end
        }
        return viewHolder
    }//type if end
    throw Exception("unrecognized type:[$type]")
}

fun registerSeparatorItemHolder(map: MutableMap<KClass<out DataItemHolder>, BuildBatch>) {
    map.put(SeparatorItemHolder::class, BuildBatch(b2 = ::buildSeparatorItemHolder));
}
