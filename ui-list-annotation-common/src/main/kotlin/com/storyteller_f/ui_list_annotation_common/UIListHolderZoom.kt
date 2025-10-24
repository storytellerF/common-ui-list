package com.storyteller_f.ui_list_annotation_common

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

    fun importHolders(entries: List<Entry<T>>): List<String> {
        return entries.flatMap { entry ->
            val values = entry.viewHolders.values
            val bindings = values.map {
                it.bindingFullName
            }
            val viewHolders = values.map {
                it.viewHolderFullName
            }
            bindings + viewHolders + entry.itemHolderFullName
        }
    }

    private fun receiverList(longClickEvent: Map<String, Map<String, List<Event<T>>>>) =
        longClickEvent.flatMap { it.value.flatMap { entry -> entry.value } }
            .map { it.receiverFullName }.distinct()

    fun importReceiverClass(
        clickEventMap: EventMap<T>,
        longClickEventMap: EventMap<T>
    ): List<String> {
        val flatMap = receiverList(clickEventMap)
        val flatMap2 = receiverList(longClickEventMap)
        return (flatMap + flatMap2).distinct()
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
