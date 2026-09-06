package com.storyteller_f.common_ui_list.test_navigation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import com.storyteller_f.common_ui.Registry
import com.storyteller_f.common_ui.observeResponse
import com.storyteller_f.common_ui.request
import com.storyteller_f.common_ui.response
import com.storyteller_f.common_ui_list.R
import com.storyteller_f.common_ui_list.databinding.ActivityTestNavigationResultBinding
import com.storyteller_f.common_ui_list.dialog.TestDialog2
import com.storyteller_f.common_ui.viewBinding

class TestNavigationResultActivity : AppCompatActivity(), Registry {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private val binding by viewBinding(ActivityTestNavigationResultBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding
        setSupportActionBar(binding.toolbar)
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            request(TestDialog2::class.java).response(TestDialog2.Result::class.java) {
                Snackbar.make(view, it.test, Snackbar.LENGTH_LONG)
                    .setAction("activity->dialog", null).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        observeResponse()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
