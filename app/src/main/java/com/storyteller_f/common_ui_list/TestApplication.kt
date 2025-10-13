package com.storyteller_f.common_ui_list

import androidx.multidex.MultiDexApplication
import com.google.android.material.color.DynamicColors
import com.storyteller_f.ui_list.core.holders

class TestApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        holders(
            com.storyteller_f.common_ui_list.holders.seprator.ui_list.HolderBuilder::registerAll,
            com.storyteller_f.common_ui_list.holders.ui_list.HolderBuilder::registerAll,
        )
    }
}
