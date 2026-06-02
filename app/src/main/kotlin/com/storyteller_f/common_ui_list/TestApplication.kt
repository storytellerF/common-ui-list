package com.storyteller_f.common_ui_list

import android.app.Application
import com.google.android.material.color.DynamicColors

class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
