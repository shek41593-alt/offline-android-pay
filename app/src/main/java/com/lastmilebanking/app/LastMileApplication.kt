package com.lastmilebanking.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LastMileApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialization code here
    }
}
