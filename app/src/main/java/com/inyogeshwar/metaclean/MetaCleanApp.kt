package com.inyogeshwar.metaclean

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MetaCleanApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
