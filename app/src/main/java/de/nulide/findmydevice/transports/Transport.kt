package de.nulide.findmydevice.transports

import android.content.Context
import android.util.Log
import androidx.annotation.CallSuper
import de.nulide.findmydevice.locationproviders.LocationProvider
import de.nulide.findmydevice.permissions.Permission
import de.nulide.findmydevice.utils.Utils


abstract class Transport<DestinationType>(
    private val destination: DestinationType
) {
    companion object {
        private val TAG = this::class.simpleName
    }

    abstract val requiredPermissions: List<Permission>

    fun missingRequiredPermissions(context: Context): List<Permission> {
        return requiredPermissions.filter { p -> !p.isGranted(context) }
    }

    open fun getDestinationString(): String? = null

    @CallSuper
    open fun send(context: Context, msg: String) {
        val missing = missingRequiredPermissions(context)
        if (missing.isNotEmpty()) {
            Log.w(TAG, "Cannot send message: missing permissions ${missing.joinToString(", ")}")
            return
        }
        // continue sending message
        // (this should be done in the concrete classes that override this function)
    }

    open fun sendNewLocation(
        context: Context,
        provider: String,
        lat: String,
        lon: String,
        timeMillis: Long,
    ) {
        val batteryLevel = Utils.getBatteryLevel(context)
        val msg = LocationProvider.buildLocationString(provider, lat, lon, batteryLevel, timeMillis)
        send(context, msg)
    }
}
