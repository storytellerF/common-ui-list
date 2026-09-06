package com.storyteller_f.ui_list_annotation_common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UIListHolderZoomTest {

    @Test
    fun `holder metadata exposes constructor parameters and entry package`() {
        val holder = holder("RepoBinding", "RepoHolder", parameterCount = 2)
        val entry = entry("RepoItem", mapOf("root" to holder))

        assertEquals(", key", holder.constructorExtraParams)
        assertEquals("com.example", entry.packageName)
        assertEquals(
            "Event(receiver='listener', functionName='onClick', parameterCount=item)",
            event("onClick").toString(),
        )
        assertTrue(UiAdapterGenerator.commonImports.contains("kotlin.reflect.KClass"))
    }

    @Test
    fun `merges holders and exposes their imports`() {
        val zoom = UIListHolderZoom<String>()
        zoom.addHolderEntry(
            listOf(
                entry("RepoItem", mapOf("root" to holder("RepoBinding", "RepoHolder"))),
                entry("RepoItem", mapOf("title" to holder("TitleBinding", "TitleHolder"))),
            ),
        )

        val merged = zoom.entries().single()
        assertEquals(setOf("root", "title"), merged.viewHolders.keys)
        assertEquals(listOf(merged), zoom.grouped()["com.example"])
        assertEquals(
            listOf(
                "com.example.RepoBinding",
                "com.example.TitleBinding",
                "com.example.RepoHolder",
                "com.example.TitleHolder",
                "com.example.RepoItem",
            ),
            zoom.importHolders(zoom.entries()),
        )
        assertEquals("click:0 long:0 holder:1 ", zoom.debugState())
    }

    @Test
    fun `filters events by holder and imports distinct receivers`() {
        val zoom = UIListHolderZoom<String>()
        val click = mapOf(
            "com.example.RepoItem" to mapOf("root" to listOf(event("onClick", "com.example.ClickListener"))),
        )
        val longClick = mapOf(
            "com.example.OtherItem" to mapOf("root" to listOf(event("onLongClick", "com.example.ClickListener"))),
            "com.example.RepoItem" to mapOf("title" to listOf(event("onLongPress", "com.example.LongClickListener"))),
        )
        zoom.addClickEvent(click)
        zoom.addLongClick(longClick)

        val (clickEvents, longClickEvents) = zoom.extractEventMap(listOf("com.example.RepoItem"))
        assertEquals(click, clickEvents)
        assertEquals(mapOf("com.example.RepoItem" to longClick.getValue("com.example.RepoItem")), longClickEvents)
        assertEquals(
            listOf("com.example.ClickListener", "com.example.LongClickListener"),
            zoom.importReceiverClass(clickEvents, longClickEvents),
        )
        assertEquals("click:1 long:2 holder:0 ", zoom.debugState())
        assertTrue(zoom.extractEventMap(emptyList()).first.isEmpty())
    }

    private fun holder(binding: String, viewHolder: String, parameterCount: Int = 1) = Holder(
        bindingName = binding,
        bindingFullName = "com.example.$binding",
        viewHolderName = viewHolder,
        viewHolderFullName = "com.example.$viewHolder",
        parameterCount = parameterCount,
        origin = binding,
    )

    private fun entry(name: String, holders: Map<String, Holder<String>>) = Entry(
        itemHolderName = name,
        itemHolderFullName = "com.example.$name",
        itemHolderOrigin = name,
        viewHolders = holders.toMutableMap(),
    )

    private fun event(
        functionName: String,
        receiverFullName: String = "com.example.Listener",
    ) = Event(
        receiver = "listener",
        receiverFullName = receiverFullName,
        functionName = functionName,
        parameterList = "item",
        origin = functionName,
    )
}
