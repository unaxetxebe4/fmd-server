package de.nulide.findmydevice

import android.app.Application
import de.nulide.findmydevice.data.UncaughtExceptionHandler.Companion.initUncaughtExceptionHandler
import de.nulide.findmydevice.utils.Notifications


class FmdApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Notifications.init(this)
        initUncaughtExceptionHandler(this)
    }
}
