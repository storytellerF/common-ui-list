package com.storyteller_f.common_ui_list.test_model

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.storyteller_f.common_ui_list.R
import com.storyteller_f.common_ui_list.databinding.FragmentTestToolbarBinding

class TestToolBarFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FragmentTestToolbarBinding.inflate(inflater, container, false).root

    override fun onStart() {
        super.onStart()
        val toolbarCompose = (requireActivity().findViewById<Toolbar>(R.id.toolbar).getChildAt(0) as ComposeView)
        toolbarCompose.setContent {
            ToolBar()
        }
    }

    @Preview
    @Composable
    fun ToolBar() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {},
            ) {
                Text(text = "Reset")
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {},
            ) {
                Text(text = "Submit")
            }
        }
    }
}
