package dev.techda.sahatisukari

import android.app.Application
import dev.techda.sahatisukari.data.UpdateCheckScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SahatiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        UpdateCheckScheduler.schedule(this)
    }
}
