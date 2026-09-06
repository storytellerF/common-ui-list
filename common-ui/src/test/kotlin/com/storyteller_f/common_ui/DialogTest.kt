package com.storyteller_f.common_ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.viewbinding.ViewBinding
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class DialogTest {

    @Test
    fun `simple dialog inflates binding and invokes bind callback`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val dialog = TestSimpleDialog()

        dialog.show(activity.supportFragmentManager, "simple")
        activity.supportFragmentManager.executePendingTransactions()

        assertTrue(dialog.didBind)
        assertNotNull(dialog.boundRoot)
        dialog.dismiss()
        activity.supportFragmentManager.executePendingTransactions()
    }

    @Test
    fun `dialog requests attach a uuid to provided arguments`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val parameters = Bundle().apply { putString("source", "class") }

        val request = activity.request(TestResponseDialog::class.java, parameters)
        activity.supportFragmentManager.executePendingTransactions()

        val dialog = activity.supportFragmentManager.fragments.single() as TestResponseDialog
        val uuid = dialog.arguments?.getSerializable("uuid") as UUID
        assertTrue(request.toString().contains(uuid.toString()))
    }

    @Test
    fun `kclass dialog request creates a dialog`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()

        val request = activity.request(TestResponseDialog::class)
        activity.supportFragmentManager.executePendingTransactions()

        val dialog = activity.supportFragmentManager.fragments.single() as TestResponseDialog
        val uuid = dialog.arguments?.getSerializable("uuid") as UUID
        assertTrue(request.toString().contains(uuid.toString()))
    }
}

class TestSimpleDialog : SimpleDialogFragment<TestDialogBinding>({ inflater ->
    TestDialogBinding(View(inflater.context))
}) {
    var didBind = false
    var boundRoot: View? = null

    override fun onBindViewEvent(binding: TestDialogBinding) {
        didBind = true
        boundRoot = binding.root
    }
}

class TestDialogBinding(private val view: View) : ViewBinding {
    override fun getRoot() = view
}

class TestResponseDialog : CommonDialogFragment()
