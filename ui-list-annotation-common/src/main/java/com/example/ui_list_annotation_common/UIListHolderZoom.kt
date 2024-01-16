package com.example.ui_list_annotation_common

typealias ItemHolderFullName = String

typealias ViewName = String

typealias EventForHolder<T> = Map<ViewName, List<Event<T>>>

typealias EventMap<T> = Map<ItemHolderFullName, EventForHolder<T>>

typealias EventMaps<T> = Pair<EventMap<T>, EventMap<T>>

typealias EventEntry<T> = Map.Entry<ItemHolderFullName, EventForHolder<T>>

class UIListHolderZoom<T> {
    private val holderEntry = mutableListOf<Entry<T>>()
    private val clickEventMap = mutableMapOf<ItemHolderFullName, EventForHolder<T>>()
    private val longClickEventMap = mutableMapOf<ItemHolderFullName, EventForHolder<T>>()

    fun debugState(): String {
        return "click:${clickEventMap.size} " +
            "long:${longClickEventMap.size} " +
            "holder:${holderEntry.size} "
    }

    fun grouped() = holderEntry.groupBy {
        it.packageName
    }

    fun addHolderEntry(list: List<Entry<T>>) {
        holderEntry.addAll(getGroupedHolders(holderEntry + list))
    }

    fun addClickEvent(map: Map<String, Map<String, List<Event<T>>>>) {
        clickEventMap.putAll(map)
    }

    fun addLongClick(map: Map<String, Map<String, List<Event<T>>>>) {
        longClickEventMap.putAll(map)
    }

    /**
     * 合并相同itemHolder的entry
     */
    private fun getGroupedHolders(mutableList: List<Entry<T>>): List<Entry<T>> {
        return mutableList.groupBy {
            it.itemHolderFullName
        }.map { entry ->
            val first = entry.value.first()
            entry.value.subList(1, entry.value.size).map {
                first.viewHolders.putAll(it.viewHolders)
            }
            first
        }
    }

    fun importHolders(entries: List<Entry<T>>): String {
        return entries.joinToString("\n") { entry ->
            val bindings = entry.viewHolders.values.map {
                it.bindingFullName
            }.distinct().joinToString("\n") {
                "import $it;"
            }
            val viewHolders = entry.viewHolders.values.joinToString("\n") {
                "import ${it.viewHolderFullName};"
            }
            bindings + "\n" + viewHolders + "\nimport ${entry.itemHolderFullName};//item holder end\n"
        }
    }

    private fun getHasComposeView(entries: List<Entry<T>>): Boolean {
        return entries.any { entry ->
            entry.viewHolders.any {
                !it.value.bindingName.endsWith("Binding")
            }
        }
    }

    private fun receiverList(longClickEvent: Map<String, Map<String, List<Event<T>>>>) =
        longClickEvent.flatMap { it.value.flatMap { entry -> entry.value } }
            .map { it.receiverFullName }.distinct()

    fun importReceiverClass(
        clickEventMap: EventMap<T>,
        longClickEventMap: EventMap<T>
    ): String {
        val flatMap = receiverList(clickEventMap)
        val flatMap2 = receiverList(longClickEventMap)
        return flatMap.plus(flatMap2).joinToString("\n") {
            "import $it;\n"
        }
    }

    fun importComposeLibrary(entries: List<Entry<T>>): String {
        return if (getHasComposeView(entries)) {
            "import androidx.compose.ui.platform.ComposeView;\n"
        } else {
            ""
        }
    }

    fun extractEventMap(
        allItemHolderName: List<ItemHolderFullName>
    ): EventMaps<T> {
        val predicate: (EventEntry<T>) -> Boolean =
            {
                allItemHolderName.any { entry ->
                    entry == it.key
                }
            }

        val clickEventsMap = clickEventMap.filter(predicate)
        val longClickEventsMap = longClickEventMap.filter(predicate)
        return Pair(clickEventsMap, longClickEventsMap)
    }
}

inline fun <T, K1 : Any, K2 : Any, V> Iterable<T>.doubleLayerGroupBy(
    doubleKeySelector: (T) -> Pair<K1, K2>?,
    valueTransform: (T) -> V
): Map<K1, Map<K2, List<V>>> {
    val destination = mutableMapOf<K1, MutableMap<K2, MutableList<V>>>()
    for (element in this) {
        val key = doubleKeySelector(element)
        key?.let {
            val map = destination.getOrPut(key.first) { mutableMapOf() }
            val secondMap = map.getOrPut(key.second) {
                mutableListOf()
            }
            secondMap.add(valueTransform(element))
        }
    }
    return destination
}

inline fun <T, K1, K2, V> Sequence<T>.doubleLayerGroupBy(
    doubleKeySelector: (T) -> Pair<K1, K2>?,
    valueTransform: (T) -> V
): Map<K1, Map<K2, List<V>>> {
    val destination = mutableMapOf<K1, MutableMap<K2, MutableList<V>>>()
    for (element in this) {
        val key = doubleKeySelector(element)
        key?.let {
            val map = destination.getOrPut(key.first) { mutableMapOf() }
            val secondMap = map.getOrPut(key.second) {
                mutableListOf()
            }
            secondMap.add(valueTransform(element))
        }
    }
    return destination
}
