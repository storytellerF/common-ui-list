package com.storyteller_f.ui_list_annotation_common

class Holder<T>(
    val bindingName: String,
    val bindingFullName: String,
    val viewHolderName: String,
    val viewHolderFullName: String,
    val origin: T
)

class Entry<T>(
    val itemHolderName: String,
    val itemHolderFullName: ItemHolderFullName,
    val viewHolders: MutableMap<String, Holder<T>>,
) {
    val packageName = itemHolderFullName.substringBeforeLast(".")
}

class Event<T>(
    val receiver: String,
    val receiverFullName: String,
    val functionName: String,
    val parameterList: String,
    val origin: T
) {
    override fun toString(): String {
        return "Event(receiver='$receiver', functionName='$functionName', parameterCount=$parameterList)"
    }
}
